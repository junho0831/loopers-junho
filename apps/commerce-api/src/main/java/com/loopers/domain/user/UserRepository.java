package com.loopers.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserId(String userId);

    @Query("SELECT u.point.amount FROM User u WHERE u.userId = :userId")
    int findUserPointByUserId(@Param("userId") String userId);

    boolean existsByUserId(String userId);

    @Query("UPDATE User u SET u.point.amount = :newPoints WHERE u.userId = :userId")
    int updateUserPoints(@Param("userId") String userId, @Param("newPoints") int newPoints);
}
