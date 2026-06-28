package com.todo.controller;

import com.todo.dto.TaskCreateRequest;
import com.todo.dto.TaskCursorPage;
import com.todo.dto.TaskResponse;
import com.todo.dto.TaskUpdateRequest;
import com.todo.exception.ResourceAlreadyExistsException;
import com.todo.exception.ResourceNotFoundException;
import com.todo.exception.ValidationException;
import com.todo.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;



@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService mockService;

    private String asJsonString(Object obj) {

        try {
            return new ObjectMapper().writeValueAsString(obj);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createTask_ShouldReturnCreatedTask_WhenRequestIsValid() throws Exception {
        //GIVEN
        TaskCreateRequest request = new TaskCreateRequest("title", "description", true);

        TaskResponse response = new TaskResponse(
                1L,
                request.title(),
                request.description(),
                request.done(),
                LocalDateTime.now()
                );

        when(mockService.createTask(any(TaskCreateRequest.class))).thenReturn(response);

        //WHEN & THEN
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value(response.title()));
    }

    @Test
    public void createTask_ShouldReturnProblemDetail_WhenRequestIsInvalid() throws Exception {
        // GIVEN
        TaskCreateRequest invalidRequest = new TaskCreateRequest("",  "description", null);

        // WHEN & THEN
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.instance").value("/api/tasks"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("check 'errors' field"))
                .andExpect(jsonPath("$.errors.title").value("title is required"))
                .andExpect(jsonPath("$.errors.done").value("done is required"));
    }

    @Test
    public void getTaskById_ShouldReturnTask_WhenTaskExists() throws Exception {
        // GIVEN
        long id = 1L;
        TaskResponse response = new TaskResponse(
                1L,
                "title",
                "description",
                true,
                LocalDateTime.now()
                );

        when(mockService.getTaskById(id)).thenReturn(response);

        // WHEN & THEN
        mockMvc.perform(get("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(response.title()))
                .andExpect(jsonPath("$.description").value(response.description()))
                .andExpect(jsonPath("$.done").value(response.done()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    public void getTaskById_ShouldReturnProblemDetail_WhenTaskDoesNotExist() throws Exception {
        // GIVEN
        long id = 999L;
        when(mockService.getTaskById(id)).thenThrow(new ResourceNotFoundException("Task with id " + id + " not found"));

        // WHEN & THEN
        mockMvc.perform(get("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Task with id " + id + " not found"))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Nested
    class PaginationTests {

        private List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        private List<TaskResponse> tasks;
        int noOffset = 0;
        int limit = 5;
        int totalElements;

        @BeforeEach
        void setUp(){
            this.tasks = ids.stream()
                    .map(id -> {
                        TaskResponse task = new TaskResponse(
                                id,
                                "title" + id,
                                "",
                                false,
                                LocalDateTime.now()
                                );
                        return task;
                    }).toList();

            totalElements = tasks.size();
        }

        @Test
        public void getTasksPage_ShouldReturnTasksPage_WhenTasksExist() throws Exception {
            // GIVEN
            int pageNumber = 1;
            int pageSize = 5;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageSize, tasks.size());
            List<TaskResponse> expectedTasks = tasks.subList(start, end);

            Page<TaskResponse> expectedPage = new PageImpl<>(expectedTasks, pageable, totalElements);

            when(mockService.getTasksPage(any(Pageable.class))).thenReturn(expectedPage);

            // WHEN & THEN
           mockMvc.perform(get("/api/tasks?page={pageNumber}&size={pageSize}", pageNumber, pageSize)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPages").value(expectedPage.getTotalPages()))
                    .andExpect(jsonPath("$.totalElements").value(expectedPage.getTotalElements()))
                    .andExpect(jsonPath("$.size").value(expectedPage.getSize()))
                    .andExpect(jsonPath("$.number").value(expectedPage.getNumber()))
                    .andExpect(jsonPath("$.empty").value(expectedPage.isEmpty()))
                    .andExpect(jsonPath("$.first").value(expectedPage.isFirst()))
                    .andExpect(jsonPath("$.last").value(expectedPage.isLast()))
                    .andExpect(jsonPath("$.content.length()").value(expectedTasks.size()))
                    .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(expectedTasks.stream()
                            .map(TaskResponse::id)
                            .map(Long::intValue)
                            .toArray())));
        }

        @Test
        public void getTasksPage_ShouldReturnEmptyPage_WhenPageIsMissing() throws Exception {
            // GIVEN
            int pageNumber = 999;
            int pageSize = 10;
            int total = 0;

            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<TaskResponse> expectedPage = new PageImpl<>(List.of(), pageable, total);

            when(mockService.getTasksPage(any(Pageable.class))).thenReturn(expectedPage);

            // WHEN & THEN
            mockMvc.perform(get("/api/tasks?page={pageNumber}&size={pageSize}", pageNumber, pageSize)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPages").value(expectedPage.getTotalPages()))
                    .andExpect(jsonPath("$.totalElements").value(expectedPage.getTotalElements()))
                    .andExpect(jsonPath("$.size").value(expectedPage.getSize()))
                    .andExpect(jsonPath("$.number").value(expectedPage.getNumber()))
                    .andExpect(jsonPath("$.empty").value(expectedPage.isEmpty()))
                    .andExpect(jsonPath("$.first").value(expectedPage.isFirst()))
                    .andExpect(jsonPath("$.last").value(expectedPage.isLast()))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        public void getTasksWithCursor_ShouldReturnTasksWithCursor_WhenCursorIsNull() throws Exception {
            // GIVEN
            Pageable pageable = PageRequest.of(noOffset, limit);
            Long lastId = null;

            List<TaskResponse> expectedTasks = tasks.subList(0, limit);
            List<Integer> expectedIds = expectedTasks.stream().map(TaskResponse::id).map(Long::intValue).toList();
            Long expectedNextCursor = (long) expectedIds.get(expectedIds.size() - 1);

            TaskCursorPage expectedPage = new TaskCursorPage(expectedTasks, expectedNextCursor);
            when(mockService.getTasksWithCursor(isNull(), any(Pageable.class))).thenReturn(expectedPage);

            // WHEN & THEN
            mockMvc.perform(get("/api/tasks/cursor?lastId={lastId}&limit={limit}", lastId, limit)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(expectedTasks.size()))
                    .andExpect(jsonPath("$.tasks[*].id", containsInAnyOrder(expectedIds.toArray(Integer[]::new))))
                    .andExpect(jsonPath("$.nextCursor").value(expectedNextCursor));
        }

        @Test
        public void getTasksWithCursor_ShouldReturnTasksWithCursor_WhenCursorIsNotNull() throws Exception {
            // GIVEN
            Long lastId = ids.get(limit - 1); // id последнего элемента с первой страницы
            Pageable pageable = PageRequest.of(noOffset, limit);
            List<TaskResponse> expectedTasks = tasks.subList(limit, tasks.size()); // задачи со второй (последней) страницы
            Long nextCursor = null;
            TaskCursorPage expectedPage = new TaskCursorPage(expectedTasks, nextCursor);
            List<Integer> expectedIds = expectedTasks.stream().map(TaskResponse::id).map(Long::intValue).toList();


            when(mockService.getTasksWithCursor(eq(lastId), any(Pageable.class))).thenReturn(expectedPage);

            // WHEN & THEN
            mockMvc.perform(get("/api/tasks/cursor?lastId={lastId}&limit={limit}", lastId, limit)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(expectedTasks.size()))
                    .andExpect(jsonPath("$.tasks[*].id", containsInAnyOrder(expectedIds.toArray(Integer[]::new))))
                    .andExpect(jsonPath("$.nextCursor").value(nextCursor));
        }

        @Test
        public void getTasksWithCursor_ShouldReturnEmptyList_WhenLastIdIsMoreThanLatest() throws Exception {
            // GIVEN
            Long lastId = 999L;
            Pageable pageable = PageRequest.of(noOffset, limit);
            List<TaskResponse> expectedEmptyTasks = List.of();
            Long nextCursor = null;
            TaskCursorPage expectedPage = new TaskCursorPage(expectedEmptyTasks, nextCursor);

            when(mockService.getTasksWithCursor(eq(lastId), any(Pageable.class))).thenReturn(expectedPage);

            // WHEN & THEN
            mockMvc.perform(get("/api/tasks/cursor?lastId={lastId}&limit={limit}", lastId, limit)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasks.length()").value(expectedEmptyTasks.size()))
                    .andExpect(jsonPath("$.nextCursor").value(nextCursor));
        }

        @Test
        public void getTasksWithCursor_ShouldReturnProblemDetail_WhenArgumentTypeMismatch() throws Exception {
            String invalidId = "invalidId";
            mockMvc.perform(get("/api/tasks/cursor?lastId={invalidId}", invalidId)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Type", "application/problem+json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Parameter 'lastId' should be of type Long"))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.title").value("Invalid Parameter"))
                    .andExpect(jsonPath("$.instance").value("/api/tasks/cursor"));
        }
    }

    @Test
    public void updateTask_ShouldReturnUpdatedTask_WhenTaskExistsAndRequestParametersAreCorrect() throws Exception {
        // GIVEN
        long id = 1L;
        TaskUpdateRequest request = new TaskUpdateRequest("New Title", null, null);
        // на PATCH запросе меняем только title

        TaskResponse updatedTask = new TaskResponse(
                id,
                request.title(),
                "Old Description",
                false,
                LocalDateTime.now()
                );

        when(mockService.updateTask(eq(id), any(TaskUpdateRequest.class))).thenReturn(updatedTask);

        // WHEN & THEN
        mockMvc.perform(patch("/api/tasks/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(request.title()))
                .andExpect(jsonPath("$.description").value(updatedTask.description()))
                .andExpect(jsonPath("$.done").value(updatedTask.done()))
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    public void updateTask_ShouldReturnProblemDetail_WhenTaskDoesNotExist() throws Exception {
        // GIVEN
        long id = 1L;
        TaskUpdateRequest request = new TaskUpdateRequest("title", null, null);
        when(mockService.updateTask(eq(id), any(TaskUpdateRequest.class))).
                thenThrow(new ResourceNotFoundException(String.format("Task with id %d not found", id)));

        // WHEN & THEN
        mockMvc.perform(patch("/api/tasks/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.detail").value(String.format("Task with id %d not found", id)))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id));
    }

    @Test
    public void updateTask_ShouldReturnProblemDetail_WhenNewTitleIsAlreadyInUse() throws Exception {
        // GIVEN
        long id = 1L;
        TaskUpdateRequest request = new TaskUpdateRequest("Duplicate title", null, null);

        when(mockService.updateTask(eq(id), any(TaskUpdateRequest.class)))
                .thenThrow(new ResourceAlreadyExistsException("Can't update task with id " + id + ", title is already in use"));

        // WHEN & THEN
        mockMvc.perform(patch("/api/tasks/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id))
                .andExpect(jsonPath("$.detail").value("Can't update task with id " + id + ", title is already in use"));
    }

    @Test
    public void updateTask_ShouldReturnProblemDetail_WhenParametersAreInvalid() throws Exception {
        // GIVEN
        long id = 1L;
        TaskUpdateRequest request = new TaskUpdateRequest("", null, null);
        request.title();
        Map<String, String> errors = new HashMap<>();
        errors.put("title", "title must be between 1 and 255 characters");

        when(mockService.updateTask(eq(id),any(TaskUpdateRequest.class))).thenThrow(new ValidationException(errors));

        // WHEN & THEN
        mockMvc.perform(patch("/api/tasks/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id))
                .andExpect(jsonPath("$.detail").value("check 'errors' field"))
                .andExpect(jsonPath("$.errors.title")
                        .value("title must be between 1 and 255 characters"));
    }

    @Test
    public void updateTask_ShouldReturnProblemDetail_WhenJsonIsInvalid() throws Exception {
        // GIVEN
        long id = 1L;
        String invalidJson = "{\"done\": \"сосиска\"}";

        mockMvc.perform(patch("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.title").value("Malformed JSON"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    public void removeTask_ShouldReturnProblemDetail_WhenTaskDoesNotExist() throws Exception {
        // GIVEN
        long id = 1L;
        when(mockService.removeTaskById(eq(id))).thenThrow(new ResourceNotFoundException("Task with id " + id + " not found"));
        // WHEN & THEN
        mockMvc.perform(delete("/api/tasks/{id}", id)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.instance").value("/api/tasks/" + id))
                .andExpect(jsonPath("$.detail").value("Task with id " + id + " not found"));
    }

    @Test
    public void removeTask_ShouldReturnRemovedTask_WhenTaskExists() throws Exception {
        // GIVEN
        long id = 1L;
        TaskResponse response = new TaskResponse(
                id,
                "title",
                "description",
                true,
                LocalDateTime.now()
                );

        when(mockService.removeTaskById(eq(id))).thenReturn(response);
        // WHEN & THEN
        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());
    }
}