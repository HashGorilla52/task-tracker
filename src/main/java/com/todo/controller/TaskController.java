package com.todo.controller;

import com.todo.dto.TaskCreateRequest;
import com.todo.dto.TaskCursorPage;
import com.todo.dto.TaskResponse;
import com.todo.dto.TaskUpdateRequest;
import com.todo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Метод для получения задач с постраничной offset/limit пагинацией.
     * @param pageable - объект с параметрами страницы
     * @return - страницу с задачами
     */
    @GetMapping
    public Page<TaskResponse> getTasksPage(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC)
                                              Pageable pageable) {

        long start = System.currentTimeMillis();
        Page<TaskResponse> page = taskService.getTasksPage(pageable);
        long duration = System.currentTimeMillis() - start;
        System.out.println("Total time taken: " + duration);
        return page;
    }

    /**
     * Метод для получения списка задач с keyset/cursor пагинацией.
     * @return - список задач
     */

    @GetMapping("/cursor")
    public TaskCursorPage getTasksWithCursor(@RequestParam(required = false) Long lastId,
                                             @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return taskService.getTasksWithCursor(lastId, pageable);
    }



    /**
     * Метод для получения задачи по id.
     * @param id
     * @return
     */

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable long id) {
        return taskService.getTaskById(id);
    }

    /**
     * Метод для создания задачи.
     * @param request
     * @return
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
    public TaskResponse updateTask(@PathVariable long id, @Valid @RequestBody TaskUpdateRequest request) {
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