package com.todo.service;

import com.todo.dto.TaskCreateRequest;
import com.todo.dto.TaskCursorPage;
import com.todo.dto.TaskResponse;
import com.todo.dto.TaskUpdateRequest;
import com.todo.exception.ResourceAlreadyExistsException;
import com.todo.exception.ResourceNotFoundException;
import com.todo.exception.ValidationException;
import com.todo.model.TaskEntity;
import com.todo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @return TaskResponse объект.
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
     * @param request - dto объект с полями для запроса на создание задачи,
     *                в случае невалидности данных вместе с ответом возвращает список ошибок.
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

        TaskEntity createdEntity;
        try {
            createdEntity = taskRepository.save(creatingEntity);
        }
        catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Can't create task, title is already in use");
        }

        return toResponse(createdEntity);
    }

    /**
     * Метод для получения всех задач из БД.
     * @return список всех задач.
     */
    public Page<TaskResponse> getTasksPage(Pageable pageable) {
       return taskRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Метод для получения страницы с задачами размером, передаваемым в объекте Pageable, начиная с
     * задачи, следующей за задачей с id, равным lastId; реализует keyset/cursor пагинацию.
     * @param lastId - id последней задачи
     * @param pageable - объект, хранящий размер желаемой страницы
     * @return - объект со списком задач и курсор(id последней задачи)
     */
    public TaskCursorPage getTasksWithCursor(Long lastId, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int noOffset = 0;

        Pageable nextPageable = PageRequest.of(noOffset, pageSize + 1, pageable.getSort());
        List<TaskResponse> tasks = taskRepository.findNextPage(lastId, nextPageable).
                stream().map(this::toResponse).toList();

        Long nextCursor = null;
        if (tasks.size() == pageSize + 1){
            tasks = tasks.subList(0, pageSize);
            nextCursor = tasks.get(pageSize - 1).getId();
        }

        return new TaskCursorPage(tasks, nextCursor);
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

        Map<String, String> errors = new HashMap<>();

        TaskEntity entity = taskRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Task with id " + id + " not found"));

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank() || request.getTitle().length() > 255) {
                errors.put("title", "title must be between 1 and 255 characters");
            }
            else {
                entity.setTitle(request.getTitle());
            }
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getDone() != null) {
            entity.setDone(request.getDone());
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            taskRepository.save(entity);
        }
        catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("Can't update task with id " + id + ", title is already in use");
        }

        return toResponse(entity);
    }

    /**
     * Метод для удаления задачи.
     * @param id задачи.
     * @return удалённая задача в TaskResponse.
     */
    public TaskResponse removeTaskById(long id) {
        TaskEntity entity = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "Task with id " + id + " not found"));
        taskRepository.delete(entity);
        return toResponse(entity);
    }
}
