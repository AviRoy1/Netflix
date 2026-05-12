package com.netflix.content_service.controller;

import com.netflix.content_service.dto.MovieRequestDto;
import com.netflix.content_service.dto.MovieResponseDto;
import com.netflix.content_service.model.Genre;
import com.netflix.content_service.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/movies")
@Slf4j
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<MovieResponseDto> addMovie(@Valid @RequestBody MovieRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.addMovie(request));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponseDto>> getAllMovies() {
        return ResponseEntity.ok(contentService.getAllMovies());
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponseDto>> getMoviesByGenre(@PathVariable Genre genre) {
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }

    @GetMapping("/genre/{movieId}")
    public ResponseEntity<MovieResponseDto> getMoviesById(@PathVariable String movieId) {
        return ResponseEntity.ok(contentService.getMoviesById(movieId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponseDto>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(contentService.searchMovies(title));
    }
}
