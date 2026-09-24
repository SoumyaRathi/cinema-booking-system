package com.movie.movieticket.service;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.repository.CinemaRepository;
import com.movie.movieticket.repository.ScreeningRepository;
import com.movie.movieticket.validator.ScreeningValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ScreeningServiceImpl implements ScreeningService {

    private static final Logger logger = Logger.getLogger(ScreeningServiceImpl.class.getName());

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private ScreeningValidator screeningValidator;

    @Override
    public List<Screening> getAllScreenings() {
        try {
            // Use the new method to get screenings ordered by newest first
            return screeningRepository.findAllByOrderByIdDesc();
        } catch (Exception e) {
            logger.severe("Error getting all screenings: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Screening getScreeningById(Long id) {
        if (id == null) {
            logger.warning("Screening ID is null when getting screening by ID");
            return null;
        }
        try {
            return screeningRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.severe("Error getting screening by ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Screening> getScreeningsByMovieId(Long movieId) {
        if (movieId == null) {
            logger.warning("Movie ID is null when getting screenings by movie ID");
            return new ArrayList<>();
        }
        try {
            // Use the new method to get screenings by movie ordered by newest first
            return screeningRepository.findByMovieIdOrderByIdDesc(movieId);
        } catch (Exception e) {
            logger.severe("Error getting screenings by movie ID " + movieId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByMovie(Movie movie) {
        if (movie == null) {
            logger.warning("Movie is null when getting screenings by movie");
            return new ArrayList<>();
        }
        try {
            // Get current time
            LocalDateTime now = LocalDateTime.now();
            
            // Use the repository method to find screenings for this movie that are in the future
            return screeningRepository.findByMovieIdAndScreeningTimeAfterOrderByScreeningTimeAsc(movie.getId(), now);
        } catch (Exception e) {
            logger.severe("Error getting screenings by movie: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Screening saveScreening(Screening screening) {
        if (screening == null) {
            logger.warning("Screening is null when saving");
            return null;
        }
        try {
            return screeningRepository.save(screening);
        } catch (Exception e) {
            logger.severe("Error saving screening: " + e.getMessage());
            throw new RuntimeException("Failed to save screening", e);
        }
    }

    @Override
    @Transactional
    public void deleteScreening(Long id) {
        if (id == null) {
            logger.warning("Screening ID is null when deleting");
            return;
        }
        try {
            screeningRepository.deleteById(id);
            logger.info("Deleted screening with ID: " + id);
        } catch (Exception e) {
            logger.severe("Error deleting screening with ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete screening", e);
        }
    }

    @Override
    public long getScreeningCount() {
        try {
            return screeningRepository.countScreenings();
        } catch (Exception e) {
            logger.severe("Error getting screening count: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public long getTodayScreeningCount() {
        try {
            return screeningRepository.countTodayScreenings();
        } catch (Exception e) {
            logger.severe("Error getting today's screening count: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public List<Screening> getUpcomingScreenings(LocalDateTime fromDate, int limit) {
        if (fromDate == null) {
            fromDate = LocalDateTime.now();
        }
        try {
            // Use the new method that includes ID in the ordering
            return screeningRepository.findByScreeningTimeAfterOrderByScreeningTimeAscIdDesc(
                fromDate,
                PageRequest.of(0, limit)
            );
        } catch (Exception e) {
            logger.severe("Error getting upcoming screenings: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsBetweenDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            logger.warning("Start or end date is null when getting screenings between dates");
            return new ArrayList<>();
        }
        try {
            // Use the new method to get screenings between dates ordered by newest first
            return screeningRepository.findByScreeningTimeBetweenOrderByIdDesc(start, end);
        } catch (Exception e) {
            logger.severe("Error getting screenings between dates: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByTheater(String theater) {
        if (theater == null || theater.trim().isEmpty()) {
            logger.warning("Theater is null or empty when getting screenings by theater");
            return new ArrayList<>();
        }
        try {
            return screeningRepository.findByTheater(theater);
        } catch (Exception e) {
            logger.severe("Error getting screenings by theater: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByCinema(Cinema cinema) {
        if (cinema == null) {
            logger.warning("Cinema is null when getting screenings by cinema");
            return new ArrayList<>();
        }
        try {
            // Use the new method to get screenings by cinema ordered by newest first
            return screeningRepository.findByCinemaIdOrderByIdDesc(cinema.getId());
        } catch (Exception e) {
            logger.severe("Error getting screenings by cinema: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByCinemaId(Long cinemaId) {
        if (cinemaId == null) {
            logger.warning("Cinema ID is null when getting screenings by cinema ID");
            return new ArrayList<>();
        }
        try {
            // Use the new method to get screenings by cinema ordered by newest first
            return screeningRepository.findByCinemaIdOrderByIdDesc(cinemaId);
        } catch (Exception e) {
            logger.severe("Error getting screenings by cinema ID " + cinemaId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // New methods for available screenings

    @Override
    public List<Screening> getAvailableScreenings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            // Use the new method to get screenings after a time ordered by newest first
            List<Screening> upcomingScreenings = screeningRepository.findByScreeningTimeAfterOrderByIdDesc(now);
            return upcomingScreenings.stream()
                    .filter(Screening::isAvailable)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error getting available screenings: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getAvailableScreeningsByMovieId(Long movieId) {
        if (movieId == null) {
            logger.warning("Movie ID is null when getting available screenings by movie ID");
            return new ArrayList<>();
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            // Use the new method to get screenings by movie after a time ordered by newest first
            List<Screening> upcomingScreenings = screeningRepository.findByMovieIdAndScreeningTimeAfterOrderByIdDesc(movieId, now);
            return upcomingScreenings.stream()
                    .filter(Screening::isAvailable)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error getting available screenings by movie ID " + movieId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getAvailableScreeningsByCinemaId(Long cinemaId) {
        if (cinemaId == null) {
            logger.warning("Cinema ID is null when getting available screenings by cinema ID");
            return new ArrayList<>();
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            // Use the new method to get screenings by cinema after a time ordered by newest first
            List<Screening> upcomingScreenings = screeningRepository.findByCinemaIdAndScreeningTimeAfterOrderByIdDesc(cinemaId, now);
            return upcomingScreenings.stream()
                    .filter(Screening::isAvailable)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error getting available screenings by cinema ID " + cinemaId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // New methods for time conflict checking

    @Override
    public boolean hasTimeConflict(Long cinemaId, LocalDateTime screeningTime, Long... screeningId) {
        try {
            return screeningValidator.hasTimeConflict(cinemaId, screeningTime, screeningId);
        } catch (Exception e) {
            logger.severe("Error checking time conflict: " + e.getMessage());
            return true; // Return true to be safe
        }
    }

    @Override
    public boolean isTimeAvailable(Long cinemaId, LocalDate screeningDate, LocalTime time, Long... screeningId) {
        try {
            return screeningValidator.isTimeAvailable(cinemaId, screeningDate, time, screeningId);
        } catch (Exception e) {
            logger.severe("Error checking time availability: " + e.getMessage());
            return false; // Return false to be safe
        }
    }

    @Override
    public List<LocalTime> getAvailableTimes(Long cinemaId, LocalDate screeningDate, Long... screeningId) {
        try {
            return screeningValidator.getAvailableTimes(cinemaId, screeningDate, screeningId);
        } catch (Exception e) {
            logger.severe("Error getting available times: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Map<Long, List<LocalTime>> getAvailableTimesByCinema(LocalDate screeningDate) {
        try {
            Map<Long, List<LocalTime>> result = new HashMap<>();
            List<Cinema> cinemas = cinemaRepository.findAll();
            
            for (Cinema cinema : cinemas) {
                List<LocalTime> availableTimes = screeningValidator.getAvailableTimes(cinema.getId(), screeningDate);
                result.put(cinema.getId(), availableTimes);
            }
            
            return result;
        } catch (Exception e) {
            logger.severe("Error getting available times by cinema: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // MISSING METHODS IMPLEMENTATION FOR AdminScreeningController

    @Override
    public List<Screening> getScreeningsByDate(LocalDate date) {
        if (date == null) {
            logger.warning("Date is null when getting screenings by date");
            return new ArrayList<>();
        }
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            return screeningRepository.findByScreeningTimeBetweenOrderByIdDesc(startOfDay, endOfDay);
        } catch (Exception e) {
            logger.severe("Error getting screenings by date " + date + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            logger.warning("Start date or end date is null when getting screenings by date range");
            return new ArrayList<>();
        }
        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            return screeningRepository.findByScreeningTimeBetweenOrderByIdDesc(startDateTime, endDateTime);
        } catch (Exception e) {
            logger.severe("Error getting screenings by date range " + startDate + " to " + endDate + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            logger.warning("DateTime is null when getting screenings by date time");
            return new ArrayList<>();
        }
        try {
            return screeningRepository.findByScreeningTime(dateTime);
        } catch (Exception e) {
            logger.severe("Error getting screenings by date time " + dateTime + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByMovieAndDate(Movie movie, LocalDate date) {
        if (movie == null || date == null) {
            logger.warning("Movie or date is null when getting screenings by movie and date");
            return new ArrayList<>();
        }
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            return screeningRepository.findByMovieAndScreeningTimeBetween(movie, startOfDay, endOfDay);
        } catch (Exception e) {
            logger.severe("Error getting screenings by movie and date: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByMovieAndCinema(Movie movie, Cinema cinema) {
        if (movie == null || cinema == null) {
            logger.warning("Movie or cinema is null when getting screenings by movie and cinema");
            return new ArrayList<>();
        }
        try {
            return screeningRepository.findByMovieAndCinema(movie, cinema);
        } catch (Exception e) {
            logger.severe("Error getting screenings by movie and cinema: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsByCinemaAndDate(Cinema cinema, LocalDate date) {
        if (cinema == null || date == null) {
            logger.warning("Cinema or date is null when getting screenings by cinema and date");
            return new ArrayList<>();
        }
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            return screeningRepository.findByCinemaAndScreeningTimeBetween(cinema, startOfDay, endOfDay);
        } catch (Exception e) {
            logger.severe("Error getting screenings by cinema and date: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsAfterDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            logger.warning("DateTime is null when getting screenings after date time");
            return new ArrayList<>();
        }
        try {
            return screeningRepository.findByScreeningTimeAfterOrderByIdDesc(dateTime);
        } catch (Exception e) {
            logger.severe("Error getting screenings after date time " + dateTime + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Screening> getScreeningsBeforeDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            logger.warning("DateTime is null when getting screenings before date time");
            return new ArrayList<>();
        }
        try {
            return screeningRepository.findByScreeningTimeBefore(dateTime);
        } catch (Exception e) {
            logger.severe("Error getting screenings before date time " + dateTime + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasAvailableSeats(Long screeningId) {
        if (screeningId == null) {
            logger.warning("Screening ID is null when checking available seats");
            return false;
        }
        try {
            Screening screening = screeningRepository.findById(screeningId).orElse(null);
            return screening != null && screening.getAvailableSeats() > 0;
        } catch (Exception e) {
            logger.severe("Error checking available seats for screening " + screeningId + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void updateAvailableSeats(Long screeningId, int availableSeats) {
        if (screeningId == null) {
            logger.warning("Screening ID is null when updating available seats");
            return;
        }
        try {
            Screening screening = screeningRepository.findById(screeningId).orElse(null);
            if (screening != null) {
                screening.setAvailableSeats(availableSeats);
                screeningRepository.save(screening);
                logger.info("Updated available seats for screening " + screeningId + " to " + availableSeats);
            } else {
                logger.warning("Screening not found with ID: " + screeningId);
            }
        } catch (Exception e) {
            logger.severe("Error updating available seats for screening " + screeningId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update available seats", e);
        }
    }

    @Override
    public List<Screening> searchScreenings(String movieTitle, String cinemaName, LocalDate date) {
        try {
            List<Screening> allScreenings = screeningRepository.findAllByOrderByIdDesc();
            
            return allScreenings.stream()
                .filter(screening -> {
                    boolean matches = true;
                    
                    // Filter by movie title
                    if (movieTitle != null && !movieTitle.trim().isEmpty()) {
                        matches = matches && screening.getMovie().getTitle()
                            .toLowerCase().contains(movieTitle.toLowerCase());
                    }
                    
                    // Filter by cinema name
                    if (cinemaName != null && !cinemaName.trim().isEmpty()) {
                        matches = matches && screening.getCinema().getName()
                            .toLowerCase().contains(cinemaName.toLowerCase());
                    }
                    
                    // Filter by date
                    if (date != null) {
                        LocalDate screeningDate = screening.getScreeningTime().toLocalDate();
                        matches = matches && screeningDate.equals(date);
                    }
                    
                    return matches;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error searching screenings: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}