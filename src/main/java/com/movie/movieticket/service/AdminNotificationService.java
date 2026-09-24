package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Notification;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.User;

import java.util.List;

public interface AdminNotificationService {
    
    /**
     * Notify admins about a new booking
     * @param booking The new booking
     */
    void notifyNewBooking(Booking booking);
    
    /**
     * Notify admins about a payment
     * @param payment The payment
     */
    void notifyPayment(Payment payment);
    
    /**
     * Notify admins about a no-show
     * @param booking The booking marked as no-show
     */
    void notifyNoShow(Booking booking);
    
    /**
     * Notify admins about a user being blocked
     * @param user The blocked user
     * @param reason The reason for blocking
     */
    void notifyUserBlocked(User user, String reason);
    
    /**
     * Send a daily summary of bookings and payments to admins
     */
    void sendDailySummary();
    
    /**
     * Send a notification about pending counter payments
     * @param count Number of pending payments
     */
    void notifyPendingCounterPayments(int count);
    
    /**
     * Notify admins about processed no-shows
     * @param count Number of no-shows processed
     */
    void notifyNoShowsProcessed(int count);
    
    /**
     * Create an in-app notification
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type (INFO, WARNING, SUCCESS, ERROR)
     * @param entityType Type of entity (BOOKING, PAYMENT, USER, etc.)
     * @param entityId ID of the related entity
     * @param link URL to view the entity
     * @return The created notification
     */
    Notification createNotification(String title, String message, String type, String entityType, Long entityId, String link);
    
    /**
     * Get all notifications
     * @return List of all notifications
     */
    List<Notification> getAllNotifications();
    
    /**
     * Get unread notifications
     * @return List of unread notifications
     */
    List<Notification> getUnreadNotifications();
    
    /**
     * Get recent notifications (limited to 10)
     * @return List of recent notifications
     */
    List<Notification> getRecentNotifications();
    
    /**
     * Mark a notification as read
     * @param id Notification ID
     */
    void markAsRead(Long id);
    
    /**
     * Mark all notifications as read
     */
    void markAllAsRead();
    
    /**
     * Get count of unread notifications
     * @return Count of unread notifications
     */
    long getUnreadCount();
}