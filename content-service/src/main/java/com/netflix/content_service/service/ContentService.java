package com.netflix.content_service.service;

import com.netflix.content_service.dto.MovieRequestDto;
import com.netflix.content_service.dto.MovieResponseDto;
import com.netflix.content_service.helper.MovieHelper;
import com.netflix.content_service.model.Genre;
import com.netflix.content_service.model.Movie;
import com.netflix.content_service.model.VideoStatus;
import com.netflix.content_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {

    private final MovieRepository movieRepository;
    private final MovieHelper movieHelper;

    public MovieResponseDto addMovie(MovieRequestDto request) {
        log.info("Adding new movie: {}", request.getTitle());

        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRating(request.getRating());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setDirector(request.getDirector());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie saveMovie = movieRepository.save(movie);
        log.info("Movie added with id: {}", saveMovie.getId());
        return movieHelper.mapToResponse(saveMovie);
    }

    public @Nullable List<MovieResponseDto> getAllMovies() {
        List<Movie> movies  = movieRepository.findAll();
        return movies.stream().map(movieHelper::mapToResponse).toList();
    }

    public @Nullable List<MovieResponseDto> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenre(genre).stream().map(movieHelper::mapToResponse).toList();
    }

    public @Nullable MovieResponseDto getMoviesById(String movieId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found."));
        return movieHelper.mapToResponse(movie);
    }

    public @Nullable List<MovieResponseDto> searchMovies(String title) {
        return movieRepository.findByTitle(title).stream().map(movieHelper::mapToResponse).toList();
    }

    public void updateVideoKey(String movieId, String videoKey) {
        log.info("Updating video key for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found."));
        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    public void updateHlsUrl(String movieId, String hlsUrl) {
        log.info("Updating HLS Url for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found."));
        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);
        log.info("Movie {} is for ready for streaming", movieId);
    }
}
