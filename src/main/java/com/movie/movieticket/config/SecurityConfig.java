package com.movie.movieticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Log logger = LogFactory.getLog(SecurityConfig.class);

    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(UserDetailsService userDetailsService, 
                         BCryptPasswordEncoder passwordEncoder,
                         CustomAuthenticationSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.successHandler = successHandler;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                              org.springframework.security.access.AccessDeniedException accessDeniedException) 
                              throws IOException, ServletException {
                
                String path = request.getRequestURI();
                logger.warn("Access denied to path: " + path);
                
                // Redirect to login instead of dashboard to avoid loops
                response.sendRedirect("/login?accessDenied=true");
            }
        };
    }
    
    @Bean
    public LogoutHandler logoutHandler(SessionRegistry sessionRegistry) {
        return new SecurityContextLogoutHandler() {
            @Override
            public void logout(HttpServletRequest request, HttpServletResponse response, 
                              org.springframework.security.core.Authentication authentication) {
                
                logger.info("Custom logout handler invoked");
                
                String sessionId = request.getSession(false) != null ? 
                                  request.getSession(false).getId() : null;
                
                super.logout(request, response, authentication);
                
                if (sessionId != null) {
                    try {
                        logger.info("Removing session from registry: " + sessionId);
                        sessionRegistry.removeSessionInformation(sessionId);
                    } catch (Exception e) {
                        logger.error("Error removing session from registry: " + e.getMessage(), e);
                    }
                }
            }
        };
    }
    
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
        handler.setDefaultTargetUrl("/login?logout");
        return handler;
    }
    
    @Bean
    public AuthenticationFailureHandler userAuthenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
        handler.setDefaultFailureUrl("/login?error=true");
        return handler;
    }
    
    @Bean
    public AuthenticationFailureHandler adminAuthenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
        handler.setDefaultFailureUrl("/admin-login?error=true");
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain");
        
        http
            .authorizeHttpRequests(auth -> auth
                // Public URLs - accessible to everyone
                .requestMatchers("/", "/index", "/home", "/movies", "/movies/**", 
                    "/screenings", "/screenings/**", "/register/**", "/css/**", "/js/**", 
                    "/images/**", "/uploads/**", "/poster-images/**", "/verify/**", 
                    "/verify-account/**", "/verify-account", "/session/info",
                    "/forgot-password/**", "/reset-password/**", "/login", "/admin-login", 
                    "/error", "/about", "/contact", "/cinemas/**", "/debug/**",
                    "/favicon.ico", "/robots.txt").permitAll()
                
                // Admin URLs - require ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // User booking URLs - require authentication (any authenticated user)
                .requestMatchers("/bookings/**", "/my-bookings/**", "/dashboard/**", 
                    "/profile/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(userAuthenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .addLogoutHandler(logoutHandler(sessionRegistry()))
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "MOVIETICKET-SESSION")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
                .sessionRegistry(sessionRegistry())
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
            )
            .csrf(csrf -> csrf.disable()); // Disable CSRF for simplicity - enable in production
        
        logger.info("Security filter chain configured successfully");
        return http.build();
    }
}