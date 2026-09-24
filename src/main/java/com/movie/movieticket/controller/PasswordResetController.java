package com.movie.movieticket.controller;

import com.movie.movieticket.model.User;
import com.movie.movieticket.model.PasswordResetToken;
import com.movie.movieticket.service.UserService;
import com.movie.movieticket.service.EmailService;
import com.movie.movieticket.dto.PasswordResetDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public PasswordResetController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(HttpServletRequest request, Model model, @RequestParam("email") String userEmail) {
        User user = userService.findByEmail(userEmail);
        
        if (user == null) {
            model.addAttribute("error", "We couldn't find an account with that email address.");
            return "forgot-password";
        }
        
        if (!user.isEnabled()) {
            model.addAttribute("error", "Your account is not verified. Please verify your account first.");
            return "forgot-password";
        }
        
        // Generate token and send reset email
        String token = userService.createPasswordResetTokenForUser(user);
        String applicationUrl = getApplicationUrl(request);
        emailService.sendPasswordResetEmail(user.getEmail(), token, applicationUrl);
        
        model.addAttribute("success", "Password reset instructions have been sent to your email.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        PasswordResetToken resetToken = userService.getPasswordResetToken(token);
        
        if (resetToken == null) {
            model.addAttribute("message", "Invalid password reset token");
            return "error";
        }
        
        if (resetToken.isExpired()) {
            model.addAttribute("message", "Password reset token has expired");
            return "error";
        }
        
        model.addAttribute("token", token);
        model.addAttribute("passwordResetDto", new PasswordResetDto());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute("passwordResetDto") @Valid PasswordResetDto passwordResetDto,
                                      BindingResult result, @RequestParam("token") String token, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("token", token);
            return "reset-password";
        }
        
        if (!passwordResetDto.getPassword().equals(passwordResetDto.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("token", token);
            return "reset-password";
        }
        
        PasswordResetToken resetToken = userService.getPasswordResetToken(token);
        if (resetToken == null || resetToken.isExpired()) {
            model.addAttribute("message", "Invalid or expired password reset token");
            return "error";
        }
        
        User user = resetToken.getUser();
        userService.changeUserPassword(user, passwordResetDto.getPassword());
        userService.deletePasswordResetToken(resetToken);
        
        return "redirect:/login?resetSuccess";
    }

    private String getApplicationUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
}
