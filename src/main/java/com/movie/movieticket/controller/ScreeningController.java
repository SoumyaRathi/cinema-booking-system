package com.movie.movieticket.controller;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.service.CinemaService;
import com.movie.movieticket.service.MovieService;
import com.movie.movieticket.service.ScreeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Controller
@RequestMapping("/screenings")
public class ScreeningController {

    private static final Logger logger = Logger.getLogger(ScreeningController.class.getName());

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private CinemaService cinemaService;

    @GetMapping
    public String getAllScreenings(Model model) {
        try {
            logger.info("Loading all screenings");
            
            // Get all screenings (already sorted by ID desc from service)
            List<Screening> screenings = screeningService.getAllScreenings();
            
            // Filter out screenings with null movies
            screenings = screenings.stream()
                .filter(screening -> screening.getMovie() != null)
                .collect(Collectors.toList());
            
            // Get all unique dates from screenings
            List<LocalDate> sortedDates = screenings.stream()
                .map(screening -> screening.getScreeningTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder()) // Sort dates in descending order (newest first)
                .collect(Collectors.toList());
            
            // Group screenings by date using LinkedHashMap to maintain order
            Map<LocalDate, List<Screening>> screeningsByDate = new LinkedHashMap<>();
            
            // Populate the map with screenings for each date
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screenings.stream()
                    .filter(s -> s.getScreeningTime().toLocalDate().equals(date))
                    .collect(Collectors.toList());
                screeningsByDate.put(date, dateScreenings);
            }
            
            // Pre-process data for the template
            // Group screenings by date and movie
            Map<LocalDate, Map<Movie, List<Screening>>> screeningsByDateAndMovie = new LinkedHashMap<>();
            
            // Group screenings by date, movie, and cinema
            Map<LocalDate, Map<Movie, Map<Cinema, List<Screening>>>> screeningsByCinema = new LinkedHashMap<>();
            
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screeningsByDate.get(date);
                
                // Group by movie for this date
                Map<Movie, List<Screening>> byMovie = dateScreenings.stream()
                    .filter(s -> s.getMovie() != null)
                    .collect(Collectors.groupingBy(Screening::getMovie));
                
                screeningsByDateAndMovie.put(date, byMovie);
                
                // Group by movie and cinema for this date
                Map<Movie, Map<Cinema, List<Screening>>> byMovieAndCinema = new HashMap<>();
                
                for (Map.Entry<Movie, List<Screening>> movieEntry : byMovie.entrySet()) {
                    Movie movie = movieEntry.getKey();
                    List<Screening> movieScreenings = movieEntry.getValue();
                    
                    // Group by cinema for this movie
                    Map<Cinema, List<Screening>> byCinema = movieScreenings.stream()
                        .collect(Collectors.groupingBy(Screening::getCinema));
                    
                    byMovieAndCinema.put(movie, byCinema);
                }
                
                screeningsByCinema.put(date, byMovieAndCinema);
            }
            
            // Get today's date for date navigation
            LocalDate today = LocalDate.now();
            
            model.addAttribute("screenings", screenings);
            model.addAttribute("screeningsByDate", screeningsByDate);
            model.addAttribute("screeningsByDateAndMovie", screeningsByDateAndMovie);
            model.addAttribute("screeningsByCinema", screeningsByCinema);
            model.addAttribute("today", today);
            
            return "screenings";
        } catch (Exception e) {
            logger.severe("Error loading screenings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading screenings: " + e.getMessage());
            // Even in error case, set today to avoid template errors
            model.addAttribute("today", LocalDate.now());
            return "error";
        }
    }

    @GetMapping("/movie/{movieId}")
    public String getScreeningsByMovie(@PathVariable Long movieId, Model model) {
        try {
            logger.info("Loading screenings for movie ID: " + movieId);
            
            // Get the movie
            Movie movie = movieService.getMovieById(movieId);
            if (movie == null) {
                logger.warning("Movie not found with ID: " + movieId);
                model.addAttribute("error", "Movie not found");
                // Set today even in error case
                model.addAttribute("today", LocalDate.now());
                return "error";
            }
            
            // Get screenings for this movie (already sorted by ID desc from service)
            List<Screening> screenings = screeningService.getScreeningsByMovieId(movieId);
            
            // Get all unique dates from screenings
            List<LocalDate> sortedDates = screenings.stream()
                .map(screening -> screening.getScreeningTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder()) // Sort dates in descending order (newest first)
                .collect(Collectors.toList());
            
            // Group screenings by date using LinkedHashMap to maintain order
            Map<LocalDate, List<Screening>> screeningsByDate = new LinkedHashMap<>();
            
            // Populate the map with screenings for each date
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screenings.stream()
                    .filter(s -> s.getScreeningTime().toLocalDate().equals(date))
                    .collect(Collectors.toList());
                screeningsByDate.put(date, dateScreenings);
            }
            
            // Group screenings by date and cinema
            Map<LocalDate, Map<Cinema, List<Screening>>> screeningsByCinema = new LinkedHashMap<>();
            
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screeningsByDate.get(date);
                
                // Group by cinema for this date
                Map<Cinema, List<Screening>> byCinema = dateScreenings.stream()
                    .collect(Collectors.groupingBy(Screening::getCinema));
                
                screeningsByCinema.put(date, byCinema);
            }
            
            // Get today's date for date navigation
            LocalDate today = LocalDate.now();
            
            model.addAttribute("movie", movie);
            model.addAttribute("screenings", screenings);
            model.addAttribute("screeningsByDate", screeningsByDate);
            model.addAttribute("screeningsByCinema", screeningsByCinema);
            model.addAttribute("today", today);
            
            return "movie-screenings";
        } catch (Exception e) {
            logger.severe("Error loading screenings for movie: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading screenings: " + e.getMessage());
            // Even in error case, set today to avoid template errors
            model.addAttribute("today", LocalDate.now());
            return "error";
        }
    }

    @GetMapping("/cinema/{cinemaId}")
    public String getScreeningsByCinema(@PathVariable Long cinemaId, Model model) {
        try {
            logger.info("Loading screenings for cinema ID: " + cinemaId);
            
            // Get the cinema
            Cinema cinema = cinemaService.getCinemaById(cinemaId);
            if (cinema == null) {
                logger.warning("Cinema not found with ID: " + cinemaId);
                model.addAttribute("error", "Cinema not found");
                // Set today even in error case
                model.addAttribute("today", LocalDate.now());
                return "error";
            }
            
            // Get screenings for this cinema (already sorted by ID desc from service)
            List<Screening> screenings = screeningService.getScreeningsByCinemaId(cinemaId);
            
            // Filter out screenings with null movies
            screenings = screenings.stream()
                .filter(screening -> screening.getMovie() != null)
                .collect(Collectors.toList());
            
            // Get all unique dates from screenings
            List<LocalDate> sortedDates = screenings.stream()
                .map(screening -> screening.getScreeningTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder()) // Sort dates in descending order (newest first)
                .collect(Collectors.toList());
            
            // Group screenings by date using LinkedHashMap to maintain order
            Map<LocalDate, List<Screening>> screeningsByDate = new LinkedHashMap<>();
            
            // Populate the map with screenings for each date
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screenings.stream()
                    .filter(s -> s.getScreeningTime().toLocalDate().equals(date))
                    .collect(Collectors.toList());
                screeningsByDate.put(date, dateScreenings);
            }
            
            // Group screenings by date and movie
            Map<LocalDate, Map<Movie, List<Screening>>> screeningsByDateAndMovie = new LinkedHashMap<>();
            
            for (LocalDate date : sortedDates) {
                List<Screening> dateScreenings = screeningsByDate.get(date);
                
                // Group by movie for this date
                Map<Movie, List<Screening>> byMovie = dateScreenings.stream()
                    .filter(s -> s.getMovie() != null)
                    .collect(Collectors.groupingBy(Screening::getMovie));
                
                screeningsByDateAndMovie.put(date, byMovie);
            }
            
            // Get today's date for date navigation
            LocalDate today = LocalDate.now();
            
            model.addAttribute("cinema", cinema);
            model.addAttribute("screenings", screenings);
            model.addAttribute("screeningsByDate", screeningsByDate);
            model.addAttribute("screeningsByDateAndMovie", screeningsByDateAndMovie);
            model.addAttribute("today", today);
            
            return "cinema-screenings";
        } catch (Exception e) {
            logger.severe("Error loading screenings for cinema: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading screenings: " + e.getMessage());
            // Even in error case, set today to avoid template errors
            model.addAttribute("today", LocalDate.now());
            return "error";
        }
    }

    @GetMapping("/date/{date}")
    public String getScreeningsByDate(@PathVariable String date, Model model) {
        try {
            logger.info("Loading screenings for date: " + date);
            
            // Parse the date
            LocalDate screeningDate = LocalDate.parse(date);
            
            // Get screenings for this date
            LocalDateTime startOfDay = screeningDate.atStartOfDay();
            LocalDateTime endOfDay = screeningDate.plusDays(1).atStartOfDay().minusSeconds(1);
            
            // Use the method that sorts by ID desc
            List<Screening> screenings = screeningService.getScreeningsBetweenDates(startOfDay, endOfDay);
            
            // Filter out screenings with null movies
            screenings = screenings.stream()
                .filter(screening -> screening.getMovie() != null)
                .collect(Collectors.toList());
            
            // Group screenings by movie
            Map<Movie, List<Screening>> screeningsByMovie = screenings.stream()
                .filter(s -> s.getMovie() != null)
                .collect(Collectors.groupingBy(Screening::getMovie));
            
            // Group screenings by movie and cinema
            Map<Movie, Map<Cinema, List<Screening>>> screeningsByCinema = new HashMap<>();
            
            for (Map.Entry<Movie, List<Screening>> entry : screeningsByMovie.entrySet()) {
                Movie movie = entry.getKey();
                List<Screening> movieScreenings = entry.getValue();
                
                // Group by cinema for this movie
                Map<Cinema, List<Screening>> byCinema = movieScreenings.stream()
                    .collect(Collectors.groupingBy(Screening::getCinema));
                
                screeningsByCinema.put(movie, byCinema);
            }
            
            model.addAttribute("screeningDate", screeningDate);
            model.addAttribute("screenings", screenings);
            model.addAttribute("screeningsByMovie", screeningsByMovie);
            model.addAttribute("screeningsByCinema", screeningsByCinema);
            model.addAttribute("today", LocalDate.now()); // Always set today
            
            return "screenings-by-date";
        } catch (Exception e) {
            logger.severe("Error loading screenings for date: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading screenings: " + e.getMessage());
            // Even in error case, set today to avoid template errors
            model.addAttribute("today", LocalDate.now());
            return "error";
        }
    }
}