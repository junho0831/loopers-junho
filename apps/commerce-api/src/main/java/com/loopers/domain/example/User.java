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

    @Embedded
    private Email email;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;



    public User(String userId, String email, String birthday, Gender gender) {
        validate(userId, email, birthday, gender);

        this.userId = userId;
        this.email = new Email(email);
        this.birthday = parseBirthday(birthday);
        this.gender = gender;
    }




    protected User() {}

    private void validate(String userId, String email, String birthday, Gender gender) {
        validateUserId(userId);
        // Email 검증은 Email VO 생성자에서 처리
        validateBirthday(birthday);
        validateGender(gender);
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

    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.INVALID_GENDER);
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email.getValue();
    }

    public LocalDate getBirthday(){
        return birthday;
    }
    public Gender getGender() {
        return gender;
    }
}
