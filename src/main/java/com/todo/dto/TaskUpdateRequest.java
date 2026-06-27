package com.todo.dto;

import lombok.Data;

/**
 * DTO класс для обновления полей задачи (Task).
 */
@Data
public class TaskUpdateRequest
{
    private String title;
    private String description;
    private Boolean done;
}
