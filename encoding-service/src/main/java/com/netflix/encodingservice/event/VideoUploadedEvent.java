package com.netflix.encodingservice.event;

import lombok.Data;

@Data
public class VideoUploadedEvent {
    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private long fileSizeInBytes;
}
