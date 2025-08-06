package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isAlphanumeric;

@Entity
@Table(name = "`user`", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "userId" })
})
public class User extends BaseEntity {
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Embedded
    private Email email;
    
    @Embedded
    private Point point;
    
    private LocalDate birthDate;

    protected User() {
    }

    public User(String userId, Gender gender, LocalDate birthDate, Email email, Point point) {
        validateUserId(userId);
        validateGender(gender);
        validateBirthDate(birthDate);
        validateEmail(email);

        this.userId = userId;
        this.gender = gender;
        this.birthDate = birthDate;
        this.email = email;
        this.point = point;
    }

    public User(String userId, Email email, String birthday, Gender gender) {
        LocalDate parsedBirthDate = LocalDate.parse(birthday);

        validateUserId(userId);
        validateGender(gender);
        validateBirthDate(parsedBirthDate);

        this.userId = userId;
        this.gender = gender;
        this.birthDate = parsedBirthDate;
        this.email = email;
        this.point = new Point(0);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.INVALID_USER_ID);
        }
        if (userId.length() > 10) {
            throw new CoreException(ErrorType.INVALID_USER_ID_LENGTH);
        }
        if (!isAlphanumeric(userId)) {
            throw new CoreException(ErrorType.INVALID_USER_ID_FORMAT);
        }
    }

    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.INVALID_GENDER);
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new CoreException(ErrorType.INVALID_BIRTHDAY);
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new CoreException(ErrorType.INVALID_BIRTHDAY_FORMAT);
        }
    }

    private void validateEmail(Email email) {
        if (email == null) {
            throw new CoreException(ErrorType.INVALID_EMAIL);
        }
    }

    public String getUserId() {
        return userId;
    }

    public Gender getGender() {
        return gender;
    }
    
    public Email getEmail() {
        return email;
    }
    
    public Point getPoint() {
        return point;
    }

    public LocalDate getBirthDate() {
        return birthDate;
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
