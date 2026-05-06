package com.todo.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskCursorPage {
    private final List<TaskResponse> tasks;
    private final Long nextCursor;
}
