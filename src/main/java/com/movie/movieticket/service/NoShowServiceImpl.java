package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.model.User;
import com.movie.movieticket.repository.BookingRepository;
import com.movie.movieticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Service
public class NoShowServiceImpl implements NoShowService {
    
    private static final Logger logger = Logger.getLogger(NoShowServiceImpl.class.getName());
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private ScreeningService screeningService;
    
    @Autowired
    private AdminNotificationService adminNotificationService;
    
    @Autowired
    private BookingService bookingService;
    
    @Override
    @Transactional
    public Booking markAsNoShow(Booking booking) {
        return markAsNoShow(booking, true);
    }
    
    @Override
    @Transactional
    public Booking markAsNoShow(Booking booking, boolean processUser) {
        if (booking == null) {
            logger.warning("Cannot mark null booking as no-show");
            return null;
        }
        
        logger.info("Marking booking ID: " + booking.getId() + " as no-show");
        
        // Get current time for logging
        LocalDateTime now = LocalDateTime.now();
        String timeStamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Update booking status
        booking.setStatus("NO_SHOW");
        booking.setMarkedAsNoShow(true);
        booking.setNoShowProcessed(true);
        
        // Add timestamp to admin notes
        String noShowNote = "Marked as no-show on " + timeStamp;
        if (booking.getAdminNotes() != null && !booking.getAdminNotes().isEmpty()) {
            booking.setAdminNotes(booking.getAdminNotes() + " | " + noShowNote);
        } else {
            booking.setAdminNotes(noShowNote);
        }
        
        // Save the booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notification to admin about the no-show
        try {
            adminNotificationService.notifyNoShow(savedBooking);
            logger.info("No-show notification sent for booking ID: " + savedBooking.getId());
        } catch (Exception e) {
            logger.warning("Failed to send no-show notification: " + e.getMessage());
        }
        
        // Process user's no-show count if requested
        if (processUser && booking.getUser() != null) {
            User user = booking.getUser();
            updateUserNoShowStatus(user);
        }
        
        // Release the seats
        releaseSeats(booking);
        
        return savedBooking;
    }
    
    @Override
    public List<Booking> findPendingNoShows() {
        // Find bookings that are past their screening time and not marked as no-show
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1); // 1 hour after screening time
        return bookingRepository.findPendingNoShows(cutoffTime);
    }
    
    @Override
    @Transactional
    public int processPendingNoShows() {
        List<Booking> pendingNoShows = findPendingNoShows();
        int count = 0;
        
        for (Booking booking : pendingNoShows) {
            try {
                markAsNoShow(booking);
                count++;
            } catch (Exception e) {
                logger.severe("Error processing no-show for booking ID: " + booking.getId() + " - " + e.getMessage());
            }
        }
        
        // Send notification about bulk processing if any were processed
        if (count > 0) {
            try {
                adminNotificationService.notifyNoShowsProcessed(count);
                logger.info("Bulk no-show processing notification sent for " + count + " bookings");
            } catch (Exception e) {
                logger.warning("Failed to send bulk no-show processing notification: " + e.getMessage());
            }
        }
        
        return count;
    }
    
    @Override
    @Transactional
    public User updateUserNoShowStatus(User user) {
        if (user == null) {
            logger.warning("Cannot update no-show status for null user");
            return null;
        }
        
        // Increment no-show count
        Integer noShowCount = user.getNoShowCount();
        if (noShowCount == null) {
            noShowCount = 0;
        }
        noShowCount++;
        user.setNoShowCount(noShowCount);
        
        // Apply restrictions based on no-show count
        if (noShowCount == 1) {
            // First no-show: Just a warning
            user.setBlockedUntil(null);
            user.setIsBlocked(false);
            
            // Send warning email - FIXED: Removed MessagingException catch
            try {
                emailService.sendNoShowWarningEmail(user, 1);
                logger.info("No-show warning email sent for user: " + user.getEmail());
            } catch (Exception e) {
                logger.warning("Failed to send no-show warning email: " + e.getMessage());
            }
        } else if (noShowCount == 2) {
            // Second no-show: Block for 7 days
            LocalDateTime blockedUntil = LocalDateTime.now().plusDays(7);
            user.setBlockedUntil(blockedUntil);
            user.setIsBlocked(false);
            
            // Send warning email - FIXED: Removed MessagingException catch
            try {
                emailService.sendNoShowWarningEmail(user, 2);
                logger.info("No-show warning email sent for user: " + user.getEmail());
            } catch (Exception e) {
                logger.warning("Failed to send no-show warning email: " + e.getMessage());
            }
        } else if (noShowCount >= 3) {
            // Third or more no-show: Permanent block
            user.setBlockedUntil(null);
            user.setIsBlocked(true);
            
            // Send blocked email - FIXED: Removed MessagingException catch
            try {
                emailService.sendNoShowBlockedEmail(user);
                logger.info("No-show blocked email sent for user: " + user.getEmail());
            } catch (Exception e) {
                logger.warning("Failed to send no-show blocked email: " + e.getMessage());
            }
            
            // Send notification to admin about user being blocked due to no-shows
            try {
                adminNotificationService.notifyUserBlocked(user, "Automatically blocked due to " + noShowCount + " no-shows");
                logger.info("User blocked notification sent for user ID: " + user.getId());
            } catch (Exception e) {
                logger.warning("Failed to send user blocked notification: " + e.getMessage());
            }
        }
        
        return userRepository.save(user);
    }
    
    @Override
    public List<User> getUsersWithNoShows() {
        return userRepository.findByNoShowCountGreaterThan(0);
    }
    
    @Override
    @Transactional
    public User resetUserNoShowStatus(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warning("Cannot reset no-show status for non-existent user: " + userId);
            return null;
        }
        // Reset no-show count and clear all blocks
        user.setNoShowCount(0);
        user.setBlockedUntil(null);
        user.setIsBlocked(false);
        user.setBlockReason(null);
        user.setLastNoShow(null);
        
        logger.info("Reset no-show status for user ID: " + userId + 
                   ", Email: " + user.getEmail() + 
                   ", isBlocked set to: " + user.getIsBlocked() + 
                   ", blockedUntil set to: null");
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public void processNoShow(Booking booking, String processedBy) {
        if (booking == null) {
            logger.warning("Cannot process null booking as no-show");
            return;
        }
        
        // Get current time for logging
        LocalDateTime now = LocalDateTime.now();
        String timeStamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Mark as no-show
        booking.setStatus("NO_SHOW");
        booking.setMarkedAsNoShow(true);
        booking.setNoShowProcessed(true);
        booking.setProcessedBy(processedBy);
        
        // Add timestamp to admin notes
        String noShowNote = "Processed as no-show by " + processedBy + " on " + timeStamp;
        if (booking.getAdminNotes() != null && !booking.getAdminNotes().isEmpty()) {
            booking.setAdminNotes(booking.getAdminNotes() + " | " + noShowNote);
        } else {
            booking.setAdminNotes(noShowNote);
        }
        
        // Save the booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // Send notification to admin about the manual no-show processing
        try {
            adminNotificationService.notifyNoShow(savedBooking);
            logger.info("Manual no-show notification sent for booking ID: " + savedBooking.getId());
        } catch (Exception e) {
            logger.warning("Failed to send manual no-show notification: " + e.getMessage());
        }
        
        // Update user's no-show count
        if (booking.getUser() != null) {
            updateUserNoShowStatus(booking.getUser());
        }
        
        // Release the seats
        releaseSeats(booking);
    }
    
    @Override
    public void sendNoShowWarning(User user, Booking booking) {
        if (user == null || booking == null) {
            logger.warning("Cannot send no-show warning for null user or booking");
            return;
        }
        
        try {
            emailService.sendNoShowWarning(user, booking);
            logger.info("No-show warning sent for user: " + user.getEmail() + ", booking: " + booking.getConfirmationCode());
        } catch (Exception e) {
            logger.warning("Failed to send no-show warning email: " + e.getMessage());
        }
    }
    
    @Override
    public List<Booking> getUserNoShowBookings(Long userId) {
        try {
            logger.info("Fetching no-show bookings for user ID: " + userId);
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                logger.warning("User not found with ID: " + userId);
                return List.of();
            }
            
            // Use the existing BookingService method to get no-show bookings by user email
            List<Booking> noShowBookings = bookingService.getNoShowBookingsByUserEmail(user.getEmail());
            
            // Sort by screening time descending (most recent first)
            List<Booking> sortedBookings = noShowBookings.stream()
                .filter(booking -> booking.getScreening() != null && booking.getScreening().getScreeningTime() != null)
                .sorted((b1, b2) -> b2.getScreening().getScreeningTime().compareTo(b1.getScreening().getScreeningTime()))
                .collect(Collectors.toList());
            
            logger.info("Found " + sortedBookings.size() + " no-show bookings for user: " + user.getEmail());
            
            return sortedBookings;
        } catch (Exception e) {
            logger.severe("Error fetching no-show bookings for user ID: " + userId + " - " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * Helper method to release seats for a no-show booking
     */
    private void releaseSeats(Booking booking) {
        if (booking == null) {
            logger.warning("Cannot release seats for null booking");
            return;
        }
        
        try {
            // Get the seats for this booking
            List<Seat> seats = seatService.getSeatsByBookingId(booking.getId());
            logger.info("Found " + seats.size() + " seats to release for booking ID: " + booking.getId());
            
            // Update seats to be available again
            for (Seat seat : seats) {
                seat.setBooked(false);
                seat.setBookingId(null);
                seatService.saveSeat(seat);
                logger.info("Released seat ID: " + seat.getId() + " for booking ID: " + booking.getId());
            }
            
            // Update available seats count in screening
            Screening screening = booking.getScreening();
            if (screening != null) {
                int currentAvailableSeats = screening.getAvailableSeats();
                int seatsToRelease = seats.size();
                
                screening.setAvailableSeats(currentAvailableSeats + seatsToRelease);
                screeningService.saveScreening(screening);
                
                logger.info("Updated screening ID: " + screening.getId() + 
                           " available seats from " + currentAvailableSeats + 
                           " to " + screening.getAvailableSeats());
            } else {
                logger.warning("Cannot update screening for booking ID: " + booking.getId() + " - screening is null");
            }
        } catch (Exception e) {
            logger.severe("Error releasing seats for booking ID: " + booking.getId() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}