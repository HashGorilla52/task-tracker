package com.todo.app;

import com.todo.manager.TaskManager;
import com.todo.model.BaseTask;
import com.todo.model.Task;
import com.todo.ui.ConsoleUI;

public class Main {

    public static void main(String[] args) {

        TaskManager taskManager = new TaskManager();

        ConsoleUI consoleProcessor = new ConsoleUI(taskManager);

        consoleProcessor.start();
    }
}