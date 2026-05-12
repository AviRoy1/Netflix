package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import com.netflix.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";

    // Format: resolution, bitrate, height
    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
            new int[]{1920, 5000, 1080},
            new int[]{1280, 2800, 720},
            new int[]{854, 1200, 480},
            new int[]{640, 800, 360}
    );


    public void encodeVideo(VideoUploadedEvent event) throws IOException {
        log.info("Starting encoding platform for movie: {}", event.getMovieId());

        //  Create a unique path for movie
        String jobPath = basePath + "/" + event.getMovieId();

        try {
            // Create temp directories
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            //  Step 1: Download raw video from S3
            String localVideoPath = jobPath + "/raw_video.mp4";
            downloadFromS3(event.getVideoKey(), localVideoPath);
            log.info("Raw video download to local video path: {}", localVideoPath);

            // Step 2: Encode to multiple qualities and generate HLS
            for(int[] quality: VIDEO_QUALITIES) {
                int width = quality[0];
                int bitrate = quality[1];
                int height = quality[2];

                String qualityDir = jobPath + "/encoded" + height + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHls(localVideoPath, qualityDir, width, bitrate, height);
                log.info("Encoded {}p successfully.", height);
            }

            //  Step 3: Generate master playlist.
            String masterPlayListPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlayList(masterPlayListPath);
            log.info("Master playlist generated.");

            // Step 4: Upload all resource file to S3
            String encodedPrefix = "encoded/" + event.getMovieId() + "/";
            uploadEncodedFileToS3(jobPath + "/encoded", encodedPrefix);
            log.info("All encoded files uploaded to S3.");

            // Step 5: Publish VideoEncodedEvent.
            String masterPlayListKey = encodedPrefix + "/master.m3u8";
            String hlsUrl = "htts://" + bucketName + ".s3.amazonaws.com/" + masterPlayListKey;

            VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    hlsUrl,
                    masterPlayListKey,
                    true,
                    null
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
            log.info("VideoEncodedEvent publish for movie: {}", event.getMovieId());

        } catch(Exception e) {
            log.error("Encoding failed for movie: {}", event.getMovieId());
            log.error(e.getMessage());
            VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage()
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, event.getMovieId(), encodedEvent);
        } finally {
            cleanupTempFiles(jobPath);
        }
    }


    /**
     * Download file form S3 to local path
     */
    private void downloadFromS3(String videoKey, String localVideoPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(videoKey)
                .build();
        s3Client.getObject(getObjectRequest, Paths.get(localVideoPath));

    }

    private void encodeToHls(String inputPath, String outputDir, int width, int bitrate, int height) throws IOException, InterruptedException {

        String playListPath = outputDir + "/playlist.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

        //  FFmpeg command for HLS encoding
        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", inputPath,                            //  input file
                "-vf", "scale=" + width + ":" + height,     // Scale to resolution
                "-c:v", "libx264",                          // video codec
                "-b:v", bitrate + "k",                      // video bitrate
                "-c:a", "aac",                              // audio codec
                "-b:a", "128k",                             // audio bitrate
                "-hls_time", "10",                          // 10 second segment
                "-hls_list_size", "0",                      // keep all segment
                "-hls_segment_filename", segmentPattern,    // segment naming
                "-f", "hls",                                // output format HLS
                playListPath                                // output playlist
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if(exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed with exit code: " + exitCode);
        }

    }


    private void generateMasterPlayList(String masterPlayListPath) throws IOException {
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3u\n");
        master.append("EXT-X-VERSION:3\n\n");

        // Add each quality to master playlist
        int[][] qualities = {{1920, 5000, 1080}, {1280, 2800, 720}, {854, 1200, 480}, {640, 800, 360}};

        for(int []q: qualities) {
            int width = q[0];
            int bitrate = q[1];
            int height = q[2];

            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate*1000)
                    .append(", RESOLUTION=").append(width).append("x").append(height)
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"\n");
            master.append(height).append("p/playlist.m3u8\n\n");

        }
        Files.writeString(Paths.get(masterPlayListPath), master.toString());
    }

    private void uploadEncodedFileToS3(String localDir, String encodedPrefix) throws IOException{
        File directory  = new File(localDir);
        uploadDirectoryToS3(directory, localDir,encodedPrefix);
    }

    private void uploadDirectoryToS3(File directory, String localDir, String s3Prefix) throws IOException{
        for(File file: Objects.requireNonNull(directory.listFiles())) {
            if(file.isDirectory()) {
                uploadDirectoryToS3(file, localDir, s3Prefix);
            } else {
                String relativePath = file.getAbsolutePath()
                        .substring(basePath.length()+1)
                        .replace("\\", "/");

                String s3Key = s3Prefix + relativePath;
                String contentType = file.getName().endsWith(".m3u8") ? "application/x-mpegURL" : "video/MP2T";

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                log.debug("Uploaded: {}", s3Key);
            }
        }
    }

    private void cleanupTempFiles(String jobPath) throws IOException{
        try {
            Path dirPath = Paths.get(jobPath);
            if(Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

        } catch(IOException e) {
            log.warn("Fail to clean up temp files: {}" , e.getMessage());
        }
    }

}
