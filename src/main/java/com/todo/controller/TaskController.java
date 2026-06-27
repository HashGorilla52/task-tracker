package com.todo.controller;

import com.todo.dto.TaskCreateRequest;
import com.todo.dto.TaskCursorPage;
import com.todo.dto.TaskResponse;
import com.todo.dto.TaskUpdateRequest;
import com.todo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Value("${app.pagination.cursor.max-limit:100}")
    private int maxCursorLimit;

    /**
     * Метод для получения задач с постраничной offset/limit пагинацией.
     * По умолчанию возвращает страницу с первыми 10 объектами из БД.
     * @param pageable - объект с параметрами страницы
     * @return - страницу с задачами
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<TaskResponse> getTasksPage(@PageableDefault(sort = "id", direction = Sort.Direction.ASC)
                                              Pageable pageable) {
        return taskService.getTasksPage(pageable);
    }

    /**
     * Метод для получения списка задач с keyset/cursor пагинацией.
     * @return - список задач
     */

    @GetMapping("/cursor")
    @ResponseStatus(HttpStatus.OK)
    public TaskCursorPage getTasksWithCursor(@RequestParam(required = false) Long lastId,
                                             @RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0){
           limit = 10;
        }
        else if (limit > maxCursorLimit){
            limit = maxCursorLimit;
        }

        Pageable pageable = PageRequest.of(0, limit);
        return taskService.getTasksWithCursor(lastId, pageable);
    }

    /**
     * Метод для получения задачи по id.
     * @param id
     * @return
     */

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TaskResponse getTaskById(@PathVariable long id) {
        return taskService.getTaskById(id);
    }

    /**
     * Метод для создания задачи.
     * @param request - dto объект с полями для запроса на создание задачи,
     *                в случае невалидности данных вместе с ответом возвращает список ошибок.
     * @return создаваемую задачу.
     */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    /**
     * Метод для обновления полей задачи. Возможно частичное обновление.
     * @param id
     * @param request
     * @return
     */

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TaskResponse updateTask(@PathVariable long id, @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(id, request);
    }

    /**
     * Удаляет задачу.
     * @param id
     */

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaskById(@PathVariable long id) {
        taskService.removeTaskById(id);
    }
}