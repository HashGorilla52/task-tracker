package com.todo.model;

import java.time.LocalDateTime;

public interface Task {

    public long getId();

    public String getTitle();

    public String getDescription();

    public boolean isDone();

    public LocalDateTime getCreatedAt();

    public void markAsDone();

    public void markAsUndone();

    public void updateTitle(String newTitle);

    public void updateDescription(String newDescription);

    public boolean hasDescription();

    public Task copy();
}
