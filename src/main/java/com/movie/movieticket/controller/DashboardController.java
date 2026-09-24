package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.BookingService;
import com.movie.movieticket.service.MovieService;
import com.movie.movieticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.logging.Logger;

@Controller
public class DashboardController {

    private static final Logger logger = Logger.getLogger(DashboardController.class.getName());

    @Autowired
    private MovieService movieService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            logger.info("Loading dashboard");
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
                String currentUsername = authentication.getName();
                User currentUser = userService.findByEmail(currentUsername);
                
                // Check if user is admin
                boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
                if (isAdmin) {
                    // Redirect admin to admin dashboard
                    return "redirect:/admin/dashboard";
                }
                
                if (currentUser != null) {
                    model.addAttribute("user", currentUser);
                    
                    // Add no-show information to the model
                    if (currentUser.getNoShowCount() != null && currentUser.getNoShowCount() > 0) {
                        model.addAttribute("noShowCount", currentUser.getNoShowCount());
                        
                        // Add warning message based on no-show count
                        if (currentUser.getNoShowCount() == 1) {
                            model.addAttribute("noShowWarning", "You have 1 no-show on your record. Please be aware that multiple no-shows may result in temporary or permanent booking restrictions.");
                            model.addAttribute("noShowWarningLevel", "warning");
                        } else if (currentUser.getNoShowCount() == 2) {
                            model.addAttribute("noShowWarning", "You have 2 no-shows on your record. Your account has been temporarily restricted for 7 days. Further no-shows will result in permanent restrictions.");
                            model.addAttribute("noShowWarningLevel", "danger");
                        } else if (currentUser.getNoShowCount() >= 3) {
                            model.addAttribute("noShowWarning", "Your account has been restricted due to multiple no-shows. Please contact customer service for assistance.");
                            model.addAttribute("noShowWarningLevel", "danger");
                        }
                        
                        // Add blocked status information
                        if (currentUser.getIsBlocked() != null && currentUser.getIsBlocked()) {
                            model.addAttribute("userBlocked", true);
                            model.addAttribute("blockReason", currentUser.getBlockReason());
                        } else if (currentUser.getBlockedUntil() != null) {
                            model.addAttribute("userTempBlocked", true);
                            model.addAttribute("blockedUntil", currentUser.getBlockedUntil());
                        }
                    }
                    
                    // Get user's recent bookings
                    List<Booking> recentBookings = bookingService.getBookingsByUser(currentUser);
                    if (recentBookings != null && !recentBookings.isEmpty()) {
                        if (recentBookings.size() > 3) {
                            recentBookings = recentBookings.subList(0, 3); // Limit to 3 recent bookings
                        }
                        model.addAttribute("recentBookings", recentBookings);
                    }
                }
            }
            
            // Get featured movies (limit to 6) - UPDATED with image debugging
            List<Movie> movies = movieService.getReleasedMovies(); // Use released movies instead of all movies
            if (movies != null && !movies.isEmpty()) {
                if (movies.size() > 6) {
                    movies = movies.subList(0, 6);
                }
                
                // NEW: Log image information for debugging
                logger.info("Dashboard - Number of featured movies: " + movies.size());
                for (Movie movie : movies) {
                    logger.info("Dashboard Featured Movie: " + movie.getTitle() + 
                               ", Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()) +
                               ", Image URL: " + movie.getImageUrl() +
                               ", Effective URL: " + movie.getEffectiveImageUrl());
                }
                
                model.addAttribute("movies", movies);
            } else {
                logger.warning("No movies found for dashboard");
                model.addAttribute("movies", List.of()); // Empty list to avoid null pointer
            }
            
            // Return the existing dashboard template
            return "dashboard";
        } catch (Exception e) {
            logger.severe("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }
}