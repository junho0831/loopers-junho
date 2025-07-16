package com.loopers.domain.example;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.apache.commons.lang3.StringUtils.isAlphanumeric;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;


    public User(String userId, String email, String birthday, Gender gender) {
        validateUserId(userId);
        validateEmail(email);
        validateBirthday(birthday);
        validateGender(gender);

        this.userId = userId;
        this.email = email;
        this.birthday = parseBirthday(birthday);
        this.gender = gender;
    }



    protected User() {}

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

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.INVALID_EMAIL);
        }
        if (!isValidEmailFormat(email)) {
            throw new CoreException(ErrorType.INVALID_EMAIL_FORMAT);
        }
    }

    private void validateBirthday(String birthday) {
        if (birthday == null || birthday.isBlank()) {
            throw new CoreException(ErrorType.INVALID_BIRTHDAY);
        }
        try {
            LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.INVALID_BIRTHDAY_FORMAT);
        }
    }

    private LocalDate parseBirthday(String birthday) {
        return LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private boolean isValidEmailFormat(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        int dotIndex = domain.lastIndexOf('.');

        return dotIndex > 0 && dotIndex < domain.length() - 1;
    }
    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.INVALID_GENDER);
        }
    }
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthday(){
        return birthday;
    }
    public Gender getGender() {
        return gender;
    }
}
