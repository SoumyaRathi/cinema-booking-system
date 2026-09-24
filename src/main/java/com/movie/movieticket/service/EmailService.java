package com.movie.movieticket.service;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Payment;
import com.movie.movieticket.model.User;

import jakarta.mail.MessagingException;

public interface EmailService {
    
    void sendEmail(String to, String subject, String body);
    
    void sendVerificationEmail(String to, String token, String applicationUrl);
    
    void sendVerificationEmail(User user, String applicationUrl, String token);
    
    void sendBookingConfirmation(String to, String bookingDetails, String confirmationCode);
    
    void sendBookingConfirmationWithReceipt(Booking booking, Payment payment);
    
    void sendPasswordResetEmail(String to, String token, String applicationUrl);
    
    void sendPasswordResetEmail(User user, String applicationUrl, String token);
    
    // No-show warning methods
    void sendNoShowWarning(User user, Booking booking);
    
    void sendNoShowWarningEmail(User user, int noShowCount);
    
    void sendNoShowBlockedEmail(User user);
    
    // New method for booking cancellation
    void sendBookingCancellationEmail(Booking booking);
    
    // FIXED: Removed throws MessagingException
    void sendPaymentReminderEmail(Booking booking);
    
    void sendBookingVoidedEmail(Booking booking);
}