package com.movie.movieticket.config;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.repository.CinemaRepository;
import com.movie.movieticket.repository.ScreeningRepository;
import com.movie.movieticket.service.SeatService;
import com.movie.movieticket.service.ScreeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Component
@Order(1) // Run before DataInitializer
public class DatabaseMigration implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(DatabaseMigration.class.getName());

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private CinemaRepository cinemaRepository;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private ScreeningService screeningService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting database migration...");
        
        // First, ensure we have at least one cinema
        ensureCinemasExist();
        
        // Update screenings without cinema
        updateScreeningsWithoutCinema();
        
        // Initialize seats for screenings and fix seat counts
        initializeSeatsForScreenings();
        
        // Fix existing seat counts
        fixExistingScreeningSeatCounts();
        
        logger.info("Database migration completed");
    }
    
    @Transactional
    private void ensureCinemasExist() {
        try {
            if (cinemaRepository.count() == 0) {
                Cinema defaultCinema = new Cinema("Cinema 1", 120, "standard");
                cinemaRepository.save(defaultCinema);
                logger.info("Created default cinema for migration");
                
                // Add more cinemas
                Cinema cinema2 = new Cinema("Cinema 2", 100, "standard");
                Cinema cinema3 = new Cinema("Cinema 3", 80, "premium");
                cinemaRepository.save(cinema2);
                cinemaRepository.save(cinema3);
            }
        } catch (Exception e) {
            logger.severe("Error ensuring cinemas exist: " + e.getMessage());
        }
    }
    
    @Transactional
    private void updateScreeningsWithoutCinema() {
        try {
            // Get the default cinema
            Cinema defaultCinema = cinemaRepository.findByName("Cinema 2"); // Use Cinema 2 as default
            if (defaultCinema == null) {
                List<Cinema> cinemas = cinemaRepository.findAll();
                if (!cinemas.isEmpty()) {
                    defaultCinema = cinemas.get(1); // Get second cinema if available
                    if (defaultCinema == null) {
                        defaultCinema = cinemas.get(0);
                    }
                } else {
                    logger.warning("No cinemas found for updating screenings");
                    return;
                }
            }
            
            // Update all screenings that don't have a cinema
            List<Screening> screeningsWithoutCinema = screeningRepository.findAll();
            int updatedCount = 0;
            
            for (Screening screening : screeningsWithoutCinema) {
                if (screening.getCinema() == null) {
                    screening.setCinema(defaultCinema);
                    screeningRepository.save(screening);
                    updatedCount++;
                }
            }
            
            if (updatedCount > 0) {
                logger.info("Updated " + updatedCount + " screenings with default cinema");
            }
        } catch (Exception e) {
            logger.severe("Error updating screenings without cinema: " + e.getMessage());
        }
    }
    
    private void initializeSeatsForScreenings() {
        try {
            // Get all screenings
            List<Screening> allScreenings = screeningRepository.findAll();
            logger.info("Found " + allScreenings.size() + " screenings");
            
            for (Screening screening : allScreenings) {
                try {
                    // Check if this screening has seats
                    List<Seat> seats = seatService.getSeatsByScreeningId(screening.getId());
                    logger.info("Screening ID " + screening.getId() + " has " + seats.size() + " seats");
                    
                    if (seats.isEmpty()) {
                        logger.info("Initializing seats for screening ID: " + screening.getId());
                        seatService.initializeSeatsForScreening(screening);
                        logger.info("Successfully initialized seats for screening ID: " + screening.getId());
                    }
                } catch (Exception e) {
                    logger.severe("Error processing screening ID " + screening.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("Error initializing seats for screenings: " + e.getMessage());
        }
    }
    
    @Transactional
    private void fixExistingScreeningSeatCounts() {
        try {
            List<Screening> allScreenings = screeningRepository.findAll();
            logger.info("Fixing seat counts for " + allScreenings.size() + " screenings");
            
            for (Screening screening : allScreenings) {
                try {
                    List<Seat> seats = seatService.getSeatsByScreeningId(screening.getId());
                    
                    if (!seats.isEmpty()) {
                        int actualTotalSeats = seats.size();
                        int actualBookedSeats = (int) seats.stream().filter(Seat::isBooked).count();
                        int actualAvailableSeats = actualTotalSeats - actualBookedSeats;
                        
                        boolean needsUpdate = false;
                        
                        if (screening.getTotalSeats() != actualTotalSeats) {
                            logger.info("Fixing total seats for screening " + screening.getId() + 
                                       ": " + screening.getTotalSeats() + " -> " + actualTotalSeats);
                            screening.setTotalSeats(actualTotalSeats);
                            needsUpdate = true;
                        }
                        
                        if (screening.getAvailableSeats() != actualAvailableSeats) {
                            logger.info("Fixing available seats for screening " + screening.getId() + 
                                       ": " + screening.getAvailableSeats() + " -> " + actualAvailableSeats);
                            screening.setAvailableSeats(actualAvailableSeats);
                            needsUpdate = true;
                        }
                        
                        if (needsUpdate) {
                            screeningRepository.save(screening);
                            logger.info("Updated seat counts for screening " + screening.getId());
                        }
                    }
                } catch (Exception e) {
                    logger.severe("Error fixing seat counts for screening " + screening.getId() + ": " + e.getMessage());
                }
            }
            
            logger.info("Completed fixing seat counts for all screenings");
        } catch (Exception e) {
            logger.severe("Error fixing existing screening seat counts: " + e.getMessage());
        }
    }
}