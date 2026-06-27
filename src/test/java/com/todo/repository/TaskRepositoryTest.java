package com.todo.repository;

import com.todo.model.TaskEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.*;
import java.time.LocalDateTime;
import java.util.List;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/schema.sql",  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
            .withDatabaseName("test_db").withUsername("test").withPassword("test");

    static {
        postgres.start();
    }

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final List<String> titles = List.of("ADD","BREAK","CLEAN","DELETE", "ESCAPE", "SLEEP",
            "EAT", "SWAP", "GRIND", "LEARN", "TEACH", "DREAM");


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeEach
    public void setUp() {
        for (int i = 0; i < titles.size(); i++) {
            TaskEntity task = new TaskEntity();
            task.setTitle(titles.get(i));
            task.setDescription("Test");
            task.setDone(false);
            entityManager.persist(task);
        }
        entityManager.flush();
    }

    @Test
    @Order(1)
    public void findNextPage_ShouldReturnFirstPage_WhenLastIdIsNull() {
        int pageSize = 5;
        int noOffset = 0;
        var firstPage = taskRepository.findNextPage(null, PageRequest.of(noOffset, pageSize));
        List<Long> allIds = taskRepository.findAll(Sort.by("id")).stream().
                map(TaskEntity::getId).toList();

        assertThat(firstPage).hasSize(pageSize).extracting(TaskEntity::getId).
                containsExactlyElementsOf(allIds.subList(0, pageSize));
    }

    @Test
    @Order(2)
    public void findNextPage_ShouldReturnNextPage_WhenLastIdIsNotNull() {
        int pageSize = 5;
        int pageNumber = 2;
        int noOffset = 0;

        List<Long> allIds = taskRepository.findAll(Sort.by("id")).stream().
                map(TaskEntity::getId).toList();

        Long lastId = allIds.get(pageSize * (pageNumber - 1) - 1);

        var nextPage = taskRepository.findNextPage(lastId, PageRequest.of(noOffset, pageSize));

        assertThat(nextPage).hasSize(pageSize).extracting(TaskEntity::getId).
                containsExactlyElementsOf(allIds.subList(pageSize * (pageNumber - 1), pageSize * pageNumber));
    }

    @Test
    @Order(3)
    public void findNextPage_ShouldReturnLastPage_WhenLastIdIsNotNull() {
        int pageSize = 5;
        int noOffset = 0;

        List<Long> allIds = taskRepository.findAll(Sort.by("id")).stream().
                map(TaskEntity::getId).toList();

        int tasksCount = allIds.size();
        int fullPageCount = tasksCount /  pageSize;
        int lastIncompletePageSize = tasksCount % pageSize;

        boolean lastPageIsFull = lastIncompletePageSize == 0;

        int lastPageSize = lastPageIsFull ? pageSize :  lastIncompletePageSize;

        int lastIdIndex = lastPageIsFull ? (fullPageCount - 1) * pageSize - 1 : fullPageCount * pageSize - 1;
        long lastId = allIds.get(lastIdIndex);

        List<Long> tasksForComparing = allIds.subList(lastIdIndex + 1, allIds.size());

        var lastPage = taskRepository.findNextPage(lastId, PageRequest.of(noOffset, pageSize));
        assertThat(lastPage).hasSize(lastPageSize).extracting(TaskEntity::getId).
                containsExactlyElementsOf(tasksForComparing);
    }
}
