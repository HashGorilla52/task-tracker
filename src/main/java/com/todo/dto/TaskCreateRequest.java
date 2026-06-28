package com.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskCreateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255)
        String title,

        String description,

        @NotNull(message = "done is required")
        Boolean done
){}