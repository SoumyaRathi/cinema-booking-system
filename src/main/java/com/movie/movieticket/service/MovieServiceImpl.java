package com.movie.movieticket.service;

import com.movie.movieticket.dto.MovieDto;
import com.movie.movieticket.dto.MovieSalesDTO;
import com.movie.movieticket.model.Booking;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.model.Screening;
import com.movie.movieticket.repository.BookingRepository;
import com.movie.movieticket.repository.MovieRepository;
import com.movie.movieticket.repository.ScreeningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger logger = Logger.getLogger(MovieServiceImpl.class.getName());
    
    private static final List<String> DEFAULT_GENRES = Arrays.asList(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", 
        "Mystery", "Romance", "Science Fiction", "Thriller", "Crime", 
        "Musical", "Animation", "Documentary", "Family", "Historical", 
        "War", "Western", "Sports", "Biographical"
    );

    @Autowired
    private MovieRepository movieRepository;
    
    @Autowired
    private ScreeningRepository screeningRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    @Override
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @Override
    public Movie getMovieById(Long id) {
        return movieRepository.findById(id).orElse(null);
    }

    @Override
    public List<Movie> getReleasedMovies() {
        return movieRepository.findByReleasedTrue();
    }

    @Override
    public List<Movie> getUpcomingMovies() {
        return movieRepository.findByReleasedFalse();
    }

    // NEW: Add the missing method implementation
    @Override
    public List<Movie> getReleasedMoviesByGenre(String genre) {
        if (genre == null || genre.isEmpty() || genre.equals("All")) {
            return getReleasedMovies();
        }
        
        List<Movie> releasedMovies = getReleasedMovies();
        return releasedMovies.stream()
                .filter(movie -> movieHasGenre(movie, genre))
                .collect(Collectors.toList());
    }

    @Override
    public Movie saveMovie(MovieDto movieDto) {
        Movie movie = new Movie();
        movie.setTitle(movieDto.getTitle());
        movie.setDescription(movieDto.getDescription());
        movie.setGenre(movieDto.getGenre());
        movie.setDuration(movieDto.getDuration());
        movie.setDirector(movieDto.getDirector());
        movie.setCast(movieDto.getCast());
        movie.setImageUrl(movieDto.getImageUrl());
        movie.setImageData(movieDto.getImageData()); // NEW: Set image data
        movie.setTrailerUrl(movieDto.getTrailerUrl());
        movie.setReleased(movieDto.isReleased());
        movie.setRating(movieDto.getRating());
        movie.setLanguage(movieDto.getLanguage());
        movie.setScreeningDurationDays(movieDto.getScreeningDurationDays());
        
        Movie savedMovie = movieRepository.save(movie);
        
        logger.info("Movie saved - ID: " + savedMovie.getId() + 
                   ", Title: " + savedMovie.getTitle() + 
                   ", Has Image Data: " + (savedMovie.getImageData() != null && !savedMovie.getImageData().isEmpty()));
        
        return savedMovie;
    }

    @Override
    public Movie updateMovie(Long id, MovieDto movieDto) {
        Movie existingMovie = getMovieById(id);
        if (existingMovie == null) {
            throw new RuntimeException("Movie not found with id: " + id);
        }
        
        existingMovie.setTitle(movieDto.getTitle());
        existingMovie.setDescription(movieDto.getDescription());
        existingMovie.setGenre(movieDto.getGenre());
        existingMovie.setDuration(movieDto.getDuration());
        existingMovie.setDirector(movieDto.getDirector());
        existingMovie.setCast(movieDto.getCast());
        existingMovie.setImageUrl(movieDto.getImageUrl());
        
        // NEW: Only update image data if provided
        if (movieDto.getImageData() != null && !movieDto.getImageData().isEmpty()) {
            existingMovie.setImageData(movieDto.getImageData());
        }
        
        existingMovie.setTrailerUrl(movieDto.getTrailerUrl());
        existingMovie.setReleased(movieDto.isReleased());
        existingMovie.setRating(movieDto.getRating());
        existingMovie.setLanguage(movieDto.getLanguage());
        existingMovie.setScreeningDurationDays(movieDto.getScreeningDurationDays());
        
        Movie updatedMovie = movieRepository.save(existingMovie);
        
        logger.info("Movie updated - ID: " + updatedMovie.getId() + 
                   ", Title: " + updatedMovie.getTitle() + 
                   ", Has Image Data: " + (updatedMovie.getImageData() != null && !updatedMovie.getImageData().isEmpty()));
        
        return updatedMovie;
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {
        screeningRepository.deleteByMovieId(id);
        movieRepository.deleteById(id);
    }

    @Override
    public List<Screening> getAllScreenings() {
        return screeningRepository.findAll();
    }

    @Override
    public List<Screening> getAllUpcomingScreenings() {
        return screeningRepository.findByScreeningTimeAfter(LocalDateTime.now());
    }

    @Override
    public Screening getScreeningById(Long id) {
        return screeningRepository.findById(id).orElse(null);
    }

    @Override
    public List<Screening> getScreeningsByMovieId(Long movieId) {
        return screeningRepository.findByMovieId(movieId);
    }

    @Override
    public Screening saveScreening(Screening screening) {
        return screeningRepository.save(screening);
    }

    @Override
    public void deleteScreening(Long id) {
        screeningRepository.deleteById(id);
    }

    @Override
    public List<Movie> searchMovies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMovies();
        }
        return movieRepository.findByTitleContainingIgnoreCase(keyword);
    }
    
    @Override
    public long getMovieCount() {
        return movieRepository.count();
    }
    
    @Override
    public List<Movie> getRecentMovies(int limit) {
        return movieRepository.findAll(
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();
    }
    
    @Override
    public List<Movie> getTopGrossingMovies(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return movieRepository.findTopGrossingMovies(startDate, endDate, PageRequest.of(0, limit));
    }
    
    @Override
    public List<Movie> getMostWatchedMoviesAllTime(int limit) {
        return movieRepository.findMostWatchedMoviesAllTime(PageRequest.of(0, limit));
    }

    @Override
    public List<MovieSalesDTO> getTopGrossingMoviesWithRevenue(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<Movie> topMovies = getTopGrossingMovies(startDate, endDate, limit);
        List<MovieSalesDTO> result = new ArrayList<>();
        
        List<Booking> bookings = bookingRepository.findByBookingTimeBetweenAndPaidTrue(startDate, endDate);
        
        Map<Long, Double> movieRevenue = new HashMap<>();
        Map<Long, Integer> movieTickets = new HashMap<>();
        
        for (Booking booking : bookings) {
            if (booking.getScreening() != null && booking.getScreening().getMovie() != null) {
                Long movieId = booking.getScreening().getMovie().getId();
                
                double currentRevenue = movieRevenue.getOrDefault(movieId, 0.0);
                movieRevenue.put(movieId, currentRevenue + booking.getTotalPrice());
                
                int currentTickets = movieTickets.getOrDefault(movieId, 0);
                movieTickets.put(movieId, currentTickets + booking.getNumberOfSeats());
            }
        }
  
        for (Movie movie : topMovies) {
            MovieSalesDTO dto = new MovieSalesDTO();
            dto.setId(movie.getId());
            dto.setTitle(movie.getTitle());
            dto.setImageUrl(movie.getEffectiveImageUrl()); // NEW: Use effective image URL
            dto.setGenre(movie.getGenre());
            dto.setRevenue(movieRevenue.getOrDefault(movie.getId(), 0.0));
            dto.setTicketsSold(movieTickets.getOrDefault(movie.getId(), 0));
            result.add(dto);
        }
        
        return result;
    }
    
    @Override
    public Set<String> getAllGenres() {
        Set<String> genres = extractAllGenres();
        
        if (genres.isEmpty()) {
            logger.info("No genres found in database, using default genres");
            return new HashSet<>(DEFAULT_GENRES);
        }
        
        return genres;
    }
    
    @Override
    public List<Movie> getTopGrossingMoviesByGenre(LocalDateTime startDate, LocalDateTime endDate, String genre, int limit) {
        if (genre == null || genre.isEmpty() || genre.equals("All")) {
            return getTopGrossingMovies(startDate, endDate, limit);
        }
        return movieRepository.findTopGrossingMoviesByGenre(startDate, endDate, genre, PageRequest.of(0, limit));
    }
    
    @Override
    public List<Movie> getMostWatchedMoviesByGenre(String genre, int limit) {
        if (genre == null || genre.isEmpty() || genre.equals("All")) {
            return getMostWatchedMoviesAllTime(limit);
        }
        return movieRepository.findMostWatchedMoviesByGenre(genre, PageRequest.of(0, limit));
    }
    
    @Override
    public List<MovieSalesDTO> getTopGrossingMoviesWithRevenueByGenre(LocalDateTime startDate, LocalDateTime endDate, String genre, int limit) {
        List<Movie> topMovies;
        
        if (genre == null || genre.isEmpty() || genre.equals("All")) {
            topMovies = getTopGrossingMovies(startDate, endDate, limit);
        } else {
            topMovies = getTopGrossingMoviesByGenre(startDate, endDate, genre, limit);
        }
        
        List<MovieSalesDTO> result = new ArrayList<>();
        
        List<Booking> bookings = bookingRepository.findByBookingTimeBetweenAndPaidTrue(startDate, endDate);
        
        if (genre != null && !genre.isEmpty() && !genre.equals("All")) {
            bookings = bookings.stream()
                    .filter(b -> b.getScreening() != null && 
                                b.getScreening().getMovie() != null && 
                                movieHasGenre(b.getScreening().getMovie(), genre))
                    .collect(Collectors.toList());
        }
        
        Map<Long, Double> movieRevenue = new HashMap<>();
        Map<Long, Integer> movieTickets = new HashMap<>();
        
        for (Booking booking : bookings) {
            if (booking.getScreening() != null && booking.getScreening().getMovie() != null) {
                Long movieId = booking.getScreening().getMovie().getId();
                
                double currentRevenue = movieRevenue.getOrDefault(movieId, 0.0);
                movieRevenue.put(movieId, currentRevenue + booking.getTotalPrice());
                
                int currentTickets = movieTickets.getOrDefault(movieId, 0);
                movieTickets.put(movieId, currentTickets + booking.getNumberOfSeats());
            }
        }
        
        for (Movie movie : topMovies) {
            MovieSalesDTO dto = new MovieSalesDTO();
            dto.setId(movie.getId());
            dto.setTitle(movie.getTitle());
            dto.setImageUrl(movie.getEffectiveImageUrl()); // NEW: Use effective image URL
            dto.setGenre(movie.getGenre());
            dto.setRevenue(movieRevenue.getOrDefault(movie.getId(), 0.0));
            dto.setTicketsSold(movieTickets.getOrDefault(movie.getId(), 0));
            result.add(dto);
        }
        
        return result;
    }
    
    @Override
    public Set<String> extractAllGenres() {
        Set<String> allGenres = new HashSet<>();
        List<Movie> allMovies = getAllMovies();
        
        for (Movie movie : allMovies) {
            if (movie.getGenre() != null && !movie.getGenre().isEmpty()) {
                String[] genreArray = movie.getGenre().split(",\\s*");
                for (String genre : genreArray) {
                    if (!genre.trim().isEmpty()) {
                        allGenres.add(genre.trim());
                    }
                }
            }
        }
        
        if (allGenres.isEmpty()) {
            logger.info("No genres found in database, using default genres");
            return new HashSet<>(DEFAULT_GENRES);
        }
        
        return allGenres;
    }
    
    @Override
    public boolean movieHasGenre(Movie movie, String genreToCheck) {
        if (genreToCheck == null || genreToCheck.isEmpty() || genreToCheck.equals("All")) {
            return true;
        }
        
        if (movie.getGenre() == null || movie.getGenre().isEmpty()) {
            return false;
        }
        
        String[] genres = movie.getGenre().split(",\\s*");
        for (String genre : genres) {
            if (genre.trim().equalsIgnoreCase(genreToCheck.trim())) {
                return true;
            }
        }
        
        return false;
    }
}