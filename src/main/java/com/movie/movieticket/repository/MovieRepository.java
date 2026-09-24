package com.movie.movieticket.repository;

import com.movie.movieticket.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    List<Movie> findByReleasedTrue();
    
    List<Movie> findByReleasedFalse();
    
    List<Movie> findByTitleContainingIgnoreCase(String keyword);
    
    // New methods for admin dashboard
    @Query("SELECT COUNT(m) FROM Movie m")
    long countMovies();
    
    // Use a different method name to avoid ambiguity
    @Query("SELECT m FROM Movie m ORDER BY m.id DESC")
    List<Movie> findRecentMovies(Pageable pageable);
    
    // Count movies by genre
    @Query("SELECT COUNT(m) FROM Movie m WHERE m.genre LIKE %:genre%")
    long countByGenre(@Param("genre") String genre);
    
    // Fixed query for Top Grossing Movies
    @Query("SELECT m FROM Movie m " +
           "JOIN Screening s ON s.movie.id = m.id " +
           "JOIN Booking b ON b.screening.id = s.id " +
           "WHERE b.paid = true AND b.bookingTime BETWEEN ?1 AND ?2 " +
           "GROUP BY m.id ORDER BY SUM(b.totalPrice) DESC")
    List<Movie> findTopGrossingMovies(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Query for Most Watched Movies All Time
    @Query("SELECT m FROM Movie m " +
           "JOIN Screening s ON s.movie.id = m.id " +
           "JOIN Booking b ON b.screening.id = s.id " +
           "GROUP BY m.id ORDER BY COUNT(b) DESC")
    List<Movie> findMostWatchedMoviesAllTime(Pageable pageable);
    
    // Query for Most Watched Movies by Genre (using LIKE for partial matching)
    @Query("SELECT m FROM Movie m " +
           "JOIN Screening s ON s.movie.id = m.id " +
           "JOIN Booking b ON b.screening.id = s.id " +
           "WHERE m.genre LIKE %:genre% " +
           "GROUP BY m.id ORDER BY COUNT(b) DESC")
    List<Movie> findMostWatchedMoviesByGenre(@Param("genre") String genre, Pageable pageable);
    
    // Query for Top Grossing Movies by Genre (using LIKE for partial matching)
    @Query("SELECT m FROM Movie m " +
           "JOIN Screening s ON s.movie.id = m.id " +
           "JOIN Booking b ON b.screening.id = s.id " +
           "WHERE b.paid = true AND b.bookingTime BETWEEN :startDate AND :endDate AND m.genre LIKE %:genre% " +
           "GROUP BY m.id ORDER BY SUM(b.totalPrice) DESC")
    List<Movie> findTopGrossingMoviesByGenre(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            @Param("genre") String genre, 
            Pageable pageable);
}