package com.loopers.infrastructure.example;

import com.loopers.domain.example.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
