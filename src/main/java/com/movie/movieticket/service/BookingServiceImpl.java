package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.User;
import com.movie.movieticket.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = Logger.getLogger(BookingServiceImpl.class.getName());

    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ScreeningService screeningService;
    
    @Autowired
    private SeatService seatService;

    @Override
    public Booking saveBooking(Booking booking) {
        // Ensure booking_date is set
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDate.now());
        }
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    @Override
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUserOrderByBookingTimeDesc(user);
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        logger.info("Deleting booking with ID: " + id);
        
        // First release the seats before deleting the booking
        try {
            releaseSeats(id);
        } catch (Exception e) {
            logger.warning("Error releasing seats before deleting booking " + id + ": " + e.getMessage());
            // Continue with deletion even if seat release fails
        }
        
        // Then delete the booking
        bookingRepository.deleteById(id);
        logger.info("Successfully deleted booking with ID: " + id);
    }
    
    @Override
    public Booking updateBookingPaymentStatus(Long bookingId, String paymentStatus, String paymentMethod) {
        Booking booking = getBookingById(bookingId);
        if (booking != null) {
            booking.setPaymentStatus(paymentStatus);
            booking.setPaymentMethod(paymentMethod);
            
            if ("COMPLETED".equals(paymentStatus)) {
                booking.setPaid(true);
                booking.setPaymentTime(LocalDateTime.now());
                booking.setStatus("CONFIRMED");
            } else if ("FAILED".equals(paymentStatus)) {
                booking.setPaid(false);
            } else if ("REFUNDED".equals(paymentStatus)) {
                booking.setPaid(false);
            }
            
            return bookingRepository.save(booking);
        }
        return null;
    }
    
    @Override
    public List<Booking> getBookingsByPaymentStatus(String paymentStatus) {
        // Adapt to use existing methods
        if ("COMPLETED".equals(paymentStatus)) {
            return bookingRepository.findByPaid(true);
        } else if ("PENDING".equals(paymentStatus)) {
            return bookingRepository.findByPaid(false);
        }
        return List.of(); // Return empty list if status doesn't match
    }
    
    @Override
    public List<Booking> getBookingsByPaymentMethod(String paymentMethod) {
        return bookingRepository.findByPaymentMethod(paymentMethod);
    }
    
    @Override
    public List<Booking> getBookingsByPaymentMethodAndStatus(String paymentMethod, String paymentStatus) {
        // Adapt to use existing methods
        boolean paid = "COMPLETED".equals(paymentStatus);
        return bookingRepository.findByPaymentMethodAndPaid(paymentMethod, paid);
    }
    
    /**
     * FIXED: Get valid pending counter payments - ONLY hide after 15-minute cutoff
     */
    @Override
    public List<Booking> getValidPendingCounterPayments() {
        logger.info("=== GETTING VALID PENDING COUNTER PAYMENTS ===");
        
        // Get all bookings with payment method PAY_AT_COUNTER and not paid
        List<Booking> allPendingCounterPayments = bookingRepository.findByPaymentMethodAndPaid("PAY_AT_COUNTER", false);
        
        LocalDateTime now = LocalDateTime.now();
        logger.info("Current time: " + now);
        logger.info("Found " + allPendingCounterPayments.size() + " total PAY_AT_COUNTER unpaid bookings");
        
        List<Booking> validBookings = allPendingCounterPayments.stream()
            .filter(booking -> {
                logger.info("--- Checking booking ID: " + booking.getId() + " ---");
                logger.info("Status: " + booking.getStatus());
                logger.info("Payment Status: " + booking.getPaymentStatus());
                logger.info("Marked as No Show: " + booking.getMarkedAsNoShow());
                logger.info("Is Paid: " + booking.isPaid());
                
                // Skip if booking is already cancelled or marked as no-show
                if ("CANCELLED".equals(booking.getStatus()) || "NO_SHOW".equals(booking.getStatus()) 
                    || Boolean.TRUE.equals(booking.getMarkedAsNoShow())) {
                    logger.info("FILTERED OUT: Booking is cancelled/no-show");
                    return false;
                }
                
                // Skip if booking is already paid (double check)
                if (booking.isPaid() || "COMPLETED".equals(booking.getPaymentStatus())) {
                    logger.info("FILTERED OUT: Booking is already paid");
                    return false;
                }
                
                // Check screening time and 15-minute cutoff
                if (booking.getScreening() != null && booking.getScreening().getScreeningTime() != null) {
                    LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
                    LocalDateTime fifteenMinBeforeScreening = screeningTime.minusMinutes(15);
                    
                    logger.info("Screening time: " + screeningTime);
                    logger.info("15-min cutoff: " + fifteenMinBeforeScreening);
                    logger.info("Current time: " + now);
                    logger.info("Is current time AFTER 15-min cutoff? " + now.isAfter(fifteenMinBeforeScreening));
                    
                    // CRITICAL: Only hide if current time is AFTER the 15-minute cutoff
                    if (now.isAfter(fifteenMinBeforeScreening)) {
                        logger.info("FILTERED OUT: Past 15-minute cutoff");
                        return false;
                    } else {
                        logger.info("KEEPING: Still before 15-minute cutoff");
                        return true;
                    }
                } else {
                    logger.info("KEEPING: No screening time found");
                    return true;
                }
            })
            .collect(Collectors.toList());
        
        logger.info("=== RESULT: " + validBookings.size() + " valid counter payment bookings ===");
        for (Booking booking : validBookings) {
            logger.info("Valid booking ID: " + booking.getId() + " - " + booking.getConfirmationCode());
        }
        
        return validBookings;
    }
    
    @Override
    public List<Booking> getBookingsByStatus(String status) {
        return bookingRepository.findByStatus(status);
    }
    
    @Override
    public List<Booking> getBookingsByStatus(String status, Pageable pageable) {
        return bookingRepository.findByStatusOrderByBookingTimeDesc(status, pageable);
    }
    
    // Implement new methods for admin dashboard
    @Override
    public long getBookingCount() {
        return bookingRepository.countBookings();
    }
    
    @Override
    public List<Booking> getRecentBookings(int limit) {
        if (limit <= 5) {
            return bookingRepository.findTop5ByOrderByBookingTimeDesc();
        } else {
            return bookingRepository.findAll(
                PageRequest.of(0, limit)
            ).getContent(); // Convert Page to List
        }
    }
    
    @Override
    public long getTodayBookingCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return bookingRepository.countByBookingTimeBetween(startOfDay, endOfDay);
    }
    
    @Override
    public long getBookingCountBetweenDates(LocalDateTime start, LocalDateTime end) {
        return bookingRepository.countByBookingTimeBetween(start, end);
    }
    
    @Override
    public List<Booking> getBookingsByMovieId(Long movieId) {
        return bookingRepository.findByMovieId(movieId);
    }
    
    @Override
    public List<Booking> getBookingsByScreeningId(Long screeningId) {
        return bookingRepository.findByScreeningId(screeningId);
    }
    
    // Updated releaseSeats method with better error handling
    @Override
    @Transactional
    public void releaseSeats(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking == null) {
            logger.warning("Cannot release seats for non-existent booking: " + bookingId);
            return;
        }
        try {
            // Get the seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(bookingId);
            logger.info("Found " + seats.size() + " seats to release for booking ID: " + bookingId);
            
            // Update seats to be available again
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatService.saveSeat(seat);
                logger.info("Released seat ID: " + seat.getId() + " (" + seat.getSeatNumber() + ") for booking ID: " + bookingId);
            }
            
            // Get the screening
            Screening screening = booking.getScreening();
            
            if (screening != null) {
                // Update available seats in the screening
                int currentAvailableSeats = screening.getAvailableSeats();
                int seatsToRelease = seats.size();
                
                screening.setAvailableSeats(currentAvailableSeats + seatsToRelease);
                screeningService.saveScreening(screening);
                
                logger.info("Updated screening ID: " + screening.getId() + 
                           " available seats from " + currentAvailableSeats + 
                           " to " + screening.getAvailableSeats());
            } else {
                logger.warning("Screening is null for booking ID: " + bookingId);
            }
            
            logger.info("Successfully released all seats for booking ID: " + bookingId);
        } catch (Exception e) {
            logger.severe("Error releasing seats for booking ID: " + bookingId + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to release seats: " + e.getMessage());
        }
    }
    
    @Override
    public long countNoShowBookings() {
        return bookingRepository.countByStatus("NO_SHOW");
    }
    
    @Override
    public long countPendingNoShowBookings() {
        return bookingRepository.countByMarkedAsNoShowTrueAndNoShowProcessedFalse();
    }
    
    @Override
    public long countPendingPayments() {
        return bookingRepository.countByPaidFalseAndStatusNot("CANCELLED");
    }
    
    @Override
    public long countPendingCounterPayments() {
        return getValidPendingCounterPayments().size();
    }
    
    @Override
    public BigDecimal calculateRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        List<Booking> bookings = bookingRepository.findByBookingTimeBetweenAndPaidTrue(start, end);
        BigDecimal total = BigDecimal.ZERO;
        for (Booking booking : bookings) {
            if (booking.getTotalAmount() != null) {
                total = total.add(BigDecimal.valueOf(booking.getTotalAmount()));
            }
        }
        return total;
    }
    
    @Override
    public List<Booking> findRecentNoShowBookings(int limit) {
        return bookingRepository.findByStatusOrderByBookingTimeDesc("NO_SHOW", PageRequest.of(0, limit));
    }
    
    @Override
    public List<Booking> getNoShowBookingsByProcessedStatus(boolean processed, Pageable pageable) {
        return bookingRepository.findByMarkedAsNoShowAndProcessed(processed, pageable);
    }
    
    @Override
    public List<Booking> getNoShowBookingsByUser(Long userId) {
        return bookingRepository.findNoShowBookingsByUserId(userId);
    }
    
    // Implement new methods for filtering no-shows
    @Override
    public List<Booking> getNoShowBookingsBetweenDates(LocalDateTime fromDate, LocalDateTime toDate) {
        return bookingRepository.findByStatusNoShowAndScreeningTimeBetween(fromDate, toDate);
    }
    
    @Override
    public List<Booking> getNoShowBookingsBetweenDatesByPaymentStatus(LocalDateTime fromDate, LocalDateTime toDate, boolean isPaid) {
        return bookingRepository.findByStatusNoShowAndScreeningTimeBetweenAndPaid(fromDate, toDate, isPaid);
    }
    
    @Override
    public List<Booking> getPotentialNoShows(LocalDateTime cutoffTime) {
        return bookingRepository.findPendingNoShows(cutoffTime);
    }
    
    @Override
    public List<Booking> getNoShowBookingsByUserEmail(String email) {
        return bookingRepository.findNoShowBookingsByUserEmail(email);
    }
    
    @Override
    public List<Booking> getAllMarkedAsNoShow() {
        return bookingRepository.findAllMarkedAsNoShow();
    }
}