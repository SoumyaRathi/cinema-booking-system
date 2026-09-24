package com.movie.movieticket.service;

import com.movie.movieticket.dto.MovieDto;
import com.movie.movieticket.dto.MovieSalesDTO;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface MovieService {
    
    // Movie operations
    List<Movie> getAllMovies();
    
    Movie getMovieById(Long id);
    
    List<Movie> getReleasedMovies();
    
    List<Movie> getUpcomingMovies();
    
    // NEW: Add missing method
    List<Movie> getReleasedMoviesByGenre(String genre);
    
    Movie saveMovie(MovieDto movieDto);
    
    Movie updateMovie(Long id, MovieDto movieDto);
    
    void deleteMovie(Long id);
    
    // Screening operations
    List<Screening> getAllScreenings();
    
    List<Screening> getAllUpcomingScreenings();
    
    Screening getScreeningById(Long id);
    
    List<Screening> getScreeningsByMovieId(Long movieId);
    
    Screening saveScreening(Screening screening);
    
    void deleteScreening(Long id);
    
    // Search operations
    List<Movie> searchMovies(String keyword);
    
    // Admin dashboard methods
    long getMovieCount();
    
    List<Movie> getRecentMovies(int limit);
    
    // New analytics report methods
    List<Movie> getTopGrossingMovies(LocalDateTime startDate, LocalDateTime endDate, int limit);
    
    List<Movie> getMostWatchedMoviesAllTime(int limit);
    
    List<MovieSalesDTO> getTopGrossingMoviesWithRevenue(LocalDateTime startDate, LocalDateTime endDate, int limit);
    
    // New methods for genre filtering
    Set<String> getAllGenres();
    
    List<Movie> getTopGrossingMoviesByGenre(LocalDateTime startDate, LocalDateTime endDate, String genre, int limit);
    
    List<Movie> getMostWatchedMoviesByGenre(String genre, int limit);
    
    List<MovieSalesDTO> getTopGrossingMoviesWithRevenueByGenre(LocalDateTime startDate, LocalDateTime endDate, String genre, int limit);
    
    // New methods for multi-genre support
    Set<String> extractAllGenres();
    
    boolean movieHasGenre(Movie movie, String genre);
}