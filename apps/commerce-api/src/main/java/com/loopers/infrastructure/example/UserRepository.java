package com.loopers.infrastructure.example;

import com.loopers.domain.example.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    @Query("SELECT p.amount FROM ExamplePoint p WHERE p.userId = :userId")
    int findUserPointByUserId(@Param("userId") String userId);

    boolean existsByUserId(String userId);

    @Query("UPDATE ExamplePoint p SET p.amount = :newPoints WHERE p.userId = :userId")
    int updateUserPoints(@Param("userId") String userId, @Param("newPoints") int newPoints);
}
