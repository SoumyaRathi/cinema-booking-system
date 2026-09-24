package com.movie.movieticket.service;

import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.SeatReservation;
import com.movie.movieticket.repository.SeatRepository;
import com.movie.movieticket.repository.SeatReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger logger = Logger.getLogger(SeatServiceImpl.class.getName());

    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @Override
    public List<Seat> getAllSeats() {
        try {
            return seatRepository.findAll();
        } catch (Exception e) {
            logger.severe("Error getting all seats: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Seat getSeatById(Long id) {
        if (id == null) {
            logger.warning("Seat ID is null when getting seat by ID");
            return null;
        }
        try {
            return seatRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.severe("Error getting seat by ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<Seat> getSeatsByScreening(Screening screening) {
        if (screening == null) {
            logger.warning("Screening is null when getting seats");
            return new ArrayList<>();
        }
        try {
            return seatRepository.findByScreening(screening);
        } catch (Exception e) {
            logger.severe("Error getting seats by screening: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Seat> getSeatsByScreeningId(Long screeningId) {
        if (screeningId == null) {
            logger.warning("ScreeningId is null when getting seats");
            return new ArrayList<>();
        }
        try {
            return seatRepository.findByScreeningId(screeningId);
        } catch (Exception e) {
            logger.severe("Error getting seats by screening ID " + screeningId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Seat> getSeatsByBookingId(Long bookingId) {
        if (bookingId == null) {
            logger.warning("BookingId is null when getting seats");
            return new ArrayList<>();
        }
        try {
            return seatRepository.findByBookingId(bookingId);
        } catch (Exception e) {
            logger.severe("Error getting seats by booking ID " + bookingId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Seat> getSeatsByIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            logger.warning("SeatIds list is null or empty when getting seats");
            return new ArrayList<>();
        }
        try {
            return seatRepository.findAllById(seatIds);
        } catch (Exception e) {
            logger.severe("Error getting seats by IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Seat saveSeat(Seat seat) {
        if (seat == null) {
            logger.warning("Seat is null when saving");
            return null;
        }
        try {
            return seatRepository.save(seat);
        } catch (Exception e) {
            logger.severe("Error saving seat: " + e.getMessage());
            throw new RuntimeException("Failed to save seat", e);
        }
    }

    @Override
    @Transactional
    public void deleteSeat(Long id) {
        if (id == null) {
            logger.warning("Seat ID is null when deleting");
            return;
        }
        try {
            seatRepository.deleteById(id);
            logger.info("Deleted seat with ID: " + id);
        } catch (Exception e) {
            logger.severe("Error deleting seat with ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete seat", e);
        }
    }

    @Override
    @Transactional
    public void initializeSeatsForScreening(Screening screening) {
        if (screening == null || screening.getId() == null) {
            logger.warning("Screening is null or has no ID when initializing seats");
            return;
        }
        
        try {
            // Check if seats already exist for this screening
            List<Seat> existingSeats = seatRepository.findByScreeningId(screening.getId());
            if (!existingSeats.isEmpty()) {
                logger.info("Seats already exist for screening ID: " + screening.getId() + ". Count: " + existingSeats.size());
                return;
            }
            
            logger.info("Initializing seats for screening ID: " + screening.getId());
            
            // Get the total seats to create from screening or cinema capacity
            int totalSeatsToCreate = screening.getTotalSeats();
            if (totalSeatsToCreate <= 0 && screening.getCinema() != null) {
                totalSeatsToCreate = screening.getCinema().getCapacity();
            }
            if (totalSeatsToCreate <= 0) {
                totalSeatsToCreate = 100; // Default fallback
            }
            
            logger.info("Creating " + totalSeatsToCreate + " seats for screening " + screening.getId());
            
            // Calculate optimal layout for the total seats
            int seatsPerRow = 10;
            int totalRows = (int) Math.ceil((double) totalSeatsToCreate / seatsPerRow);
            
            // Ensure we don't exceed 26 rows (A-Z)
            if (totalRows > 26) {
                seatsPerRow = (int) Math.ceil((double) totalSeatsToCreate / 26);
                totalRows = 26;
            }
            
            List<Seat> seats = new ArrayList<>();
            int seatNumber = 1;
            int seatsCreated = 0;
            
            for (int rowIndex = 0; rowIndex < totalRows && seatsCreated < totalSeatsToCreate; rowIndex++) {
                char rowLetter = (char) ('A' + rowIndex);
                String row = String.valueOf(rowLetter);
                
                // Calculate seats for this row
                int seatsInThisRow = Math.min(seatsPerRow, totalSeatsToCreate - seatsCreated);
                
                for (int col = 1; col <= seatsInThisRow; col++) {
                    try {
                        Seat seat = new Seat();
                        seat.setScreening(screening);
                        seat.setRow(row);
                        seat.setSeatColumn(col);
                        seat.setSeatNumber(row + col);
                        seat.setBooked(false);
                        seat.setBookingId(null);
                        seat.setNumber(seatNumber++);
                        
                        // Set price from screening
                        seat.setPrice(screening.getPrice());
                        seat.setType("standard");
                        
                        seats.add(seat);
                        seatsCreated++;
                    } catch (Exception e) {
                        logger.warning("Error creating seat for row " + row + ", column " + col + ": " + e.getMessage());
                    }
                }
            }
            
            // Save all seats in batch
            seatRepository.saveAll(seats);
            
            // Update screening with actual seat count
            screening.setTotalSeats(seats.size());
            screening.setAvailableSeats(seats.size());
            
            logger.info("Successfully created " + seats.size() + " seats for screening ID: " + screening.getId());
        } catch (Exception e) {
            logger.severe("Error initializing seats for screening ID " + screening.getId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to initialize seats for screening", e);
        }
    }

    @Override
    public boolean areSeatsAvailable(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            logger.warning("SeatIds list is null or empty when checking availability");
            return false;
        }
        
        try {
            List<Seat> seats = seatRepository.findAllById(seatIds);
            
            // Check if all requested seats were found
            if (seats.size() != seatIds.size()) {
                logger.warning("Not all requested seats were found. Requested: " + seatIds.size() + ", Found: " + seats.size());
                return false;
            }
            
            // Check if any seat is already booked
            for (Seat seat : seats) {
                if (seat.isBooked()) {
                    logger.warning("Seat " + seat.getId() + " (" + seat.getSeatNumber() + ") is already booked");
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Error checking seat availability: " + e.getMessage());
            return false;
        }
    }
    
    // UPDATED METHODS for admin seat management
    
    @Override
    public List<Seat> getAvailableSeatsByScreeningId(Long screeningId) {
        if (screeningId == null) {
            logger.warning("ScreeningId is null when getting available seats");
            return new ArrayList<>();
        }
        
        try {
            return seatRepository.findByScreeningId(screeningId).stream()
                    .filter(seat -> !seat.isBooked())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error getting available seats for screening " + screeningId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Seat> getBookedSeatsByScreeningId(Long screeningId) {
        if (screeningId == null) {
            logger.warning("ScreeningId is null when getting booked seats");
            return new ArrayList<>();
        }
        
        try {
            return seatRepository.findByScreeningId(screeningId).stream()
                    .filter(Seat::isBooked)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.severe("Error getting booked seats for screening " + screeningId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public int getAvailableSeatsCount(Long screeningId) {
        if (screeningId == null) {
            logger.warning("ScreeningId is null when counting available seats");
            return 0;
        }
        
        try {
            List<Seat> seats = seatRepository.findByScreeningId(screeningId);
            return (int) seats.stream().filter(seat -> !seat.isBooked()).count();
        } catch (Exception e) {
            logger.severe("Error counting available seats for screening " + screeningId + ": " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int getBookedSeatsCount(Long screeningId) {
        if (screeningId == null) {
            logger.warning("ScreeningId is null when counting booked seats");
            return 0;
        }
        
        try {
            List<Seat> seats = seatRepository.findByScreeningId(screeningId);
            return (int) seats.stream().filter(Seat::isBooked).count();
        } catch (Exception e) {
            logger.severe("Error counting booked seats for screening " + screeningId + ": " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    @Transactional
    public void updateSeatStatus(Long seatId, boolean isBooked, Long bookingId) {
        if (seatId == null) {
            logger.warning("SeatId is null when updating seat status");
            return;
        }
        
        try {
            Seat seat = seatRepository.findById(seatId).orElse(null);
            if (seat == null) {
                logger.warning("Seat not found with ID: " + seatId);
                return;
            }
            
            seat.setBooked(isBooked);
            seat.setBookingId(isBooked ? bookingId : null);
            seatRepository.save(seat);
            
            logger.info("Updated seat " + seatId + " (" + seat.getSeatNumber() + ") status to " + (isBooked ? "booked" : "available"));
        } catch (Exception e) {
            logger.severe("Error updating seat status for seat " + seatId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update seat status", e);
        }
    }
    
    @Override
    @Transactional
    public void bulkUpdateSeatStatus(List<Long> seatIds, boolean isBooked, Long bookingId) {
        if (seatIds == null || seatIds.isEmpty()) {
            logger.warning("SeatIds list is null or empty when bulk updating seat status");
            return;
        }
        
        try {
            List<Seat> seats = seatRepository.findAllById(seatIds);
            for (Seat seat : seats) {
                seat.setBooked(isBooked);
                seat.setBookingId(isBooked ? bookingId : null);
            }
            seatRepository.saveAll(seats);
            logger.info("Bulk updated " + seats.size() + " seats status to " + (isBooked ? "booked" : "available"));
        } catch (Exception e) {
            logger.severe("Error bulk updating seat status: " + e.getMessage());
            throw new RuntimeException("Failed to bulk update seat status", e);
        }
    }
    
    // Existing methods for temporary reservations
    
    @Override
    @Transactional
    public void createTemporaryReservations(List<Seat> seats, Long bookingId) {
        if (seats == null || seats.isEmpty() || bookingId == null) {
            logger.warning("Invalid parameters for creating temporary reservations");
            return;
        }
        
        try {
            logger.info("Creating temporary reservations for booking ID: " + bookingId);
            
            // First, mark seats as temporarily reserved in the Seat table
            // This is just visual for the UI, the actual reservation is tracked in SeatReservation
            for (Seat seat : seats) {
                seat.setBooked(true);
                seat.setBookingId(bookingId);
                seatRepository.save(seat);
            }
            
            logger.info("Successfully created temporary reservations for " + seats.size() + " seats");
        } catch (Exception e) {
            logger.severe("Error creating temporary reservations for booking " + bookingId + ": " + e.getMessage());
            throw new RuntimeException("Failed to create temporary reservations", e);
        }
    }
    
    @Override
    @Transactional
    public void releaseTemporaryReservations(Long bookingId) {
        if (bookingId == null) {
            logger.warning("BookingId is null when releasing temporary reservations");
            return;
        }
        
        try {
            logger.info("Releasing temporary reservations for booking ID: " + bookingId);
            
            // Get all seats for this booking
            List<Seat> seats = seatRepository.findByBookingId(bookingId);
            
            // Release the seats
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatRepository.save(seat);
            }
            
            logger.info("Successfully released temporary reservations for " + seats.size() + " seats");
        } catch (Exception e) {
            logger.severe("Error releasing temporary reservations for booking " + bookingId + ": " + e.getMessage());
            throw new RuntimeException("Failed to release temporary reservations", e);
        }
    }
    
    @Override
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredReservations() {
        try {
            logger.info("Starting cleanup of expired seat reservations");
            
            // Find reservations older than 15 minutes
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
            List<SeatReservation> expiredReservations = seatReservationRepository.findByReservationTimeBefore(cutoffTime);
            
            if (expiredReservations.isEmpty()) {
                logger.info("No expired reservations found");
                return;
            }
            
            logger.info("Found " + expiredReservations.size() + " expired reservations");
            
            for (SeatReservation reservation : expiredReservations) {
                Long bookingId = reservation.getBooking().getId();
                logger.info("Processing expired reservation for booking ID: " + bookingId);
                
                // Release the seats
                releaseTemporaryReservations(bookingId);
            }
            
            // Delete the expired reservations
            logger.info("Deleting " + expiredReservations.size() + " expired reservations");
            seatReservationRepository.deleteAll(expiredReservations);
            
            logger.info("Successfully cleaned up expired reservations");
        } catch (Exception e) {
            logger.severe("Error during cleanup of expired reservations: " + e.getMessage());
        }
    }
}