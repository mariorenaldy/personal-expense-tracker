package com.mariorenaldy.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
        String color
) {
        public CategoryRequest {
                name = (name == null) ? null : name.trim();
        }
}