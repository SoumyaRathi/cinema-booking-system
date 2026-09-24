package com.movie.movieticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean isAdmin;
    private LocalDateTime lastAccess;
    private String sessionId;
    
    // Constructor
    public UserSession() {
        this.lastAccess = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public LocalDateTime getLastAccess() {
        return lastAccess;
    }
    
    public void updateLastAccess() {
        this.lastAccess = LocalDateTime.now();
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}