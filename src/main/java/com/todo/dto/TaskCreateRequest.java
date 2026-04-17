package com.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "title is required")
    @Size(min = 1, max = 255)
    private String title;
    private String description;
    @NotNull(message = "done is required")
    private Boolean done;
}
