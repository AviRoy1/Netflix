package com.netflix.content_service.helper;

import com.netflix.content_service.dto.MovieResponseDto;
import com.netflix.content_service.model.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieHelper {

    public MovieResponseDto mapToResponse(Movie request) {
        MovieResponseDto movie = new MovieResponseDto();
        movie.setId(request.getId());
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRating(request.getRating());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setDirector(request.getDirector());
        movie.setVideoStatus(request.getVideoStatus());
        movie.setCreatedAt(request.getCreatedAt());

        return movie;
    }
}
