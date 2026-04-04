package com.todo.dto;

/**
 * DTO класс для обновления полей задачи (Task).
 */
public class TaskUpdateRequest
{
    private String title;
    private String description;
    private Boolean isDone;

    public String getTitle() {return this.title;}
    public String getDescription() {return this.description;}
    public Boolean getDone() {return this.isDone;}

    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setDone(Boolean done) {this.isDone = done;}
}
