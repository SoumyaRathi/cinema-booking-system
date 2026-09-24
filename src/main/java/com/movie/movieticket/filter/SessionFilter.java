package com.movie.movieticket.filter;

import com.movie.movieticket.model.UserSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Component
@Order(1)
public class SessionFilter extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(SessionFilter.class.getName());
    private static final String USER_SESSION_KEY = "userSession";
    
    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/login", "/admin-login", "/register", "/", "/index", "/movies", 
            "/css/", "/js/", "/images/", "/error", "/forgot-password",
            "/reset-password", "/verify", "/verify-account", "/verification-success", 
            "/verification-failed", "/uploads/", "/login-process"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        logger.fine("SessionFilter processing request for path: " + path);
        
        // Allow public paths without session check
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        HttpSession session = request.getSession(false);
        
        // No session exists, redirect to login
        if (session == null) {
            logger.info("No session found, redirecting to login: " + path);
            response.sendRedirect("/login");
            return;
        }
        
        UserSession userSession = (UserSession) session.getAttribute(USER_SESSION_KEY);
        
        // No user in session, redirect to login
        if (userSession == null) {
            logger.info("No user session found, redirecting to login: " + path);
            response.sendRedirect("/login");
            return;
        }
        
        // Update last access time
        userSession.updateLastAccess();
        
        // Continue with the request
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}