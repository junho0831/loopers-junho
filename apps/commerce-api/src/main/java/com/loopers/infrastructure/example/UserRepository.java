package com.loopers.infrastructure.example;

import com.loopers.domain.example.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    // TODO: Point 엔티티로 이동 예정
    // int findUserPointByUserId(String userId);
    // int updateUserPoints(String userId, int newPoints);

    boolean existsByUserId(String userId);
}
