package com.todo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",  nullable = false)
    private long id;

    @Column (name = "title", nullable = false)
    private String title;

    @Column (name = "description")
    private String description = "";

    @Column (name = "is_done", nullable = false)
    private boolean done;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TaskEntity() {}
}
