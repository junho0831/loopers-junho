package com.loopers.domain.example;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class Email {
    
    private String value;
    
    public Email(String value) {
        validate(value);
        this.value = value;
    }
    
    protected Email() {}
    
    private void validate(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.INVALID_EMAIL);
        }
        if (!isValidEmailFormat(email)) {
            throw new CoreException(ErrorType.INVALID_EMAIL_FORMAT);
        }
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
    
    public String getValue() {
        return value;
    }
}