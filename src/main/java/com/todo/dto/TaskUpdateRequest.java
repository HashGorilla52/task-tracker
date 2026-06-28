package com.todo.dto;

/**
 * DTO класс для обновления полей задачи (Task).
 */
public record TaskUpdateRequest(
        String title,
        String description,
        Boolean done
){}