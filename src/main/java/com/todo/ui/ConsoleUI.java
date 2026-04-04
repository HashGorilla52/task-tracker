package com.todo.ui;

import com.todo.dto.TaskUpdateRequest;
import com.todo.exception.DatabaseException;
import com.todo.model.Task;
import com.todo.manager.TaskManager;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

public class ConsoleUI {

    /**
     * Служит для определения состояния программы,
     * ассоциирует пользовательский ввод с операцией главного меню. Порядок имеет строгое значение для соответствия с пунктами меню.
     */
    private enum MenuOperations { ADD_TASK, UPDATE_TASK, REMOVE_TASK, SHOW_ALL_TASKS, EXIT }

    private final TaskManager manager; // через него происходит работа с данными (задачами)
    private final Scanner in = new Scanner(System.in);

    public ConsoleUI(TaskManager manager)
    {
        this.manager = manager;
    }

    /**
     * Запускает пользовательский интерфейс.
     */
    public void start()
    {
        boolean isBeingUsed = true;

        System.out.println("Добро пожаловать в ваш To-Do List!\n");

        while (isBeingUsed)
        {
            printMainMenu();

            // Считываем пользовательский целочисленный ввод и возвращаем элемент из перечисления,
            // соответствующий данной позиции.
            MenuOperations operation = getUserChoice(MenuOperations.values());

            try {
                switch (operation) {
                    case ADD_TASK:
                        addTask();
                        break;

                    case UPDATE_TASK:
                        updateTask();
                        break;

                    case REMOVE_TASK:
                        removeTask();
                        break;

                    case SHOW_ALL_TASKS:
                        showAllTasks();
                        break;

                    case EXIT:
                        System.out.println("\nДо свидания!\n");
                        isBeingUsed = false;
                        break;

                    case null:
                        System.out.println("\nВы не выбрали операцию, повторите ввод.\n");
                        break;
                }
            }
            catch (IllegalArgumentException e) {
                System.out.println("Ошибка ввода: " + e.getMessage());
                e.printStackTrace();
            }
            catch (DatabaseException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Инициирует добавление задачи.
     */
    private void addTask() throws DatabaseException
    {
        String title, description = "";
        boolean isDone;

        // Номер элемента в перечислении соответствует номеру меню.
        enum AddTaskNewOrFinished{NEW, DONE, CANCEL} // Выбор статуса создаваемой задачи
        enum NeedDescription{CREATE, IGNORE, BACK, CANCEL} // Выбор, создавать ли описание, вернуться назад или выйти из создания.

        boolean taskIsCreating = true;

        // Начинаем создавать задачу.
        while (taskIsCreating)
        {
            System.out.println("\nЧто хотите сделать?\n1. Добавить новую задачу\n" +
                    "2. Добавить выполненную задачу\n3. Отменить добавление задачи\n");

            // Определяем операцию, нужную пользователю.
            AddTaskNewOrFinished TaskStatusChoice = getUserChoice(AddTaskNewOrFinished.values());

            // Выход из создания задачи.
            if (TaskStatusChoice == AddTaskNewOrFinished.CANCEL) {return;}

            isDone = TaskStatusChoice != AddTaskNewOrFinished.NEW;

            System.out.printf("\nКак %s задача?\n\n", isDone ? "называется выполненная"  :
                    "будет называться новая");

            // Пользователь вводит название задачи.
            title = getUserInput(false);
            System.out.println("\nНазвание добавлено!\n");

            boolean descriptionIsCreating = true;

            while (descriptionIsCreating)
            {
                System.out.println("Хотите добавить к задаче описание?\n" +
                        "1. Добавить описание \n2. Пропустить описание\n3. Вернуться назад\n4. Отменить добавление задачи\n");

                NeedDescription needDescriptionChoice = getUserChoice(NeedDescription.values());

                switch (needDescriptionChoice)
                {
                    case CREATE: // Создаём описание и заканичиваем создание задачи.
                        System.out.println("\nВведите описание задачи:\n");
                        description = getUserInput(true);
                        System.out.println("\nОписание добавлено!\n");
                        descriptionIsCreating = false;
                        taskIsCreating = false;
                        // На основе введённых данных создаём задачу и добавляем её в БД.
                        manager.addTask(title, description, isDone);
                        System.out.println("\nЗадача успешно добавлена!\n");
                        break;

                    case IGNORE: // Не создаём описание и заканичиваем создание задачи.
                        descriptionIsCreating = false;
                        taskIsCreating = false;
                        // На основе введённых данных создаём задачу и добавляем её в БД.
                        manager.addTask(title, description, isDone);
                        System.out.println("\nЗадача успешно добавлена!\n");
                        break;

                    case BACK: // Возвращаемся к выбору статуса создаваемой задачи и вводу названия.
                        descriptionIsCreating = false;
                        break;

                    case CANCEL: // Выходим из создания задачи и ничего не создаём.
                        return;
                }
            }
        }
    }

    /**
     * Инициирует редактирование полей задачи.
     */
    private void updateTask() throws IllegalArgumentException, DatabaseException
    {
        // Спрашиваем пользователя, нужно ли показывать список всех задач.
        makeSureToShowAllTasks();
        System.out.println("\nУкажите номер задачи для редактирования:\n");
        long taskId = getUserChoice(manager.getAllTasksCount());
        // Создаём объект, ссылающийся на редактируемую задачу.
        Task updatingTask = manager.getTask(taskId);
        // Создаём локальную копию задачи на которой будем показывать изменения.
        Task exampleTask = updatingTask.copy();


        enum EditingFields { TITLE, DESCRIPTION, DONE }

        enum UpdatingActions { EDIT, SAVE, RESET, CANCEL }

        // DTO объект для заполнения полей для изменения.
        TaskUpdateRequest request = new TaskUpdateRequest();
        // Начинаем редактирование.
        boolean isEditing = true;
        while (isEditing)
        {
            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\nВаша задача:");
            printTask(exampleTask);
            System.out.println("\nВыберите действие:\n1) Изменить задачу\n2) Сохранить задачу\n"
                    + "3) Сбросить изменения\n4) Отменить редактирование\n");

            UpdatingActions updateAction = getUserChoice(UpdatingActions.values());

            switch (updateAction)
            {
                case EDIT:
                    System.out.println("Выберите поле для редактирования:");
                    System.out.println("1. Название\n2. Описание\n3. Статус выполнения\n");
                    EditingFields field = getUserChoice(EditingFields.values());

                    switch(field)
                    {
                        case TITLE:
                            System.out.println("\nВведите новое название:\n");
                            String newTitle = getUserInput(false);
                            request.setTitle(newTitle);
                            exampleTask.updateTitle(request.getTitle());
                            break;

                        case DESCRIPTION:
                            System.out.println("\nВведите новое описание (если хотите удалить текущее - оставьте пустым):\n");
                            String newDescription = getUserInput(true);
                            request.setDescription(newDescription);
                            exampleTask.updateDescription(request.getDescription());
                            break;

                        case DONE:
                            System.out.println("\nВыберите статус выполнения задачи:\n1) Выполнено\n2) Не выполнено"
                                    + "\n");
                            int choice = getUserChoice(2);
                            boolean done = choice == 1;
                            request.setDone(done);
                            if (done) {exampleTask.markAsDone();}
                            else {exampleTask.markAsUndone();}
                            break;
                    }
                    break;

                case SAVE:
                    manager.updateTask(taskId, request);
                    System.out.println("\nЗадача успешно изменена!\n");
                    isEditing = false;
                    break;

                case RESET:
                    exampleTask = updatingTask.copy();
                    request = new TaskUpdateRequest();
                    System.out.println("\nТекущие изменения сброшены.\n");
                    break;

                case CANCEL:
                    isEditing = false;
                    break;
            }
        }
    }

    /**
     * Инициирует удаление задачи.
     */
    private void removeTask() throws IllegalArgumentException, DatabaseException
    {
        makeSureToShowAllTasks();

        System.out.println("\nУкажите номер задачи для удаления:\n");
        int taskId = getUserChoice(manager.getAllTasksCount());
        manager.removeTask(taskId);
        System.out.println("Задача успешно удалена!\n");
    }

    /**
     * Выводит пользователю вопрос и при необходимости показывает список всех имеющихся у него задач.
     */
    private void makeSureToShowAllTasks() throws DatabaseException
    {
        System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\n" +
                "Показать список ваших задач?\n1. Показать\n" +
                "2. Не нужно показывать\n");
        boolean needAllTasksView = switch(getUserChoice(2))
        {
            case 1 -> true;
            case 2 -> false;
            default -> true;
        };

        if (needAllTasksView){showAllTasks();}
    }

    private void showAllTasks() throws DatabaseException
    {
        if (manager.getAllTasksCount() == 0)
        {
            System.out.println("\nВы пока что не добавили ни одной задачи.\n");
        }
        else
        {
            ArrayList<Task> allTasks =new ArrayList<>(manager.getAllTasks());

            System.out.println("\nСписок ваших задач:\n");

            for (int i = 0; i < allTasks.size(); i++)
            {
                System.out.printf("Задача %d\n", i + 1);
                printTask(allTasks.get(i));
                System.out.println();
            }
        }
    }

    private String getUserInput(boolean emptyInputIsAllowed)
    {
        while (true)
        {
            if (in.hasNextLine())
            {
                String input = in.nextLine();

                if (emptyInputIsAllowed || !input.isBlank()){return input;}

                System.out.println("Ввод не должен быть пустым!\n");
            }
        }
    }

    /**
     * Инициирует пользовательский ввод для целого числа,
     * ограниченного переданым числом (количеством операций для выбора),
     * и возвращает это целое число
     * @param variations кол-во операций для выбора
     * @return выбранное пользователем число, соответствующее некоторой операции.
     */
    private int getUserChoice(int variations)
    {
        while (true)
        {
            if (in.hasNextInt())
            {
                int number = in.nextInt();
                in.nextLine();

                if (number >= 1 && number <= variations){return number;}

                System.out.printf("Введите число от 1 до %d!\n", variations);
            }
            else
            {
                System.out.println("Некорректный ввод! Введите целое число!\n");
                in.nextLine();
            }
        }
    }

    /**
     * Инициирует пользовательский ввод для целого числа,
     * ограниченного переданым числом (количеством элементов перечисления),
     * и возвращает элемент переданного массива элементов перечисления, соответствующий введённому порядковому номеру.
     * @param values массив значений enum.
     * @return выбранное пользователем число, соответствующее некоторой операции.
     */
    private <T extends Enum<T>> T getUserChoice(T[] values)
    {
        while (true)
        {
            if (in.hasNextInt())
            {
                int number = in.nextInt();
                in.nextLine();

                if (number >= 1 && number <= values.length){return values[number - 1];}

                System.out.printf("Введите число от 1 до %d!\n", values.length);
            }
            else
            {
                System.out.println("Некорректный ввод! Введите целое число!\n");
                in.nextLine();
            }
        }
    }


    /**
     * Выводит текстовое представление команд главного меню.
     */
    private void printMainMenu()
    {
        System.out.println("Выберите операцию:\n"
                + "1. Добавить задачу\n2. Редактировать задачу\n3. Удалить задачу\n"
                + "4. Посмотреть список задач\n"
                + "5. Выйти из программы\n");
    }

    /**
     * Выводит данные о задаче на в консоль.
     *
     * @param task задача для вывода
     */
    private void printTask(Task task)
    {
        System.out.printf("Название: %s\n" +
                        "Описание: %s\n" +
                        "Дата создания: %s\n" +
                        "Готовность: %s\n",
                task.getTitle(),
                task.hasDescription() ? task.getDescription() : "Отсутствует",
                task.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                task.isDone() ? "выполнено" : "не выполнено");
    }
}