package com.movie.movieticket.validator;

import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.repository.ScreeningRepository;
import com.movie.movieticket.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class ScreeningValidator {
    
    private static final Logger logger = Logger.getLogger(ScreeningValidator.class.getName());
    
    @Autowired
    private ScreeningRepository screeningRepository;
    
    @Autowired
    private MovieService movieService;
    
    // Buffer time between screenings in minutes
    private static final int BUFFER_TIME = 30;
    
    /**
     * Checks if a screening time conflicts with existing screenings in the same cinema
     * 
     * @param cinemaId The cinema ID
     * @param screeningTime The proposed screening time
     * @param screeningId Optional: The ID of the screening being edited (to exclude from conflict check)
     * @return true if there's a conflict, false otherwise
     */
    public boolean hasTimeConflict(Long cinemaId, LocalDateTime screeningTime, Long... screeningId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logger.info("DEBUG: Checking time conflict for cinema ID: " + cinemaId + " at time: " + screeningTime.format(formatter));
        
        // Get all screenings for this cinema
        List<Screening> existingScreenings = screeningRepository.findByCinemaId(cinemaId);
        logger.info("DEBUG: Found " + existingScreenings.size() + " existing screenings for cinema ID: " + cinemaId);
        
        // Log all existing screenings for debugging
        for (Screening s : existingScreenings) {
            logger.info("DEBUG: Existing screening ID: " + s.getId() + 
                       ", Movie: " + s.getMovie().getTitle() + 
                       ", Time: " + s.getScreeningTime().format(formatter) + 
                       ", Duration: " + s.getMovie().getDuration() + " minutes");
        }
        
        // Get the movie from the screening time if we're editing
        Movie proposedMovie = null;
        if (screeningId.length > 0) {
            Screening screening = screeningRepository.findById(screeningId[0]).orElse(null);
            if (screening != null) {
                proposedMovie = screening.getMovie();
                logger.info("DEBUG: Editing screening ID: " + screeningId[0] + 
                           ", Movie: " + proposedMovie.getTitle() + 
                           ", Duration: " + proposedMovie.getDuration() + " minutes");
            }
        }
        
        // Get the movie duration or use default
        int movieDuration = 120; // Default duration
        if (proposedMovie != null && proposedMovie.getDuration() != null && proposedMovie.getDuration() > 0) {
            movieDuration = proposedMovie.getDuration();
        }
        logger.info("DEBUG: Using movie duration: " + movieDuration + " minutes for conflict check");
        
        // Calculate the end time of the proposed screening (start time + actual duration + buffer)
        LocalDateTime proposedEndTime = screeningTime.plusMinutes(movieDuration + BUFFER_TIME);
        logger.info("DEBUG: Proposed screening would end at: " + proposedEndTime.format(formatter) + 
                   " (including " + BUFFER_TIME + " minutes buffer)");
        
        // Check for conflicts
        for (Screening existing : existingScreenings) {
            // Skip the current screening if we're editing
            if (screeningId.length > 0 && existing.getId().equals(screeningId[0])) {
                logger.info("DEBUG: Skipping current screening ID: " + screeningId[0] + " during conflict check");
                continue;
            }
            
            LocalDateTime existingStartTime = existing.getScreeningTime();
            
            // Get the actual duration of the existing movie
            int existingMovieDuration = 120; // Default duration
            if (existing.getMovie() != null && existing.getMovie().getDuration() != null && existing.getMovie().getDuration() > 0) {
                existingMovieDuration = existing.getMovie().getDuration();
            }
            
            LocalDateTime existingEndTime = existingStartTime.plusMinutes(existingMovieDuration + BUFFER_TIME);
            
            logger.info("DEBUG: Checking conflict with screening ID: " + existing.getId() + 
                       ", Movie: " + existing.getMovie().getTitle() + 
                       ", Start: " + existingStartTime.format(formatter) + 
                       ", End: " + existingEndTime.format(formatter) + 
                       " (including " + BUFFER_TIME + " minutes buffer)");
            
            // Check if the proposed screening overlaps with an existing screening
            boolean proposedStartsDuringExisting = screeningTime.isAfter(existingStartTime) && screeningTime.isBefore(existingEndTime);
            boolean proposedEndsDuringExisting = proposedEndTime.isAfter(existingStartTime) && proposedEndTime.isBefore(existingEndTime);
            boolean proposedContainsExisting = screeningTime.isBefore(existingStartTime) && proposedEndTime.isAfter(existingEndTime);
            
            boolean hasConflict = proposedStartsDuringExisting || proposedEndsDuringExisting || proposedContainsExisting;
            
            if (hasConflict) {
                logger.warning("DEBUG: Time conflict detected with screening ID: " + existing.getId() + 
                              ", Movie: " + existing.getMovie().getTitle() + 
                              ", Start: " + existingStartTime.format(formatter) + 
                              ", End: " + existingEndTime.format(formatter));
                
                if (proposedStartsDuringExisting) {
                    logger.warning("DEBUG: Conflict reason: Proposed screening starts during existing screening");
                }
                if (proposedEndsDuringExisting) {
                    logger.warning("DEBUG: Conflict reason: Proposed screening ends during existing screening");
                }
                if (proposedContainsExisting) {
                    logger.warning("DEBUG: Conflict reason: Proposed screening contains existing screening");
                }
                
                return true;
            }
        }
        
        logger.info("DEBUG: No time conflicts found for time: " + screeningTime.format(formatter));
        return false;
    }
    
    /**
     * Checks if a specific time is available for a screening in a cinema
     * 
     * @param cinemaId The cinema ID
     * @param screeningDate The date for the screening
     * @param time The time to check
     * @param screeningId Optional: The ID of the screening being edited (to exclude from conflict check)
     * @return true if the time is available, false if there's a conflict
     */
    public boolean isTimeAvailable(Long cinemaId, LocalDate screeningDate, LocalTime time, Long... screeningId) {
        logger.info("DEBUG: Checking if time is available: " + time + " for cinema ID: " + cinemaId + " on date: " + screeningDate);
        
        // Check if the time is in the past
        if (isTimeInPast(screeningDate, time)) {
            logger.warning("DEBUG: Time is in the past: " + time + " on date: " + screeningDate);
            return false;
        }
        
        LocalDateTime screeningTime = LocalDateTime.of(screeningDate, time);
        boolean hasConflict = hasTimeConflict(cinemaId, screeningTime, screeningId);
        logger.info("DEBUG: Time " + time + " on date " + screeningDate + " has conflict: " + hasConflict);
        return !hasConflict;
    }
    
    /**
     * Gets available times for a specific cinema and date
     * 
     * @param cinemaId The cinema ID
     * @param screeningDate The date to check
     * @param screeningId Optional: The ID of the screening being edited (to exclude from conflict check)
     * @return List of available times as LocalTime objects
     */
    public List<LocalTime> getAvailableTimes(Long cinemaId, LocalDate screeningDate, Long... screeningId) {
        // Standard screening times
        List<LocalTime> standardTimes = List.of(
            LocalTime.of(10, 0),  // 10:00 AM
            LocalTime.of(13, 0),  // 1:00 PM
            LocalTime.of(16, 0),  // 4:00 PM
            LocalTime.of(19, 0),  // 7:00 PM
            LocalTime.of(22, 0)   // 10:00 PM
        );
        
        logger.info("DEBUG: Getting available times for cinema ID: " + cinemaId + " on date: " + screeningDate);
        
        // Filter out times that are in the past or have conflicts
        List<LocalTime> availableTimes = new ArrayList<>();
        for (LocalTime time : standardTimes) {
            boolean isPast = isTimeInPast(screeningDate, time);
            boolean hasConflict = false;
            
            if (!isPast) {
                hasConflict = hasTimeConflict(cinemaId, LocalDateTime.of(screeningDate, time), screeningId);
            }
            
            logger.info("DEBUG: Time " + time + " - isPast: " + isPast + ", hasConflict: " + hasConflict);
            
            if (!isPast && !hasConflict) {
                availableTimes.add(time);
                logger.info("DEBUG: Added time " + time + " to available times");
            }
        }
        
        logger.info("DEBUG: Found " + availableTimes.size() + " available times");
        return availableTimes;
    }
    
    /**
     * Checks if a time is in the past
     * 
     * @param date The date to check
     * @param time The time to check
     * @return true if the time is in the past, false otherwise
     */
    public boolean isTimeInPast(LocalDate date, LocalTime time) {
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();
        
        boolean isPast = dateTime.isBefore(now);
        logger.info("DEBUG: Time " + time + " on date " + date + " is in the past: " + isPast);
        return isPast;
    }
    
    /**
     * Validates a custom time input
     * 
     * @param timeStr The time string to validate
     * @return true if the time is valid, false otherwise
     */
    public boolean isValidTimeFormat(String timeStr) {
        try {
            LocalTime time = LocalTime.parse(timeStr);
            logger.info("DEBUG: Time string " + timeStr + " is valid, parsed as: " + time);
            return true;
        } catch (Exception e) {
            logger.warning("DEBUG: Time string " + timeStr + " is invalid: " + e.getMessage());
            return false;
        }
    }
}