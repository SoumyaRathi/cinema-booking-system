package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Notification;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.User;
import com.movie.movieticket.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {
    
    private static final Logger logger = Logger.getLogger(AdminNotificationServiceImpl.class.getName());
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Value("${admin.notification.email:admin@example.com}")
    private String adminEmail;
    
    @Value("${admin.notification.enabled:true}")
    private boolean notificationsEnabled;

    // FIXED: Use relative paths without context path
    private String buildUrl(String path) {
        // Return relative path without context path - Spring Boot will handle routing
        return path;
    }

    // Fix existing notification URLs on application startup
    @PostConstruct
    public void fixExistingNotificationUrls() {
        try {
            logger.info("Checking and fixing existing notification URLs...");
            
            List<Notification> allNotifications = notificationRepository.findAll();
            int updatedCount = 0;
            
            for (Notification notification : allNotifications) {
                if (notification.getLink() != null && 
                    notification.getLink().startsWith("/cinema-booking-system-0.0.1-SNAPSHOT")) {
                    
                    // Remove the context path from the URL
                    String oldLink = notification.getLink();
                    String newLink = notification.getLink().replace("/cinema-booking-system-0.0.1-SNAPSHOT", "");
                    notification.setLink(newLink);
                    notificationRepository.save(notification);
                    updatedCount++;
                    
                    logger.info("Updated notification ID " + notification.getId() + 
                               " URL from: " + oldLink + " to: " + newLink);
                }
            }
            
            logger.info("Fixed " + updatedCount + " notification URLs");
        } catch (Exception e) {
            logger.severe("Error fixing notification URLs: " + e.getMessage());
        }
    }

    @Override
    public void notifyNewBooking(Booking booking) {
        if (!notificationsEnabled) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for new booking ID: " + booking.getId());
            
            // Create in-app notification only (removed email sending)
            String notificationMessage = "New booking for " + booking.getScreening().getMovie().getTitle() + 
                                        " by " + booking.getUser().getFirstName() + " " + booking.getUser().getLastName();
            
            String link = buildUrl("/admin/bookings/view/" + booking.getId());
            logger.info("Generated notification link: " + link);
            
            createNotification(
                "New Booking: " + booking.getConfirmationCode(),
                notificationMessage,
                "INFO",
                "BOOKING",
                booking.getId(),
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for new booking: " + e.getMessage());
        }
    }

    @Override
    public void notifyPayment(Payment payment) {
        if (!notificationsEnabled) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for payment ID: " + payment.getId());
            
            Booking booking = payment.getBooking();
            
            // Create in-app notification only (removed email sending)
            String notificationMessage = "Payment of ₱" + String.format("%.2f", payment.getAmount()) + 
                                        " received for booking " + booking.getConfirmationCode();
            
            String link = buildUrl("/admin/bookings/view/" + booking.getId());
            logger.info("Generated payment notification link: " + link);
            
            createNotification(
                "Payment Received: " + booking.getConfirmationCode(),
                notificationMessage,
                "SUCCESS",
                "PAYMENT",
                payment.getId(),
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for payment: " + e.getMessage());
        }
    }

    @Override
    public void notifyNoShow(Booking booking) {
        if (!notificationsEnabled) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for no-show booking ID: " + booking.getId());
            
            // Enhanced notification message with more details - changed "failed to attend" to "failed to pay"
            String userInfo = booking.getUser().getFirstName() + " " + booking.getUser().getLastName() + 
                             " (" + booking.getUser().getEmail() + ")";
            String movieInfo = booking.getScreening().getMovie().getTitle();
            String screeningTime = booking.getScreening().getScreeningTime()
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            
            String notificationMessage = "No-show detected: " + userInfo + " failed to pay for " + 
                                        movieInfo + " screening on " + screeningTime + 
                                        ". Booking " + booking.getConfirmationCode() + " has been processed.";
            
            String link = buildUrl("/admin/bookings/view/" + booking.getId());
            logger.info("Generated no-show notification link: " + link);
            
            createNotification(
                "No-Show Alert: " + booking.getConfirmationCode(),
                notificationMessage,
                "WARNING",
                "NO_SHOW",
                booking.getId(),
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for no-show: " + e.getMessage());
        }
    }
    
    @Override
    public void notifyUserBlocked(User user, String reason) {
        if (!notificationsEnabled) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for blocked user ID: " + user.getId());
            
            // Create in-app notification only (removed email sending)
            String blockType = user.getBlockedUntil() != null ? "temporarily" : "permanently";
            String notificationMessage = "User " + user.getFirstName() + " " + user.getLastName() + 
                                        " (" + user.getEmail() + ") has been " + blockType + " blocked. Reason: " + reason;
            
            String link = buildUrl("/admin/users/view/" + user.getId());
            logger.info("Generated user blocked notification link: " + link);
            
            createNotification(
                "User Blocked: " + user.getEmail(),
                notificationMessage,
                "ERROR",
                "USER",
                user.getId(),
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for blocked user: " + e.getMessage());
        }
    }
    
    @Override
    public void sendDailySummary() {
        if (!notificationsEnabled) {
            return;
        }
         try {
            logger.info("Creating dashboard notification for daily summary");
             // Get today's bookings and payments
            long todayBookings = bookingService.getTodayBookingCount();
            double todayRevenue = paymentService.getTodayRevenue();
            
            // Get pending counter payments
            List<Booking> pendingCounterPayments = bookingService.getBookingsByPaymentMethodAndStatus("PAY_AT_COUNTER", "PENDING");
            
            // Create in-app notification only (removed email sending)
            String notificationMessage = "Today's summary: " + todayBookings + " bookings, ₱" + 
                                        String.format("%.2f", todayRevenue) + " revenue, " + 
                                        pendingCounterPayments.size() + " pending counter payments";
            
            String link = buildUrl("/admin/dashboard");
            logger.info("Generated daily summary notification link: " + link);
            
            createNotification(
                "Daily Summary",
                notificationMessage,
                "INFO",
                "SUMMARY",
                null,
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for daily summary: " + e.getMessage());
        }
    }
    
    @Override
    public void notifyPendingCounterPayments(int count) {
        if (!notificationsEnabled || count == 0) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for " + count + " pending counter payments");
            
            // Create in-app notification only (removed email sending)
            String notificationMessage = count + " pending counter payments require attention";
            
            String link = buildUrl("/admin/bookings/counter-payments");
            logger.info("Generated pending counter payments notification link: " + link);
            
            createNotification(
                "Pending Counter Payments: " + count,
                notificationMessage,
                "WARNING",
                "PAYMENT",
                null,
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for pending counter payments: " + e.getMessage());
        }
    }
    
    @Override
    public void notifyNoShowsProcessed(int count) {
        if (!notificationsEnabled || count == 0) {
            return;
        }
        
        try {
            logger.info("Creating dashboard notification for " + count + " processed no-shows");
            
            // Enhanced notification message for bulk processing
            String notificationMessage = count + " no-show bookings have been automatically processed. " +
                                        "Seats have been released and users have been notified. " +
                                        "Check the no-shows section for details.";
            
            String link = buildUrl("/admin/no-shows");
            logger.info("Generated no-shows processed notification link: " + link);
            
            createNotification(
                "No-Shows Processed: " + count,
                notificationMessage,
                "INFO",
                "NO_SHOW",
                null,
                link
            );
            
        } catch (Exception e) {
            logger.severe("Error creating notification for processed no-shows: " + e.getMessage());
        }
    }
    
    @Override
    public Notification createNotification(String title, String message, String type, String entityType, Long entityId, String link) {
        try {
            logger.info("Creating in-app notification: " + title + " with link: " + link);
            
            Notification notification = new Notification(title, message, type, entityType, entityId, link);
            return notificationRepository.save(notification);
        } catch (Exception e) {
            logger.severe("Error creating notification: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Override
    public List<Notification> getUnreadNotifications() {
        return notificationRepository.findByIsReadOrderByCreatedAtDesc(false);
    }
    
    @Override
    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }
    
    @Override
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            logger.info("Marked notification as read: " + id);
        });
    }
    
    @Override
    public void markAllAsRead() {
        List<Notification> unreadNotifications = notificationRepository.findByIsReadOrderByCreatedAtDesc(false);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        logger.info("Marked all notifications as read: " + unreadNotifications.size() + " notifications");
    }
    
    @Override
    public long getUnreadCount() {
        return notificationRepository.countUnreadNotifications();
    }
    
    // Keep this method private for potential future use, but it's not called anymore
    private void sendAdminEmail(String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }
}