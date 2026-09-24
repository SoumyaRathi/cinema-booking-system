package com.movie.movieticket.controller;

import com.movie.movieticket.dto.SeatDTO;
import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.Seat;
import com.movie.movieticket.service.CinemaService;
import com.movie.movieticket.service.MovieService;
import com.movie.movieticket.service.ScreeningService;
import com.movie.movieticket.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin/screenings")
public class AdminScreeningController {

    private static final Logger logger = Logger.getLogger(AdminScreeningController.class.getName());

    @Autowired
    private ScreeningService screeningService;
    
    @Autowired
    private MovieService movieService;
    
    @Autowired
    private CinemaService cinemaService;
    
    @Autowired
    private SeatService seatService;

    @GetMapping
    public String listScreenings(Model model) {
        List<Screening> screenings = screeningService.getAllScreenings();
        List<Movie> movies = movieService.getAllMovies();
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        
        // Update screening seat counts with actual data
        for (Screening screening : screenings) {
            try {
                List<Seat> seats = seatService.getSeatsByScreeningId(screening.getId());
                if (!seats.isEmpty()) {
                    int totalSeats = seats.size();
                    int bookedSeats = (int) seats.stream().filter(Seat::isBooked).count();
                    int availableSeats = totalSeats - bookedSeats;
                    
                    // Update if different
                    if (screening.getTotalSeats() != totalSeats || screening.getAvailableSeats() != availableSeats) {
                        screening.setTotalSeats(totalSeats);
                        screening.setAvailableSeats(availableSeats);
                        screeningService.saveScreening(screening);
                    }
                }
            } catch (Exception e) {
                logger.warning("Error updating seat counts for screening " + screening.getId() + ": " + e.getMessage());
            }
        }
        
        model.addAttribute("screenings", screenings);
        model.addAttribute("movies", movies);
        model.addAttribute("cinemas", cinemas);
        return "screenings-admin";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("screening", new Screening());
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "add-screeningadmin";
    }

    @PostMapping("/add")
    public String addScreening(@RequestParam("movieId") Long movieId,
                             @RequestParam("cinemaId") Long cinemaId,
                             @RequestParam("screeningDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate screeningDate,
                             @RequestParam("screeningTimes") List<String> screeningTimes,
                             @RequestParam("price") Double price,
                             @RequestParam("totalSeats") Integer totalSeats,
                             RedirectAttributes redirectAttributes) {
        try {
            logger.info("Adding new screening(s) - Movie ID: " + movieId + ", Cinema ID: " + cinemaId + 
                       ", Date: " + screeningDate + ", Times: " + screeningTimes);
            
            // Validate inputs
            if (movieId == null || cinemaId == null || screeningDate == null || 
                screeningTimes == null || screeningTimes.isEmpty() || price == null || totalSeats == null) {
                throw new IllegalArgumentException("Missing required fields");
            }
            
            // Get movie and cinema
            Movie movie = movieService.getMovieById(movieId);
            Cinema cinema = cinemaService.getCinemaById(cinemaId);
            
            if (movie == null) {
                throw new IllegalArgumentException("Movie not found with ID: " + movieId);
            }
            
            if (cinema == null) {
                throw new IllegalArgumentException("Cinema not found with ID: " + cinemaId);
            }
            
            // Create and save a screening for each selected time
            int successCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (String timeStr : screeningTimes) {
                try {
                    // Parse time string to LocalTime
                    LocalTime time = LocalTime.parse(timeStr);
                    
                    // Create screening datetime
                    LocalDateTime screeningDateTime = LocalDateTime.of(screeningDate, time);
                    
                    // Check if the screening time is in the past
                    if (screeningDateTime.isBefore(LocalDateTime.now())) {
                        errors.add("Cannot schedule screening at " + timeStr + " as it is in the past");
                        continue;
                    }
                    
                    // Check for time conflicts
                    if (screeningService.hasTimeConflict(cinemaId, screeningDateTime)) {
                        errors.add("Time conflict at " + timeStr + " - another screening is already scheduled");
                        continue;
                    }
                    
                    // Create new screening
                    Screening screening = new Screening();
                    screening.setMovie(movie);
                    screening.setCinema(cinema); // This will now set theater to cinema name
                    screening.setScreeningTime(screeningDateTime);
                    screening.setPrice(price);
                    screening.setTotalSeats(totalSeats);
                    screening.setAvailableSeats(totalSeats);
                    
                    // Save screening
                    Screening savedScreening = screeningService.saveScreening(screening);
                    
                    // Initialize seats for the new screening
                    seatService.initializeSeatsForScreening(savedScreening);
                    
                    successCount++;
                    logger.info("Successfully added screening at " + timeStr);
                } catch (Exception e) {
                    logger.severe("Error adding screening at " + timeStr + ": " + e.getMessage());
                    errors.add("Error adding screening at " + timeStr + ": " + e.getMessage());
                }
            }
            
            // Set appropriate flash messages
            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("success", 
                    "Successfully added " + successCount + " screening(s) for " + movie.getTitle());
            }
            
            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Some screenings could not be added: " + String.join("; ", errors));
            }
            
            return "redirect:/admin/screenings";
        } catch (Exception e) {
            logger.severe("Error adding screenings: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error adding screening: " + e.getMessage());
            return "redirect:/admin/screenings/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Screening screening = screeningService.getScreeningById(id);
        if (screening == null) {
            redirectAttributes.addFlashAttribute("error", "Screening not found!");
            return "redirect:/admin/screenings";
        }
        
        model.addAttribute("screening", screening);
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("cinemas", cinemaService.getAllCinemas());
        return "edit-screeningadmin";
    }

    @PostMapping("/edit/{id}")
    public String updateScreening(@PathVariable Long id, 
                                @Valid @ModelAttribute Screening screening, 
                                BindingResult result, 
                                Model model, 
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.getAllMovies());
            model.addAttribute("cinemas", cinemaService.getAllCinemas());
            return "edit-screeningadmin";
        }

        try {
            screening.setId(id);
            // Ensure theater matches cinema name for consistency
            if (screening.getCinema() != null) {
                screening.setTheater(screening.getCinema().getName());
            }
            screeningService.saveScreening(screening);
            redirectAttributes.addFlashAttribute("success", "Screening updated successfully!");
            return "redirect:/admin/screenings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating screening: " + e.getMessage());
            return "redirect:/admin/screenings/edit/" + id;
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteScreening(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Screening screening = screeningService.getScreeningById(id);
            if (screening == null) {
                redirectAttributes.addFlashAttribute("error", "Screening not found!");
                return "redirect:/admin/screenings";
            }
            
            screeningService.deleteScreening(id);
            redirectAttributes.addFlashAttribute("success", "Screening deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting screening: " + e.getMessage());
        }
        
        return "redirect:/admin/screenings";
    }

    @GetMapping("/by-date")
    public String getScreeningsByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, 
                                    Model model) {
        List<Screening> screenings = screeningService.getScreeningsByDate(date);
        List<Movie> movies = movieService.getAllMovies();
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        
        model.addAttribute("screenings", screenings);
        model.addAttribute("movies", movies);
        model.addAttribute("cinemas", cinemas);
        model.addAttribute("selectedDate", date);
        return "screenings-admin";
    }

    @GetMapping("/by-movie/{movieId}")
    public String getScreeningsByMovie(@PathVariable Long movieId, Model model, RedirectAttributes redirectAttributes) {
        Movie movie = movieService.getMovieById(movieId);
        if (movie == null) {
            redirectAttributes.addFlashAttribute("error", "Movie not found!");
            return "redirect:/admin/screenings";
        }
        
        List<Screening> screenings = screeningService.getScreeningsByMovie(movie);
        List<Movie> movies = movieService.getAllMovies();
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        
        model.addAttribute("screenings", screenings);
        model.addAttribute("movies", movies);
        model.addAttribute("cinemas", cinemas);
        model.addAttribute("selectedMovie", movie);
        return "screenings-admin";
    }

    @GetMapping("/by-cinema/{cinemaId}")
    public String getScreeningsByCinema(@PathVariable Long cinemaId, Model model, RedirectAttributes redirectAttributes) {
        Cinema cinema = cinemaService.getCinemaById(cinemaId);
        if (cinema == null) {
            redirectAttributes.addFlashAttribute("error", "Cinema not found!");
            return "redirect:/admin/screenings";
        }
        
        List<Screening> screenings = screeningService.getScreeningsByCinema(cinema);
        List<Movie> movies = movieService.getAllMovies();
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        
        model.addAttribute("screenings", screenings);
        model.addAttribute("movies", movies);
        model.addAttribute("cinemas", cinemas);
        model.addAttribute("selectedCinema", cinema);
        return "screenings-admin";
    }
 
    // TEST ENDPOINT - Add this for debugging
    @GetMapping("/test")
    @ResponseBody
    public String testEndpoint() {
        return "AdminScreeningController is working! Current time: " + LocalDateTime.now();
    }

    // Get available times for a cinema on a specific date
    @GetMapping("/available-times")
    @ResponseBody
    public List<String> getAvailableTimes(
            @RequestParam Long cinemaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            List<LocalTime> availableTimes = screeningService.getAvailableTimes(cinemaId, date);
            
            // Convert LocalTime to string format
            List<String> timeStrings = new ArrayList<>();
            for (LocalTime time : availableTimes) {
                timeStrings.add(time.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            return timeStrings;
        } catch (Exception e) {
            logger.severe("Error getting available times: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // FIXED ENDPOINT: Get seat layout for a specific screening using DTOs
    @GetMapping("/{screeningId}/seats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getScreeningSeats(@PathVariable Long screeningId) {
        System.out.println("=== SEAT ENDPOINT CALLED ===");
        System.out.println("Screening ID: " + screeningId);
        
        try {
            // Get the screening
            Screening screening = screeningService.getScreeningById(screeningId);
            if (screening == null) {
                System.err.println("Screening not found with ID: " + screeningId);
                return ResponseEntity.notFound().build();
            }

            System.out.println("Found screening: " + screening.getMovie().getTitle() + " at " + screening.getCinema().getName());

            // Get all seats for this screening
            List<Seat> seats = seatService.getSeatsByScreeningId(screeningId);
            System.out.println("Found " + seats.size() + " seats for screening " + screeningId);
            
            // If no seats exist, initialize them first
            if (seats.isEmpty()) {
                System.out.println("No seats found, initializing seats for screening " + screeningId);
                seatService.initializeSeatsForScreening(screening);
                seats = seatService.getSeatsByScreeningId(screeningId);
                System.out.println("Initialized " + seats.size() + " seats");
            }

            // Calculate actual counts from seat data
            int totalSeats = seats.size();
            int bookedSeats = (int) seats.stream().filter(Seat::isBooked).count();
            int availableSeats = totalSeats - bookedSeats;
            
            System.out.println("Seat counts - Total: " + totalSeats + ", Booked: " + bookedSeats + ", Available: " + availableSeats);
            
            // Update screening with correct counts
            if (screening.getTotalSeats() != totalSeats || screening.getAvailableSeats() != availableSeats) {
                screening.setTotalSeats(totalSeats);
                screening.setAvailableSeats(availableSeats);
                screeningService.saveScreening(screening);
                System.out.println("Updated screening seat counts");
            }

            // Convert Seat entities to DTOs to avoid lazy loading issues
            List<SeatDTO> seatDTOs = new ArrayList<>();
            for (Seat seat : seats) {
                SeatDTO dto = new SeatDTO(
                    seat.getId(),
                    seat.getSeatNumber(),
                    seat.getRow(),
                    seat.getSeatColumn(),
                    seat.isBooked(),
                    seat.getType(),
                    seat.getPrice(),
                    seat.getBookingId()
                );
                seatDTOs.add(dto);
            }

            // Format the response data
            Map<String, Object> response = new HashMap<>();
            response.put("screeningId", screeningId);
            response.put("movieTitle", screening.getMovie().getTitle());
            response.put("cinemaName", screening.getCinema().getName());
            response.put("theaterName", screening.getTheater()); // This will now be consistent with cinema name
            
            // Format date and time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy 'at' HH:mm");
            response.put("screeningDateTime", screening.getScreeningTime().format(formatter));
            
            response.put("totalSeats", totalSeats);
            response.put("availableSeats", availableSeats);
            response.put("bookedSeats", bookedSeats);
            response.put("seats", seatDTOs); // Use DTOs instead of entities

            System.out.println("Successfully returning seat data for screening " + screeningId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error in seat endpoint: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load seats: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Bulk operations
    @PostMapping("/bulk-delete")
    public String bulkDeleteScreenings(@RequestParam List<Long> screeningIds, RedirectAttributes redirectAttributes) {
        try {
            for (Long id : screeningIds) {
                screeningService.deleteScreening(id);
            }
            redirectAttributes.addFlashAttribute("success", "Selected screenings deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting screenings: " + e.getMessage());
        }
        
        return "redirect:/admin/screenings";
    }

    // Initialize seats for all screenings
    @PostMapping("/initialize-all-seats")
    public String initializeAllSeats(RedirectAttributes redirectAttributes) {
        try {
            List<Screening> screenings = screeningService.getAllScreenings();
            int initializedCount = 0;
            
            for (Screening screening : screenings) {
                List<Seat> existingSeats = seatService.getSeatsByScreeningId(screening.getId());
                if (existingSeats.isEmpty()) {
                    seatService.initializeSeatsForScreening(screening);
                    initializedCount++;
                }
            }
            
            redirectAttributes.addFlashAttribute("success", 
                "Seats initialized for " + initializedCount + " screenings!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error initializing seats: " + e.getMessage());
        }
        
        return "redirect:/admin/screenings";
    }
}
