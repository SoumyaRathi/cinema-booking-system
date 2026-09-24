package com.movie.movieticket.repository;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    
    // Existing methods (keep all your current methods)
    List<Screening> findAllByOrderByIdDesc();
    
    List<Screening> findByMovieId(Long movieId);
    
    List<Screening> findByMovieIdOrderByIdDesc(Long movieId);
    
    List<Screening> findByScreeningTimeAfter(LocalDateTime time);
    
    List<Screening> findByScreeningTimeAfterOrderByIdDesc(LocalDateTime time);
    
    List<Screening> findByMovieIdAndScreeningTimeAfter(Long movieId, LocalDateTime time);
    
    List<Screening> findByMovieIdAndScreeningTimeAfterOrderByIdDesc(Long movieId, LocalDateTime time);
    
    List<Screening> findByMovieIdAndScreeningTimeAfterOrderByScreeningTimeAsc(Long movieId, LocalDateTime time);
    
    void deleteByMovieId(Long movieId);
    
    List<Screening> findByCinemaId(Long cinemaId);
    
    List<Screening> findByCinemaIdOrderByIdDesc(Long cinemaId);
    
    List<Screening> findByCinemaIdAndScreeningTimeAfter(Long cinemaId, LocalDateTime time);
    
    List<Screening> findByCinemaIdAndScreeningTimeAfterOrderByIdDesc(Long cinemaId, LocalDateTime time);
    
    @Query("SELECT s FROM Screening s WHERE s.cinema.id = :cinemaId AND s.screeningTime BETWEEN :startTime AND :endTime")
    List<Screening> findByCinemaIdAndTimeRange(
        @Param("cinemaId") Long cinemaId, 
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT COUNT(s) FROM Screening s")
    long countScreenings();
    
    List<Screening> findByScreeningTimeAfterOrderByScreeningTimeAsc(LocalDateTime time, Pageable pageable);
    
    List<Screening> findByScreeningTimeAfterOrderByScreeningTimeAscIdDesc(LocalDateTime time, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Screening s WHERE DATE(s.screeningTime) = CURRENT_DATE")
    long countTodayScreenings();
    
    List<Screening> findByTheater(String theater);
    
    @Query("SELECT s FROM Screening s WHERE s.screeningTime BETWEEN ?1 AND ?2 ORDER BY s.screeningTime ASC")
    List<Screening> findByScreeningTimeBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT s FROM Screening s WHERE s.screeningTime BETWEEN ?1 AND ?2 ORDER BY s.id DESC")
    List<Screening> findByScreeningTimeBetweenOrderByIdDesc(LocalDateTime start, LocalDateTime end);
    
    // MISSING METHODS - Add these to fix the compilation errors
    
    // Methods that work with Movie and Cinema objects (not just IDs)
    List<Screening> findByMovie(Movie movie);
    
    List<Screening> findByCinema(Cinema cinema);
    
    // Single screening time methods
    List<Screening> findByScreeningTime(LocalDateTime screeningTime);
    
    List<Screening> findByScreeningTimeBefore(LocalDateTime dateTime);
    
    // Combined Movie and Cinema methods
    List<Screening> findByMovieAndCinema(Movie movie, Cinema cinema);
    
    List<Screening> findByMovieAndScreeningTimeBetween(Movie movie, LocalDateTime startTime, LocalDateTime endTime);
    
    List<Screening> findByCinemaAndScreeningTimeBetween(Cinema cinema, LocalDateTime startTime, LocalDateTime endTime);
    
    // Additional useful query methods
    @Query("SELECT s FROM Screening s WHERE s.movie = :movie AND s.cinema = :cinema ORDER BY s.screeningTime ASC")
    List<Screening> findByMovieAndCinemaOrderByTime(@Param("movie") Movie movie, @Param("cinema") Cinema cinema);
    
    @Query("SELECT s FROM Screening s WHERE s.movie = :movie AND s.screeningTime BETWEEN :startTime AND :endTime ORDER BY s.screeningTime ASC")
    List<Screening> findByMovieAndTimeBetweenOrderByTime(@Param("movie") Movie movie, 
                                                        @Param("startTime") LocalDateTime startTime, 
                                                        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT s FROM Screening s WHERE s.cinema = :cinema AND s.screeningTime BETWEEN :startTime AND :endTime ORDER BY s.screeningTime ASC")
    List<Screening> findByCinemaAndTimeBetweenOrderByTime(@Param("cinema") Cinema cinema, 
                                                         @Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime);
    
    // Availability and conflict checking methods
    @Query("SELECT s FROM Screening s WHERE s.availableSeats > 0")
    List<Screening> findScreeningsWithAvailableSeats();
    
    @Query("SELECT s FROM Screening s WHERE s.movie.title LIKE %:title%")
    List<Screening> findByMovieTitleContaining(@Param("title") String title);
    
    @Query("SELECT s FROM Screening s WHERE s.cinema.name LIKE %:name%")
    List<Screening> findByCinemaNameContaining(@Param("name") String name);
    
    @Query("SELECT COUNT(s) FROM Screening s WHERE s.movie.id = :movieId")
    long countByMovieId(@Param("movieId") Long movieId);
    
    @Query("SELECT COUNT(s) FROM Screening s WHERE s.cinema.id = :cinemaId")
    long countByCinemaId(@Param("cinemaId") Long cinemaId);
    
    boolean existsByMovieAndCinemaAndScreeningTime(Movie movie, Cinema cinema, LocalDateTime screeningTime);
    
    // Additional methods for better functionality
    @Query("SELECT s FROM Screening s WHERE s.movie.id = :movieId AND s.cinema.id = :cinemaId ORDER BY s.screeningTime ASC")
    List<Screening> findByMovieIdAndCinemaIdOrderByTime(@Param("movieId") Long movieId, @Param("cinemaId") Long cinemaId);
    
    @Query("SELECT s FROM Screening s WHERE s.screeningTime >= :startTime AND s.screeningTime <= :endTime ORDER BY s.screeningTime ASC")
    List<Screening> findByDateRangeOrderByTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT s FROM Screening s WHERE s.cinema.id = :cinemaId AND DATE(s.screeningTime) = DATE(:date) ORDER BY s.screeningTime ASC")
    List<Screening> findByCinemaIdAndDateOrderByTime(@Param("cinemaId") Long cinemaId, @Param("date") LocalDateTime date);
    
    @Query("SELECT s FROM Screening s WHERE s.movie.id = :movieId AND DATE(s.screeningTime) = DATE(:date) ORDER BY s.screeningTime ASC")
    List<Screening> findByMovieIdAndDateOrderByTime(@Param("movieId") Long movieId, @Param("date") LocalDateTime date);
    
    @Query("SELECT DISTINCT s.cinema FROM Screening s WHERE s.movie.id = :movieId")
    List<Cinema> findCinemasByMovieId(@Param("movieId") Long movieId);
    
    @Query("SELECT DISTINCT s.movie FROM Screening s WHERE s.cinema.id = :cinemaId")
    List<Movie> findMoviesByCinemaId(@Param("cinemaId") Long cinemaId);
    
    @Query("SELECT s FROM Screening s WHERE s.availableSeats > 0 AND s.screeningTime > :currentTime ORDER BY s.screeningTime ASC")
    List<Screening> findUpcomingAvailableScreenings(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT s FROM Screening s WHERE s.movie.id = :movieId AND s.availableSeats > 0 AND s.screeningTime > :currentTime ORDER BY s.screeningTime ASC")
    List<Screening> findUpcomingAvailableScreeningsByMovie(@Param("movieId") Long movieId, @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT s FROM Screening s WHERE s.cinema.id = :cinemaId AND s.availableSeats > 0 AND s.screeningTime > :currentTime ORDER BY s.screeningTime ASC")
    List<Screening> findUpcomingAvailableScreeningsByCinema(@Param("cinemaId") Long cinemaId, @Param("currentTime") LocalDateTime currentTime);
}