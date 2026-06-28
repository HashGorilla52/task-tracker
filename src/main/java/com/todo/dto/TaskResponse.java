package com.todo.dto;

import java.time.LocalDateTime;

public record TaskResponse(
        long id,
        String title,
        String description,
        boolean done,
        LocalDateTime createdAt
) {}