package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.User;

import java.util.List;

public interface NoShowService {
    
    /**
     * Process a booking as a no-show
     * @param booking The booking to mark as no-show
     * @return The updated booking
     */
    Booking markAsNoShow(Booking booking);
    
    /**
     * Process a booking as a no-show and update user's no-show count
     * @param booking The booking to mark as no-show
     * @param processUser Whether to update the user's no-show count
     * @return The updated booking
     */
    Booking markAsNoShow(Booking booking, boolean processUser);
    
    /**
     * Find all bookings that should be marked as no-shows
     * @return List of bookings that are no-shows
     */
    List<Booking> findPendingNoShows();
    
    /**
     * Process all pending no-shows
     * @return Number of bookings processed
     */
    int processPendingNoShows();
    
    /**
     * Update user's no-show count and apply appropriate restrictions
     * @param user The user to update
     * @return The updated user
     */
    User updateUserNoShowStatus(User user);
    
    /**
     * Get all users with no-show history
     * @return List of users with no-shows
     */
    List<User> getUsersWithNoShows();
    
    /**
     * Reset a user's no-show count and remove restrictions
     * @param userId The user ID
     * @return The updated user
     */
    User resetUserNoShowStatus(Long userId);
    
    /**
     * Process a no-show booking and update user's no-show count
     * @param booking The booking to process
     * @param processedBy The admin who processed the no-show
     */
    void processNoShow(Booking booking, String processedBy);
    
    /**
     * Send a no-show warning email to a user
     * @param user The user to send the warning to
     * @param booking The no-show booking
     */
    void sendNoShowWarning(User user, Booking booking);
    
    /**
     * Get no-show bookings for a specific user
     * @param userId The user ID
     * @return List of no-show bookings for the user
     */
    List<Booking> getUserNoShowBookings(Long userId);
}