package com.todo.model;

import java.time.LocalDateTime;
public class BaseTask implements Task {

    private final long id;
    private String title;
    private String description = "";
    private boolean isDone;
    private final LocalDateTime createdAt;

//    public BaseTask(long id, String title, String description, boolean isDone)
//    {
//        this.id = id;
//        this.title = title;
//        this.description = description;
//        this.isDone = isDone;
//        this.createdAt = LocalDateTime.now();
//    }

    public BaseTask(long id, String title, String description, boolean isDone, LocalDateTime createdAt)
    {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isDone = isDone;
        this.createdAt = createdAt;
    }

//    public BaseTask(long id, String title, String description)
//    {
//        this(id, title, description, false);
//    }

    @Override
    public long getId() {return this.id;}

    @Override
    public String getTitle() {return this.title;}

    @Override
    public String getDescription() {return this.description;}

    @Override
    public boolean isDone() {return this.isDone;}

    @Override
    public LocalDateTime getCreatedAt() {return this.createdAt;}

    @Override
    public void markAsDone() {this.isDone = true;}

    @Override
    public void markAsUndone() {this.isDone = false;}

    @Override
    public void updateTitle(String newTitle) {this.title = newTitle;}

    @Override
    public void updateDescription(String newDescription) {this.description = newDescription;}

    @Override
    public boolean hasDescription()
    {
        if (!this.description.isBlank())
        {
            return true;
        }
        return false;
    }

    @Override
    public Task copy()
    {
        return new BaseTask(this.id, this.title, this.description, this.isDone, this.createdAt);
    }


}
