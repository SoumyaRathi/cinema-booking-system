package com.movie.movieticket.controller;

import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.User;
import com.movie.movieticket.model.UserSession;
import com.movie.movieticket.service.BookingService;
import com.movie.movieticket.service.MovieService;
import com.movie.movieticket.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.logging.Logger;

@Controller
public class UserController {
    
    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    private static final String USER_SESSION_KEY = "userSession";

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/movie-details/{id}")
    public String movieDetails(@PathVariable Long id, Model model) {
        try {
            logger.info("Loading movie details for ID: " + id);
            
            Movie movie = movieService.getMovieById(id);
            if (movie == null) {
                logger.warning("Movie not found with ID: " + id);
                model.addAttribute("error", "Movie not found");
                return "error";
            }
            
            List<Screening> screenings = movieService.getScreeningsByMovieId(id);
            
            model.addAttribute("movie", movie);
            model.addAttribute("screenings", screenings);
            
            return "movie-details";
        } catch (Exception e) {
            logger.severe("Error loading movie details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading movie details: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/user-dashboard")
    public String userDashboard(Model model, HttpServletRequest request) {
        try {
            logger.info("Loading user dashboard");
            
            // Get user from session
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.warning("No session found, redirecting to login");
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                logger.warning("No user session found, redirecting to login");
                return "redirect:/login";
            }
            
            // Update last access time
            userSession.updateLastAccess();
            
            // Get user from database
            User user = userService.findByEmail(userSession.getEmail());
            
            if (user == null) {
                logger.warning("User not found in database: " + userSession.getEmail());
                session.invalidate();
                return "redirect:/login";
            }
            
            // Check if user is verified
            if (!user.isEnabled()) {
                logger.warning("User not verified: " + user.getEmail());
                return "redirect:/login?error=unverified";
            }
            
            if (userSession.isAdmin()) {
                return "redirect:/admin/dashboard";
            }
            
            // Get user bookings
            List<Booking> userBookings = bookingService.getBookingsByUser(user);
            
            // Get upcoming movies
            List<Movie> upcomingMovies = movieService.getUpcomingMovies();
            
            // Add attributes to model
            model.addAttribute("user", user);
            model.addAttribute("userSession", userSession);
            model.addAttribute("bookingCount", userBookings.size());
            model.addAttribute("upcomingMovieCount", upcomingMovies.size());
            model.addAttribute("theaterCount", 3); // Hardcoded for now
            model.addAttribute("recentBookings", userBookings);
            
            return "dashboard";
        } catch (Exception e) {
            logger.severe("Error loading user dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpServletRequest request) {
        try {
            logger.info("Loading user profile");
            
            // Get user from session
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                return "redirect:/login";
            }
            
            // Update last access time
            userSession.updateLastAccess();
            
            // Get user from database
            User user = userService.findByEmail(userSession.getEmail());
            
            if (user == null) {
                logger.warning("User not found in database: " + userSession.getEmail());
                session.invalidate();
                return "redirect:/login";
            }
            
            // Check if user is verified
            if (!user.isEnabled()) {
                return "redirect:/login?error=unverified";
            }
            
            model.addAttribute("user", user);
            model.addAttribute("userSession", userSession);
            return "profile";
        } catch (Exception e) {
            logger.severe("Error loading profile: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading profile: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("phone") String phone,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            logger.info("Updating user profile");
            
            // Get user from session
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                return "redirect:/login";
            }
            
            // Get user from database
            User user = userService.findByEmail(userSession.getEmail());
            
            if (user == null) {
                logger.warning("User not found in database: " + userSession.getEmail());
                session.invalidate();
                return "redirect:/login";
            }

            // Validate input
            if (firstName == null || firstName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "First name is required");
                return "redirect:/profile";
            }
            
            if (lastName == null || lastName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Last name is required");
                return "redirect:/profile";
            }

            // Update user details
            user.setFirstName(firstName.trim());
            user.setLastName(lastName.trim());
            user.setPhone(phone != null ? phone.trim() : "");
            
            // Save updated user
            userService.updateUser(user);
            
            // Update session data
            userSession.setFirstName(firstName.trim());
            userSession.setLastName(lastName.trim());
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            logger.severe("Error updating profile: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/profile";
        }
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model, HttpServletRequest request) {
        try {
            logger.info("Loading user bookings");
            
            // Get user from session
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                return "redirect:/login";
            }
            
            // Update last access time
            userSession.updateLastAccess();
            
            // Get user from database
            User user = userService.findByEmail(userSession.getEmail());
            
            if (user == null) {
                logger.warning("User not found in database: " + userSession.getEmail());
                session.invalidate();
                return "redirect:/login";
            }
            
            // Check if user is verified
            if (!user.isEnabled()) {
                return "redirect:/login?error=unverified";
            }
            
            List<Booking> bookings = bookingService.getBookingsByUser(user);
            model.addAttribute("bookings", bookings);
            model.addAttribute("userSession", userSession);
            return "bookings";
        } catch (Exception e) {
            logger.severe("Error loading bookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            logger.info("Changing user password");
            
            // Get user from session
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                return "redirect:/login";
            }
            
            // Get user from database
            User user = userService.findByEmail(userSession.getEmail());
            
            if (user == null) {
                logger.warning("User not found in database: " + userSession.getEmail());
                session.invalidate();
                return "redirect:/login";
            }

            // Validate input
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Current password is required");
                return "redirect:/profile";
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "New password is required");
                return "redirect:/profile";
            }
            
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long");
                return "redirect:/profile";
            }

            // Validate passwords match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New password and confirmation do not match");
                return "redirect:/profile";
            }

            // Check current password
            if (!userService.checkIfValidOldPassword(user, currentPassword)) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/profile";
            }

            // Update password
            userService.changeUserPassword(user, newPassword);
            
            redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
            return "redirect:/profile";
        } catch (Exception e) {
            logger.severe("Error changing password: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error changing password: " + e.getMessage());
            return "redirect:/profile";
        }
    }
    
    // Additional method to handle account verification status
    @GetMapping("/account-status")
    public String accountStatus(Model model, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "redirect:/login";
            }
            
            UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
            if (userSession == null) {
                return "redirect:/login";
            }
            
            User user = userService.findByEmail(userSession.getEmail());
            if (user == null) {
                session.invalidate();
                return "redirect:/login";
            }
            
            model.addAttribute("user", user);
            model.addAttribute("userSession", userSession);
            
            if (!user.isEnabled()) {
                model.addAttribute("needsVerification", true);
            }
            
            return "account-status";
        } catch (Exception e) {
            logger.severe("Error checking account status: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error checking account status: " + e.getMessage());
            return "error";
        }
    }
}