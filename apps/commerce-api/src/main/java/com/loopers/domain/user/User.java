package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "`user`", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "userId" })
})
public class User extends BaseEntity {
    private String userId;
    private String gender;
    private LocalDate birthDate;
    private String email;

    protected User() {
    }

    public User(String userId, String gender, LocalDate birthDate, String email) {
        validateUserId(userId);
        validateGender(gender);
        validateBirthDate(birthDate);
        validateEmail(email);

        this.userId = userId;
        this.gender = gender;
        this.birthDate = birthDate;
        this.email = email;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (userId.length() > 10) {
            throw new IllegalArgumentException("User ID cannot be longer than 10 characters");
        }
        if (!Pattern.matches("^[a-zA-Z0-9]+$", userId)) {
            throw new IllegalArgumentException("User ID can only contain alphanumeric characters");
        }
    }

    private void validateGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        if (!gender.equals("MALE") && !gender.equals("FEMALE")) {
            throw new IllegalArgumentException("Invalid gender. Must be MALE or FEMALE");
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!Pattern.matches(emailPattern, email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}