package com.movie.movieticket.controller;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.service.MovieService;
import com.movie.movieticket.service.ScreeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/movies")
public class MovieController {

    private static final Logger logger = Logger.getLogger(MovieController.class.getName());

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private ServletContext servletContext;

    @GetMapping("/{id}")
    public String viewMovie(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            logger.info("Loading movie details for ID: " + id);
            Movie movie = movieService.getMovieById(id);
            if (movie == null) {
                logger.warning("Movie not found with ID: " + id);
                model.addAttribute("error", "Movie not found");
                return "error";
            }
            
            // Log image information for debugging
            logger.info("Movie Details - Movie: " + movie.getTitle() + 
                       ", Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()) +
                       ", Image URL: " + movie.getImageUrl() +
                       ", Effective URL: " + movie.getEffectiveImageUrl());
            
            List<Screening> availableScreenings = screeningService.getScreeningsByMovie(movie);
            
            model.addAttribute("movie", movie);
            model.addAttribute("screenings", availableScreenings);
            return "movie-details";
        } catch (Exception e) {
            logger.severe("Error loading movie details: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading movie details: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/book/{id}")
    public String bookMovie(@PathVariable Long id, @RequestParam(required = false) String date, 
                           Model model, HttpServletRequest request) {
        try {
            logger.info("Loading booking page for movie ID: " + id);
            Movie movie = movieService.getMovieById(id);
            if (movie == null) {
                logger.warning("Movie not found with ID: " + id);
                model.addAttribute("error", "Movie not found");
                return "error";
            }
            
            // Log image information for debugging
            logger.info("Movie Booking - Movie: " + movie.getTitle() + 
                       ", Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()) +
                       ", Image URL: " + movie.getImageUrl() +
                       ", Effective URL: " + movie.getEffectiveImageUrl());
            
            List<Screening> availableScreenings = screeningService.getScreeningsByMovie(movie);
            
            model.addAttribute("movie", movie);
            model.addAttribute("screenings", availableScreenings);
            
            LocalDate today = LocalDate.now();
            model.addAttribute("today", today);
            
            List<LocalDate> dateRange = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                dateRange.add(today.plusDays(i));
            }
            model.addAttribute("dateRange", dateRange);
            
            LocalDate selectedDate = (date != null) ? LocalDate.parse(date) : today;
            model.addAttribute("selectedDate", selectedDate);
            
            Map<LocalDate, List<Screening>> screeningsByDate = availableScreenings.stream()
                    .collect(Collectors.groupingBy(s -> s.getScreeningTime().toLocalDate()));
            model.addAttribute("screeningsByDate", screeningsByDate);
            
            Map<LocalDate, Map<Cinema, List<Screening>>> screeningsByCinema = new HashMap<>();
            
            for (LocalDate dateKey : screeningsByDate.keySet()) {
                Map<Cinema, List<Screening>> cinemaMap = screeningsByDate.get(dateKey).stream()
                        .collect(Collectors.groupingBy(Screening::getCinema));
                screeningsByCinema.put(dateKey, cinemaMap);
            }
            model.addAttribute("screeningsByCinema", screeningsByCinema);
            
            Map<LocalDate, Map<Movie, List<Screening>>> screeningsByDateAndMovie = new HashMap<>();
            
            for (LocalDate dateKey : screeningsByDate.keySet()) {
                Map<Movie, List<Screening>> movieMap = screeningsByDate.get(dateKey).stream()
                        .collect(Collectors.groupingBy(s -> s.getMovie()));
                screeningsByDateAndMovie.put(dateKey, movieMap);
            }
            model.addAttribute("screeningsByDateAndMovie", screeningsByDateAndMovie);
            
            return "movie-book";
        } catch (Exception e) {
            logger.severe("Error loading booking page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading booking page: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping
    public String listMovies(Model model, @RequestParam(required = false) String genre, HttpServletRequest request) {
        try {
            logger.info("Loading movies list with genre filter: " + genre);
            List<Movie> movies;
            
            if (genre != null && !genre.isEmpty() && !genre.equals("All")) {
                movies = movieService.getReleasedMoviesByGenre(genre);
            } else {
                movies = movieService.getReleasedMovies();
            }
            
            // Log image information for debugging
            for (Movie movie : movies) {
                logger.info("Movies List - Movie: " + movie.getTitle() + 
                           ", Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()) +
                           ", Image URL: " + movie.getImageUrl() +
                           ", Effective URL: " + movie.getEffectiveImageUrl());
            }
            
            Set<String> genres = movieService.getAllGenres();
            
            model.addAttribute("movies", movies);
            model.addAttribute("genres", genres);
            model.addAttribute("selectedGenre", genre != null ? genre : "All");
            
            return "movies";
        } catch (Exception e) {
            logger.severe("Error loading movies: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error loading movies: " + e.getMessage());
            return "error";
        }
    }
}