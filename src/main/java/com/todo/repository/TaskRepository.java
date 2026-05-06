package com.todo.repository;

import com.todo.model.TaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    @Query("SELECT t FROM TaskEntity t WHERE (:lastId IS NULL OR t.id > :lastId) ORDER BY t.id")
    public List<TaskEntity> findNextPage(@Param("lastId") Long lastId, Pageable pageable);
}