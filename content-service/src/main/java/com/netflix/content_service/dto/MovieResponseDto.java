package com.netflix.content_service.dto;

import com.netflix.content_service.model.Genre;
import com.netflix.content_service.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieResponseDto {

    private String id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private Integer releaseYear;
    private Double rating;
    private String thumbnailUrl;
    private Integer durationMinutes;
    private String videoKey;
    private String hlsUrl;
    private VideoStatus videoStatus;
    private LocalDateTime createdAt;

}
