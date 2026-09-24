package com.movie.movieticket.scheduler;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.AdminNotificationService;
import com.movie.movieticket.service.BookingService;
import com.movie.movieticket.service.EmailService;
import com.movie.movieticket.service.NoShowService;
import com.movie.movieticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Component
public class ScheduledTasks {
    
    private static final Logger logger = Logger.getLogger(ScheduledTasks.class.getName());
    
    @Autowired
    private NoShowService noShowService;
    
    @Autowired
    private AdminNotificationService adminNotificationService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Process no-shows automatically every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void processNoShows() {
        logger.info("Running scheduled no-show processing task");
        try {
            int processed = noShowService.processPendingNoShows();
            logger.info("Processed " + processed + " no-shows");
            
            // Update admin dashboard counts after processing
            if (processed > 0 && adminNotificationService != null) {
                try {
                    adminNotificationService.notifyNoShowsProcessed(processed);
                } catch (Exception e) {
                    logger.warning("Failed to send admin notification: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("Error in processNoShows: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send daily summary at 23:00 every day
     */
    @Scheduled(cron = "0 0 23 * * *")
    public void sendDailySummary() {
        logger.info("Creating daily summary notification");
        try {
            adminNotificationService.sendDailySummary();
        } catch (Exception e) {
            logger.severe("Error in sendDailySummary: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notify about pending counter payments every 2 hours
     */
    @Scheduled(cron = "0 0 */2 * * *")
    public void notifyPendingCounterPayments() {
        logger.info("Checking pending counter payments");
        try {
            int count = bookingService.getBookingsByPaymentMethodAndStatus("PAY_AT_COUNTER", "PENDING").size();
            if (count > 0) {
                adminNotificationService.notifyPendingCounterPayments(count);
                logger.info("Created notification for " + count + " pending counter payments");
            }
        } catch (Exception e) {
            logger.severe("Error in notifyPendingCounterPayments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check for bookings that need reminders or need to be marked as no-shows
     * Runs every 1 minute for more precise timing
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void checkBookingsBeforeScreening() {
        logger.info("Running scheduled task: checkBookingsBeforeScreening");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Get all bookings with payment method PAY_AT_COUNTER and payment status PENDING
            List<Booking> pendingCounterPayments = bookingService.getBookingsByPaymentMethodAndStatus("PAY_AT_COUNTER", "PENDING");
            logger.info("Found " + pendingCounterPayments.size() + " pending counter payment bookings");
            
            for (Booking booking : pendingCounterPayments) {
                try {
                    // Skip if booking is already cancelled or marked as no-show
                    if ("CANCELLED".equals(booking.getStatus()) || "NO_SHOW".equals(booking.getStatus()) 
                        || Boolean.TRUE.equals(booking.getMarkedAsNoShow())) {
                        continue;
                    }
                    
                    // Skip if booking is already paid
                    if (booking.isPaid() || "COMPLETED".equals(booking.getPaymentStatus())) {
                        continue;
                    }
                    
                    LocalDateTime screeningTime = booking.getScreening().getScreeningTime();
                    
                    // Calculate time until screening
                    LocalDateTime thirtyMinBeforeScreening = screeningTime.minusMinutes(30);
                    LocalDateTime fifteenMinBeforeScreening = screeningTime.minusMinutes(15);
                    
                    // 30 minutes before screening: Send payment reminder ONLY
                    if (now.isAfter(thirtyMinBeforeScreening.minusMinutes(2)) && now.isBefore(thirtyMinBeforeScreening.plusMinutes(2))) {
                        // Check if reminder was already sent
                        if (!booking.getReminderSent()) {
                            logger.info("Sending payment reminder for booking ID: " + booking.getId() + " (30 min before screening)");
                            try {
                                emailService.sendPaymentReminderEmail(booking);
                                booking.setReminderSent(true);
                                bookingService.saveBooking(booking);
                                logger.info("Successfully sent payment reminder for booking ID: " + booking.getId());
                            } catch (Exception e) {
                                logger.severe("Failed to send payment reminder email for booking " + booking.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    // 15 minutes before screening: Mark as no-show ONLY if still unpaid
                    if (now.isAfter(fifteenMinBeforeScreening.minusMinutes(2)) && now.isBefore(fifteenMinBeforeScreening.plusMinutes(2))) {
                        // Double check if booking is still unpaid
                        if (!booking.isPaid() && "PENDING".equals(booking.getPaymentStatus())) {
                            logger.info("Auto-marking booking ID: " + booking.getId() + " as no-show (15 min before screening - still unpaid)");
                            
                            // Mark as no-show
                            noShowService.markAsNoShow(booking);
                            
                            // Add admin note about auto no-show
                            booking.setAdminNotes(booking.getAdminNotes() != null ? 
                                booking.getAdminNotes() + " | Auto-marked as no-show 15 minutes before screening on " + now : 
                                "Auto-marked as no-show 15 minutes before screening on " + now);
                            bookingService.saveBooking(booking);
                            
                            // Release seats
                            bookingService.releaseSeats(booking.getId());
                            
                            // Send void notification email
                            try {
                                emailService.sendBookingVoidedEmail(booking);
                                logger.info("Successfully sent booking voided email for booking ID: " + booking.getId());
                            } catch (Exception e) {
                                logger.severe("Failed to send booking voided email for booking " + booking.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                            
                            logger.info("Successfully voided booking ID: " + booking.getId());
                        } else {
                            logger.info("Booking ID: " + booking.getId() + " is paid, skipping no-show marking");
                        }
                    }
                } catch (Exception e) {
                    logger.severe("Error processing booking ID: " + booking.getId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.severe("Error in checkBookingsBeforeScreening: " + e.getMessage());
            e.printStackTrace();
        }
    }
}