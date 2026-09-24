package com.movie.movieticket.service;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface ScreeningService {
    
    List<Screening> getAllScreenings();
    
    Screening getScreeningById(Long id);
    
    List<Screening> getScreeningsByMovieId(Long movieId);
    
    List<Screening> getScreeningsByMovie(Movie movie);
    
    Screening saveScreening(Screening screening);
    
    void deleteScreening(Long id);
    
    long getScreeningCount();
    
    long getTodayScreeningCount();
    
    List<Screening> getUpcomingScreenings(LocalDateTime fromDate, int limit);
    
    List<Screening> getScreeningsBetweenDates(LocalDateTime start, LocalDateTime end);
    
    // Keep theater-related method
    List<Screening> getScreeningsByTheater(String theater);
    
    // Add cinema-related methods
    List<Screening> getScreeningsByCinema(Cinema cinema);
    
    List<Screening> getScreeningsByCinemaId(Long cinemaId);
    
    // New methods for available screenings
    List<Screening> getAvailableScreenings();
    
    List<Screening> getAvailableScreeningsByMovieId(Long movieId);
    
    List<Screening> getAvailableScreeningsByCinemaId(Long cinemaId);
    
    // New methods for time conflict checking
    boolean hasTimeConflict(Long cinemaId, LocalDateTime screeningTime, Long... screeningId);
    
    boolean isTimeAvailable(Long cinemaId, LocalDate screeningDate, LocalTime time, Long... screeningId);
    
    List<LocalTime> getAvailableTimes(Long cinemaId, LocalDate screeningDate, Long... screeningId);
    
    Map<Long, List<LocalTime>> getAvailableTimesByCinema(LocalDate screeningDate);
    
    // MISSING METHODS NEEDED FOR AdminScreeningController
    List<Screening> getScreeningsByDate(LocalDate date);
    
    List<Screening> getScreeningsByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<Screening> getScreeningsByDateTime(LocalDateTime dateTime);
    
    List<Screening> getScreeningsByMovieAndDate(Movie movie, LocalDate date);
    
    List<Screening> getScreeningsByMovieAndCinema(Movie movie, Cinema cinema);
    
    List<Screening> getScreeningsByCinemaAndDate(Cinema cinema, LocalDate date);
    
    List<Screening> getScreeningsAfterDateTime(LocalDateTime dateTime);
    
    List<Screening> getScreeningsBeforeDateTime(LocalDateTime dateTime);
    
    boolean hasAvailableSeats(Long screeningId);
    
    void updateAvailableSeats(Long screeningId, int availableSeats);
    
    List<Screening> searchScreenings(String movieTitle, String cinemaName, LocalDate date);
}