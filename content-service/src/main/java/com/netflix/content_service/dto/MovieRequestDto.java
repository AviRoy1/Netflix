package com.netflix.content_service.dto;

import com.netflix.content_service.model.Genre;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequestDto {
    @NotBlank(message = "Title is required.")
    private String title;
    private String description;
    @NotBlank(message = "Genre is required.")
    private Genre genre;
    private String director;
    private String cast;
    private Integer releaseYear;
    private Double rating;
    private String thumbnailUrl;
    private Integer durationMinutes;
}
