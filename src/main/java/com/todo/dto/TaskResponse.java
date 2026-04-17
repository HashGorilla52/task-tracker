package com.todo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private long id;
    private String title;
    private String description;
    private boolean done;
    private LocalDateTime createdAt;
}
