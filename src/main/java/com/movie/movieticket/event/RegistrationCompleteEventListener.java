package com.movie.movieticket.event;

import com.movie.movieticket.model.User;
import com.movie.movieticket.service.EmailService;
import com.movie.movieticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RegistrationCompleteEventListener implements ApplicationListener<RegistrationCompleteEvent> {

    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public RegistrationCompleteEventListener(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    @Override
    public void onApplicationEvent(RegistrationCompleteEvent event) {
        // Create verification token for the user
        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        userService.saveVerificationToken(user, token);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(user.getEmail(), token, event.getApplicationUrl());
        } catch (Exception e) {
            // Log the error but don't prevent user registration
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
    }
}
