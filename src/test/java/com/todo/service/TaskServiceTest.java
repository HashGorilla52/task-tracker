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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository mockRepository;

    @InjectMocks
    private TaskService service;

    @Test
    public void createTask_ShouldReturnTask_WhenDataIsValid()
    {
        // GIVEN
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Title");
        request.setDescription("Description");
        request.setDone(true);

        long id = 1;
        TaskEntity savedEntity = new TaskEntity();
        savedEntity.setId(id);
        savedEntity.setTitle(request.getTitle());
        savedEntity.setDescription(request.getDescription());
        savedEntity.setDone(request.getDone());

        when(mockRepository.save(any(TaskEntity.class))).thenReturn(savedEntity);

        // WHEN
        TaskResponse result = service.createTask(request);

        // THEN
        assertThat(result.getId()).isEqualTo(savedEntity.getId());
        assertThat(result.getTitle()).isEqualTo(request.getTitle());
        assertThat(result.getDescription()).isEqualTo(request.getDescription());
        assertThat(result.isDone()).isEqualTo(request.getDone());
        assertThat(result.getCreatedAt()).isEqualTo(savedEntity.getCreatedAt());

        verify(mockRepository).save(any(TaskEntity.class));
    }

    @Test
    public void createTask_ShouldThrowException_WhenTaskAlreadyExists()
    {
        // GIVEN
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Duplicate Title");
        request.setDescription("Description");
        request.setDone(true);

        when(mockRepository.save(any(TaskEntity.class))).thenThrow(new DataIntegrityViolationException("title already in use"));

        // WHEN & THEN
        assertThatThrownBy(() -> service.createTask(request)).isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    public void getTaskById_ShouldReturnTask_WhenTaskExists(){
        // GIVEN
        long id = 1;
        TaskEntity task =  new TaskEntity();
        task.setId(1L);
        when(mockRepository.findById(id)).thenReturn(Optional.of(task));

        // WHEN
        TaskResponse result = service.getTaskById(id);

        // THEN
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    public void getTaskById_ShouldThrowException_WhenTaskDoesNotExist(){
        // GIVEN
        long id = 999L;
        when(mockRepository.findById(id)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> service.getTaskById(id)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task with id " + id + " not found");
    }

    @Nested
    class PaginationTests{

        private List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        private List<TaskEntity> tasks;
        int noOffset = 0;
        int limit = 5;

        @BeforeEach
        void setUp(){
            this.tasks = ids.stream()
                    .map(id -> {
                        TaskEntity task = new TaskEntity();
                        task.setId(id);
                        return task;
                    }).toList();
        }

        @Test
        public void getTasksPage_ShouldReturnPage_WhenTasksExist(){
            //GIVEN
            int pageNumber = 1;
            int pageSize = 5;
            int totalPages = 2;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageSize, tasks.size());
            List<TaskEntity> lastPage = tasks.subList(start, end);

            Page<TaskEntity> page = new PageImpl<>(lastPage, pageable, pageSize);
            when(mockRepository.findAll(pageable)).thenReturn(page);

            // WHEN
            Page<TaskResponse> result = service.getTasksPage(pageable);

            // THEN
            assertThat(result.getContent()).hasSize(pageSize);
            assertThat(result.getTotalPages()).isEqualTo(totalPages);
            assertThat(result.getContent()).extracting(TaskResponse::getId).containsExactlyElementsOf(lastPage
                    .stream().map(TaskEntity::getId).collect(Collectors.toList()));
        }

        @Test
        public void getTasksPage_ShouldReturnEmptyPage_WhenPageIsMissing(){
            //GIVEN
            int pageNumber = 999; // несуществующая страница
            int pageSize = 10;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            when(mockRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

            // WHEN
            Page<TaskResponse> result = service.getTasksPage(pageable);

            // THEN
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        public void getTasksWithCursor_ShouldReturnTasksWithCursor_WhenCursorIsNull(){
            //GIVEN
            Pageable pageable = PageRequest.of(noOffset, limit);
            Long lastId = null;

            List<TaskEntity> mockPage = tasks.subList(0, limit + 1);
            List<TaskEntity> expectedPage = mockPage.subList(0, limit);

            Long nextCursor = expectedPage.get(expectedPage.size() - 1).getId();
            Pageable mockPageable = PageRequest.of(noOffset, limit + 1);

            when(mockRepository.findNextPage(lastId, mockPageable)).thenReturn(mockPage);

            // WHEN
            TaskCursorPage result = service.getTasksWithCursor(lastId, pageable);

            // THEN
            assertThat(result.getTasks()).hasSize(limit);
            assertThat(result.getTasks()).extracting(TaskResponse::getId)
                    .containsExactlyElementsOf(expectedPage.stream().map(TaskEntity::getId).collect(Collectors.toList()));
            assertThat(result.getNextCursor()).isEqualTo(nextCursor);

        }

        @Test
        public void getTasksWithCursor_ShouldReturnTasksWithCursor_WhenCursorIsNotNull(){
            //GIVEN
            Pageable pageable = PageRequest.of(noOffset, limit);
            Long lastId = 5L;
            int lastIdIndex = ids.indexOf(lastId);
            List<TaskEntity> expectedPage = tasks.subList(lastIdIndex + 1, lastIdIndex + limit + 1);

            Pageable mockPageable = PageRequest.of(noOffset, limit + 1);
            when(mockRepository.findNextPage(lastId, mockPageable)).thenReturn(expectedPage);

            // WHEN
            TaskCursorPage result = service.getTasksWithCursor(lastId, pageable);

            // THEN
            assertThat(result.getTasks()).hasSize(limit);
            assertThat(result.getTasks()).extracting(TaskResponse::getId)
                    .containsExactlyElementsOf(expectedPage.stream().map(TaskEntity::getId).collect(Collectors.toList()));
            assertThat(result.getNextCursor()).isEqualTo(null);
        }

        @Test
        public void getTasksWithCursor_ShouldReturnEmptyList_WhenLastIdIsMoreThanLatest(){
            //GIVEN
            Pageable pageable = PageRequest.of(noOffset, limit);
            Long lastId = 999L;

            List<TaskEntity> mockPage = new ArrayList<>();

            Pageable mockPageable = PageRequest.of(noOffset, limit + 1);

            when(mockRepository.findNextPage(lastId, mockPageable)).thenReturn(mockPage);

            // WHEN
            TaskCursorPage result = service.getTasksWithCursor(lastId, pageable);

            // THEN
            assertThat(result.getTasks()).isEmpty();
            assertThat(result.getNextCursor()).isEqualTo(null);
        }
    }

    @Test
    public void updateTask_ShouldThrowException_WhenTaskDoesNotExist(){
        // GIVEN
        long id = 999L;
        TaskUpdateRequest request = new TaskUpdateRequest();

        when(mockRepository.findById(id)).thenThrow(new ResourceNotFoundException("Task with id " + id + " not found"));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateTask(id, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void updateTask_ShouldReturnUpdatedTask_WhenTaskExistsAndRequestParametersAreValid(){
        // GIVEN
        long id = 1L;
        String newTitle = "title";
        String newDescription = "description";
        boolean newDone = true;

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle(newTitle);
        request.setDescription(newDescription);
        request.setDone(newDone);

        TaskEntity task = new TaskEntity();
        task.setId(id);

        TaskEntity updatedTask = new TaskEntity();
        updatedTask.setId(id);
        updatedTask.setTitle(newTitle);
        updatedTask.setDescription(newDescription);
        updatedTask.setDone(newDone);

        when(mockRepository.findById(id)).thenReturn(Optional.of(task));
        when(mockRepository.save(task)).thenReturn(updatedTask);

        //WHEN
        TaskResponse result = service.updateTask(id, request);

        // THEN
        assertThat(result.getTitle()).isEqualTo(updatedTask.getTitle());
        assertThat(result.getDescription()).isEqualTo(updatedTask.getDescription());
        assertThat(result.isDone()).isEqualTo(updatedTask.isDone());
    }

    @Test
    public void updateTask_ShouldThrowException_WhenTaskExistsButNewTitleIsAlreadyInUse(){
        // GIVEN
        long id = 1L;
        TaskEntity task = new TaskEntity();
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("duplicate title");
        when(mockRepository.findById(id)).thenReturn(Optional.of(task));
        when(mockRepository.save(task)).thenThrow(DataIntegrityViolationException.class);

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateTask(id, request)).isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    public void updateTask_ShouldThrowException_WhenTaskExistsButRequestParametersAreInvalid(){
        // Given
        long id = 1L;
        TaskEntity task = new TaskEntity();
        task.setId(id);
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle(""); // передаём некорректное название задачи для обновления, ожидается ValidationException
        when(mockRepository.findById(id)).thenReturn(Optional.of(task));

        // WHEN & THEN
        assertThatThrownBy(() -> service.updateTask(id, request)).isInstanceOf(ValidationException.class);
    }

    @Test
    public void removeTask_ShouldThrowException_WhenTaskDoesNotExist(){
        // GIVEN
        long id = 1L;
        when(mockRepository.findById(id)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> service.removeTaskById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void removeTask_ShouldReturnRemovedTask_WhenTaskExists(){
        // GIVEN
        long id = 1L;
        TaskEntity task = new TaskEntity();
        task.setId(id);
        when(mockRepository.findById(id)).thenReturn(Optional.of(task));

        // WHEN & THEN
        assertThat(service.removeTaskById(id)).extracting(TaskResponse::getId).isEqualTo(id);
        verify(mockRepository).delete(task);
    }
}
