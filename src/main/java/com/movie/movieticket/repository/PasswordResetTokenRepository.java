package com.movie.movieticket.repository;

import com.movie.movieticket.model.PasswordResetToken;
import com.movie.movieticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);
    PasswordResetToken findByUser(User user);
    void deleteByUser(User user);
}
