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
public class HomeController {

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @Autowired
    private MovieService movieService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        try {
            logger.info("Loading home page");
            
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
                    
                    // Get user's recent bookings
                    List<Booking> recentBookings = bookingService.getBookingsByUser(currentUser);
                    if (recentBookings.size() > 3) {
                        recentBookings = recentBookings.subList(0, 3); // Limit to 3 recent bookings
                    }
                    model.addAttribute("recentBookings", recentBookings);
                    
                    // For authenticated users, show the dashboard
                    return "dashboard-user";
                }
            }
            
            // For anonymous users, show the landing page
            // Get all movies (both released and upcoming)
            List<Movie> allMovies = movieService.getAllMovies();
            
            // Fix image URLs for all movies
            for (Movie movie : allMovies) {
                fixImageUrl(movie);
                logger.info("Home page - Movie: " + movie.getTitle() + ", Fixed Image URL: " + movie.getImageUrl());
            }
            
            model.addAttribute("movies", allMovies);
            
            return "index"; // Use the new landing page for anonymous users
        } catch (Exception e) {
            logger.severe("Error loading home page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading home page: " + e.getMessage());
            return "error";
        }
    }
    
    // Helper method to fix image URLs
    private void fixImageUrl(Movie movie) {
        String originalUrl = movie.getImageUrl();
        
        if (originalUrl != null && !originalUrl.isEmpty()) {
            // If the URL is just a filename, add the full path
            if (!originalUrl.contains("/")) {
                movie.setImageUrl("/uploads/posters/" + originalUrl);
                logger.info("Fixed image URL (filename only): " + originalUrl + " -> " + movie.getImageUrl());
            }
            // If the URL has /posters/ but not /uploads/, add /uploads
            else if (originalUrl.contains("/posters/") && !originalUrl.contains("/uploads/")) {
                movie.setImageUrl("/uploads" + originalUrl);
                logger.info("Fixed image URL (missing uploads): " + originalUrl + " -> " + movie.getImageUrl());
            }
            // If the URL doesn't start with /, add it
            else if (!originalUrl.startsWith("/")) {
                movie.setImageUrl("/" + originalUrl);
                logger.info("Fixed image URL (missing leading slash): " + originalUrl + " -> " + movie.getImageUrl());
            }
        }
    }
}