package com.movie.movieticket.controller;

import com.movie.movieticket.dto.UserRegistrationDto;
import com.movie.movieticket.model.User;
import com.movie.movieticket.service.EmailService;
import com.movie.movieticket.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.logging.Logger;

@Controller
public class AuthController {

    private static final Logger logger = Logger.getLogger(AuthController.class.getName());

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String login(Model model,
                       @RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       @RequestParam(value = "expired", required = false) String expired,
                       @RequestParam(value = "accessDenied", required = false) String accessDenied,
                       @RequestParam(value = "unverified", required = false) String unverified,
                       @RequestParam(value = "blocked", required = false) String blocked,
                       @RequestParam(value = "verificationSuccess", required = false) String verificationSuccess,
                       @RequestParam(value = "verificationError", required = false) String verificationError) {
        
        if (error != null) {
            if ("unverified".equals(error)) {
                model.addAttribute("error", "Please verify your email address before logging in.");
            } else if ("blocked".equals(error)) {
                model.addAttribute("error", "Your account has been blocked. Please contact customer service.");
            } else {
                model.addAttribute("error", "Invalid username or password");
            }
        }
        
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        
        if (expired != null) {
            model.addAttribute("error", "Your session has expired. Please log in again.");
        }
        
        if (accessDenied != null) {
            model.addAttribute("error", "Access denied. Please log in to continue.");
        }
        
        if (verificationSuccess != null) {
            model.addAttribute("verificationSuccess", "Your account has been successfully verified. You can now log in.");
        }
        
        if (verificationError != null) {
            model.addAttribute("verificationError", "Invalid or expired verification token.");
        }
        
        return "login";
    }
    
    @GetMapping("/admin-login")
    public String adminLogin(Model model,
                           @RequestParam(value = "error", required = false) String error) {
        
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        
        return "admin-login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("user") @Valid UserRegistrationDto userDto,
                                      BindingResult result,
                                      HttpServletRequest request,
                                      Model model) {
        
        try {
            logger.info("Registration attempt for email: " + userDto.getEmail());
            
            User existing = userService.findByEmail(userDto.getEmail());
            if (existing != null) {
                result.rejectValue("email", null, "There is already an account registered with that email");
            }
            
            if (!userDto.isPrivacyPolicyAccepted()) {
                result.rejectValue("privacyPolicyAccepted", null, "You must accept the privacy policy");
            }
            
            if (result.hasErrors()) {
                return "register";
            }
            
            User user = userService.save(userDto);
            logger.info("User saved successfully: " + user.getEmail());
            
            // Create verification token
            String token = UUID.randomUUID().toString();
            userService.saveVerificationToken(user, token);
            logger.info("Verification token created for user: " + user.getEmail());
            
            // Send verification email
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            emailService.sendVerificationEmail(user.getEmail(), token, appUrl);
            logger.info("Verification email sent to: " + user.getEmail());
            
            model.addAttribute("registrationSuccess", "Registration successful! Please check your email to verify your account.");
            return "registration-confirmation";
            
        } catch (Exception e) {
            logger.severe("Error during registration: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    // Main verification endpoint - this should handle the email verification
    @GetMapping("/verify-account")
    public String verifyAccount(@RequestParam("token") String token, 
                              Model model, 
                              RedirectAttributes redirectAttributes) {
        
        logger.info("Verification attempt with token: " + token);
        
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warning("Empty or null token provided");
                return "verification-failed";
            }
            
            boolean verified = userService.verifyUser(token);
            
            if (verified) {
                logger.info("User verified successfully with token: " + token);
                model.addAttribute("verificationSuccess", "Your account has been successfully verified. You can now log in.");
                return "verification-success";
            } else {
                logger.warning("Verification failed for token: " + token);
                model.addAttribute("verificationError", "Invalid or expired verification token.");
                return "verification-failed";
            }
        } catch (Exception e) {
            logger.severe("Error during verification: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("verificationError", "An error occurred during verification. Please try again or contact support.");
            return "verification-failed";
        }
    }
    
    // Alternative verification endpoint (backup)
    @GetMapping("/verify")
    public String verifyAccountAlternative(@RequestParam("token") String token, 
                                         RedirectAttributes redirectAttributes) {
        
        logger.info("Alternative verification attempt with token: " + token);
        
        try {
            boolean verified = userService.verifyUser(token);
            
            if (verified) {
                redirectAttributes.addAttribute("verificationSuccess", "true");
                return "redirect:/login";
            } else {
                redirectAttributes.addAttribute("verificationError", "true");
                return "redirect:/login";
            }
        } catch (Exception e) {
            logger.severe("Error during alternative verification: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addAttribute("verificationError", "true");
            return "redirect:/login";
        }
    }
    
    // Resend verification email endpoint
    @PostMapping("/resend-verification")
    public String resendVerificationEmail(@RequestParam("email") String email,
                                        HttpServletRequest request,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        
        logger.info("Resend verification request for email: " + email);
        
        try {
            User user = userService.findByEmail(email);
            
            if (user == null) {
                model.addAttribute("error", "No account found with that email address.");
                return "login";
            }
            
            if (user.isEnabled()) {
                model.addAttribute("message", "Your account is already verified.");
                return "login";
            }
            
            // Create new verification token
            String token = UUID.randomUUID().toString();
            userService.saveVerificationToken(user, token);
            
            // Send verification email
            String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            emailService.sendVerificationEmail(user.getEmail(), token, appUrl);
            
            model.addAttribute("message", "Verification email has been resent. Please check your email.");
            return "login";
            
        } catch (Exception e) {
            logger.severe("Error resending verification email: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to resend verification email. Please try again.");
            return "login";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        logger.info("Manual logout requested");
        
        // Clear Spring Security context
        SecurityContextHolder.clearContext();
        
        return "redirect:/login?logout";
    }
}