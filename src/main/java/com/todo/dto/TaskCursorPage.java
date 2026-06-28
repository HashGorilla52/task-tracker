package com.todo.dto;

import java.util.List;

public record TaskCursorPage(List<TaskResponse> tasks, Long nextCursor) {}