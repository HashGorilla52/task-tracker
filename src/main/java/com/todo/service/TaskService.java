package com.todo.service;

import com.todo.dto.TaskCreateRequest;
import com.todo.dto.TaskResponse;
import com.todo.dto.TaskUpdateRequest;
import com.todo.exception.ResourceAlreadyExistsException;
import com.todo.exception.ResourceNotFoundException;
import com.todo.model.TaskEntity;
import com.todo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с задачами.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * Метод для преобразования сущности jpa в объект DTO TaskResponse.
     * @param entity сущность TaskEntity
     * @return TaskResponce объект.
     */
    private TaskResponse toResponse(TaskEntity entity) {
        TaskResponse response = new TaskResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setDone(entity.isDone());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    /**
     * Метод для получения количества задач сохранённых в БД.
     * @return число сохранённых задач.
     */
    public long getTasksCount() {
        return taskRepository.count();
    }

    /**
     * Метод для создания задачи.
     * @return создаваемую задачу.
     */
    public TaskResponse createTask(TaskCreateRequest request) {

        TaskEntity creatingEntity = new TaskEntity();

        if (!request.getTitle().isBlank()) {
            creatingEntity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            creatingEntity.setDescription(request.getDescription());
        }
        if (request.getDone() != null) {
            creatingEntity.setDone(request.getDone());
        }

        TaskEntity createdEntity = new TaskEntity();
        try {
            createdEntity = taskRepository.save(creatingEntity);
        }
        catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Can't create task, title is already in use");
        }

        return toResponse(createdEntity);
    }


    // Переделать со Stream API
    /**
     * Метод для получения всех задач из БД.
     * @return список всех задач.
     */
    public List<TaskResponse> getAllTasks() {
        List<TaskEntity> entities = taskRepository.findAll();
        List<TaskResponse> responses = new ArrayList<>();

        for (TaskEntity entity : entities) {
           responses.add(toResponse(entity));
        }

        return responses;
    }

    /**
     * Метод для получения задачи по id.
     * @param id задачи.
     * @return объект TaskResponse.
     */
    public TaskResponse getTaskById(long id) {
        TaskEntity entity = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "Task with id " + id + " not found"));
        return toResponse(entity);
    }

    /**
     * Метод для обновления задачи.
     * @param id задачи.
     * @param request DTO с непустыми полями на обновление.
     * @return обновлённая задача в TaskResponse.
     */
    public TaskResponse updateTask(long id, TaskUpdateRequest request) {

        TaskEntity entity = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getDone() != null) {
            entity.setDone(request.getDone());
        }

        TaskEntity updatedEntity = new TaskEntity();
        try {
            updatedEntity = taskRepository.save(entity);
        }
        catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Can't update task with id " + id + ", title is already in use");
        }


        return toResponse(updatedEntity);
    }

    /**
     * Метод для удаления задачи.
     * @param id задачи.
     * @return удалённая задача в TaskResponse.
     */
    public TaskResponse removeTaskById(long id) {
        TaskEntity entity = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task with id " + id + " not found"));
        taskRepository.delete(entity);
        return toResponse(entity);
    }
}
