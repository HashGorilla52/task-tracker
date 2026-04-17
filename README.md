# Task Tracker

Консольное приложение для управления задачами.

## Технологии
- Java 21
- Spring Boot
- JPA Hibernate
- Maven
- PostgreSQL

## Запуск

1. Убедитесь, что PostgreSQL запущен, и создана база `task_tracker`
2. Скопируйте `application.properties.example` в `application.properties` и укажите свои пароль/пользователя
3. Выполните в терминале:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.todo.app.Main"

## Функции
- Добавление задач
- Редактирование
- Удаление
- Просмотр всех задач
