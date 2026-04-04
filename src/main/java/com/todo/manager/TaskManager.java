package com.todo.manager;

import com.todo.dto.TaskUpdateRequest;
import com.todo.model.BaseTask;
import com.todo.model.Task;
import com.todo.database.DatabaseConnection;
import com.todo.exception.DatabaseException;
import java.util.List;
import java.util.ArrayList;
import java.sql.*;

public class TaskManager {

    /**
     * Метод для получения количества сохранённых в базе данных задач.
     * @return количество задач пользователя.
     */
    public int getAllTasksCount() {
        String sql = "SELECT COUNT(*) FROM tasks";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Database Error while receiving tasks: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Добавляет задачу в БД.
     * @param title название;
     * @param description описание;
     * @param isDone статус завершённости.
     * @return Задачу, созданный объект Task.
     */
    public Task addTask(String title, String description, boolean isDone) {

        String sql = "INSERT INTO tasks (title, description, is_done) VALUES(?, ?, ?) RETURNING id, created_at";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setBoolean(3, isDone);

            int affectedRows =  stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating task failed, no rows affected");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Timestamp time = generatedKeys.getTimestamp("created_at");
                    return new BaseTask(
                            generatedKeys.getLong("id"),
                            title,
                            description,
                            isDone,
                            generatedKeys.getTimestamp("created_at").toLocalDateTime()
                    );
                }
                else {
                    throw new SQLException("No generated keys returned");
                }
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Database Error while adding task: " + e.getMessage());
        }
    }

    /**
     * Метод, возвращающий объект Task по id, если такой объект существует.
     * @param id
     * @return
     */
    public Task getTask(long id) {

        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                // если что-то вернулось в ResultSet - маппим в BaseTask и возвращаем.
                if (rs.next()) {
                    return new BaseTask(
                        id,
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBoolean("is_done"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
                else {
                    throw new IllegalArgumentException("No tasks found by id");
                }
            }
        }
        catch (SQLException e) {
           throw new DatabaseException("Database Error while receiving task: " + e.getMessage());
        }
    }

    /**
     * Метод, удалющий объект Task из БД.
     * @param id
     * @return удалённый объект Task.
     */
    public Task removeTask(long id) {

        String sql = "DELETE FROM tasks WHERE id = ? RETURNING title, description, is_done, created_at";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) { throw new IllegalArgumentException("No tasks removed by id");}

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new BaseTask(
                            id,
                            generatedKeys.getString("title"),
                            generatedKeys.getString("description"),
                            generatedKeys.getBoolean("is_done"),
                            generatedKeys.getTimestamp("created_at").toLocalDateTime()
                    );
                }
                else {
                    throw new SQLException("No generated keys returned");
                }
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Database Error while removing task: " + e.getMessage());
        }
    }

    /**
     * Возвращает список всех сохранённых в БД задач.
     * @return List список с задачами.
     */
    public List<Task> getAllTasks() {

        String sql = "SELECT * FROM tasks";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tasks.add(new BaseTask(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBoolean("is_done"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Receiving tasks failed: " + e.getMessage());
        }
        return tasks;
    }

    /**
     * Метод, обновляющий задачу в зависимости от переданного параметром DTO объекта TaskUpdateRequest.
     * @param id
     * @param request
     * @return
     */
    public Task updateTask(long id, TaskUpdateRequest request)
    {
        if (request == null) {throw new IllegalArgumentException("Request object must exist");}

        // Списки для частей sql запроса и значений, которые будут в него подставляться.
        List<String> sqlParts = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        // Вытягиваем из DTO значения для внесения изменений в бд.
        if (request.getTitle() != null) {
            sqlParts.add("title = ?");
            values.add(request.getTitle());
        }
        if (request.getDescription() != null) {
            sqlParts.add("description = ?");
            values.add(request.getDescription());
        }
        if (request.getDone() != null) {
            sqlParts.add("is_done = ?");
            values.add(request.getDone());
        }

        String sql = "UPDATE tasks SET " + String.join(", ", sqlParts) + " WHERE id = ? RETURNING *";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.setLong(values.size() + 1, id);

            int affectedRows =  stmt.executeUpdate();
            if (affectedRows == 0) { throw new IllegalArgumentException("No tasks found by id"); }

            try(ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new BaseTask(
                            generatedKeys.getLong("id"),
                            generatedKeys.getString("title"),
                            generatedKeys.getString("description"),
                            generatedKeys.getBoolean("is_done"),
                            generatedKeys.getTimestamp("created_at").toLocalDateTime()
                    );
                }
                else {
                    throw new SQLException("No generated keys returned");
                }
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Database Error while updating task: " + e.getMessage());
        }
    }
}