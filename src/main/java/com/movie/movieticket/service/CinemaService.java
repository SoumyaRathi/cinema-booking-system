package com.movie.movieticket.service;

import com.movie.movieticket.model.Cinema;
import java.util.List;

public interface CinemaService {
    List<Cinema> getAllCinemas();
    Cinema getCinemaById(Long id);
    Cinema saveCinema(Cinema cinema);
    void deleteCinema(Long id);
}