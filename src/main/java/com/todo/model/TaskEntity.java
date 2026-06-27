package com.todo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tasks_seq_gen")
    @SequenceGenerator(name = "tasks_seq_gen", sequenceName = "tasks_id_seq", allocationSize = 50)
    @Column(name = "id",  nullable = false)
    private long id;

    @Column (name = "title", nullable = false)
    private String title;

    @Column (name = "description")
    private String description;

    @Column (name = "is_done", nullable = false)
    private boolean done;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskEntity() {}
}
