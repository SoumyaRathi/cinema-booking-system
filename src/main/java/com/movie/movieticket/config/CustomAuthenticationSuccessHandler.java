package com.movie.movieticket.config;

import com.movie.movieticket.model.User;
import com.movie.movieticket.model.UserSession;
import com.movie.movieticket.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Logger;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = Logger.getLogger(CustomAuthenticationSuccessHandler.class.getName());
    private static final String USER_SESSION_KEY = "userSession";
    
    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        String email = authentication.getName();
        
        logger.info("Authentication successful for user: " + email + ". User has admin role: " + isAdmin);
        
        // FIXED: Check login page based on referer or request parameters
        String referer = request.getHeader("Referer");
        boolean isAdminLogin = (referer != null && referer.contains("admin-login")) || 
                              request.getRequestURI().contains("admin-login") ||
                              request.getServletPath().contains("admin-login");
        
        logger.info("Login referer: " + referer + ", isAdminLogin: " + isAdminLogin);
        
        // Create or get session
        HttpSession session = request.getSession(true);
        
        // Get user from database
        User user = userService.findByEmail(email);
        
        if (user != null) {
            // FIXED: Check if user account is verified
            if (!user.isEnabled()) {
                logger.warning("User tried to login with unverified account: " + email);
                session.invalidate();
                response.sendRedirect("/login?error=unverified");
                return;
            }
            
            // FIXED: Check if user is blocked
            if (user.getIsBlocked() != null && user.getIsBlocked()) {
                logger.warning("Blocked user tried to login: " + email);
                session.invalidate();
                response.sendRedirect("/login?error=blocked");
                return;
            }
            
            // Create and store user session data
            UserSession userSession = new UserSession();
            userSession.setUserId(user.getId());
            userSession.setUsername(user.getEmail());
            userSession.setEmail(user.getEmail());
            userSession.setFirstName(user.getFirstName());
            userSession.setLastName(user.getLastName());
            userSession.setAdmin(isAdmin);
            userSession.setSessionId(session.getId());
            
            // Store in session
            session.setAttribute(USER_SESSION_KEY, userSession);
            
            logger.info("User session created for: " + email);
        }
        
        // FIXED: Simplified role-based redirection logic
        if (isAdmin) {
            logger.info("Redirecting admin to /admin/dashboard");
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else {
            logger.info("Redirecting user to /dashboard");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}