package com.movie.movieticket.config;

import com.movie.movieticket.model.Cinema;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Role;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.model.User;
import com.movie.movieticket.repository.CinemaRepository;
import com.movie.movieticket.repository.MovieRepository;
import com.movie.movieticket.repository.RoleRepository;
import com.movie.movieticket.repository.ScreeningRepository;
import com.movie.movieticket.repository.UserRepository;
import com.movie.movieticket.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ScreeningRepository screeningRepository;
    
    @Autowired
    private CinemaRepository cinemaRepository;
    
    @Autowired
    private SeatService seatService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.count() == 0) {
            Role adminRole = new Role("ROLE_ADMIN");
            Role userRole = new Role("ROLE_USER");
            roleRepository.saveAll(Arrays.asList(adminRole, userRole));
        }

        // Initialize admin user if it doesn't exist
        if (userRepository.findByEmail("admin@example.com") == null) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN");
            User adminUser = new User(
                    "Admin",
                    "User",
                    "admin@example.com",
                    passwordEncoder.encode("admin123"),
                    Arrays.asList(adminRole)
            );
            adminUser.setEnabled(true);
            adminUser.setPrivacyPolicyAccepted(true);
            userRepository.save(adminUser);
        }
        
        // Initialize cinemas if they don't exist
        if (cinemaRepository.count() == 0) {
            Cinema cinema1 = new Cinema("Cinema 1", 120, "standard");
            Cinema cinema2 = new Cinema("Cinema 2", 100, "standard");
            Cinema cinema3 = new Cinema("Cinema 3", 80, "premium");
            cinemaRepository.saveAll(Arrays.asList(cinema1, cinema2, cinema3));
            System.out.println("Cinemas initialized successfully");
        }

        // Initialize sample movies if none exist
        if (movieRepository.count() == 0) {
            // Create movies with embedded Base64 images for database storage
            Movie movie1 = new Movie();
            movie1.setTitle("Dune: Part Two");
            movie1.setDescription("Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.");
            movie1.setGenre("Sci-Fi, Adventure");
            movie1.setDuration(166);
            movie1.setDirector("Denis Villeneuve");
            movie1.setCast("Timothée Chalamet, Zendaya, Rebecca Ferguson");
            movie1.setImageUrl("/images/dune2.jpg"); // Fallback URL
            // Add Base64 image data for database storage
            movie1.setImageData(getSampleMoviePoster1());
            movie1.setTrailerUrl("https://www.youtube.com/watch?v=Way9Dexny3w");
            movie1.setReleased(true);
            movie1.setRating("PG-13");
            movie1.setLanguage("English");
            movie1.setScreeningDurationDays(28);

            Movie movie2 = new Movie();
            movie2.setTitle("Inception");
            movie2.setDescription("A thief who steals corporate secrets through the use of dream-sharing technology.");
            movie2.setGenre("Sci-Fi, Action");
            movie2.setDuration(148);
            movie2.setDirector("Christopher Nolan");
            movie2.setCast("Leonardo DiCaprio, Joseph Gordon-Levitt, Ellen Page");
            movie2.setImageUrl("/images/inception.jpg"); // Fallback URL
            // Add Base64 image data for database storage
            movie2.setImageData(getSampleMoviePoster2());
            movie2.setTrailerUrl("https://www.youtube.com/watch?v=YoHD9XEInc0");
            movie2.setReleased(true);
            movie2.setRating("PG-13");
            movie2.setLanguage("English");
            movie2.setScreeningDurationDays(28);

            Movie movie3 = new Movie();
            movie3.setTitle("The Shawshank Redemption");
            movie3.setDescription("Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.");
            movie3.setGenre("Drama");
            movie3.setDuration(142);
            movie3.setDirector("Frank Darabont");
            movie3.setCast("Tim Robbins, Morgan Freeman");
            movie3.setImageUrl("/images/shawshank.jpg"); // Fallback URL
            // Add Base64 image data for database storage
            movie3.setImageData(getSampleMoviePoster3());
            movie3.setTrailerUrl("https://www.youtube.com/watch?v=6hB3S9bIaco");
            movie3.setReleased(true);
            movie3.setRating("R");
            movie3.setLanguage("English");
            movie3.setScreeningDurationDays(28);

            movieRepository.saveAll(Arrays.asList(movie1, movie2, movie3));

            // Get cinemas for screenings
            Cinema cinema1 = cinemaRepository.findByName("Cinema 1");
            Cinema cinema2 = cinemaRepository.findByName("Cinema 2");
            Cinema cinema3 = cinemaRepository.findByName("Cinema 3");
            
            // Create screenings with the correct constructor and cinema
            LocalDateTime now = LocalDateTime.now();
            
            // Create Dune: Part Two screening that matches your image
            Screening screening1 = new Screening();
            screening1.setMovie(movie1);
            screening1.setScreeningTime(LocalDateTime.of(2025, 5, 28, 22, 0)); // Wed, May 28, 2025 at 22:00
            screening1.setPrice(390.0); // Match the price in your image
            screening1.setTotalSeats(100); // Set to 100 to match your requirement
            screening1.setCinema(cinema2); // Cinema 2 as shown in your image
            screening1.setAvailableSeats(100);

            Screening screening2 = new Screening();
            screening2.setMovie(movie1);
            screening2.setScreeningTime(now.plusDays(1).withHour(21).withMinute(0));
            screening2.setPrice(390.0);
            screening2.setTotalSeats(100);
            screening2.setCinema(cinema3);
            screening2.setAvailableSeats(100);

            Screening screening3 = new Screening();
            screening3.setMovie(movie2);
            screening3.setScreeningTime(now.plusDays(2).withHour(19).withMinute(30));
            screening3.setPrice(350.0);
            screening3.setTotalSeats(120);
            screening3.setCinema(cinema1);
            screening3.setAvailableSeats(120);

            screeningRepository.saveAll(Arrays.asList(screening1, screening2, screening3));
            
            // Initialize seats for each screening
            seatService.initializeSeatsForScreening(screening1);
            seatService.initializeSeatsForScreening(screening2);
            seatService.initializeSeatsForScreening(screening3);
            
            System.out.println("Movies and screenings initialized successfully with embedded poster images");
        }
    }
    
    /**
     * Sample movie poster images as Base64 strings
     * These are small placeholder images that will be stored in the database
     */
    private String getSampleMoviePoster1() {
        // This is a small sample image encoded as Base64 (you can replace with actual movie posters)
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
    
    private String getSampleMoviePoster2() {
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
    
    private String getSampleMoviePoster3() {
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    }
}