package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Brand extends BaseEntity {

    private String name;

    protected Brand() {
    }

    public Brand(String name) {
        validateName(name);
        this.name = name;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand name cannot be null or empty");
        }
    }

    public Long getId() {
        return super.getId();
    }

    public String getName() {
        return this.name;
    }
}
