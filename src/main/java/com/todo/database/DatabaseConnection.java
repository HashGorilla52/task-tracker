package com.todo.database;

import java.sql.*;
import java.util.Properties;
import java.io.*;

/**
 * Utility класс для соединения с БД
 */
public class DatabaseConnection {

    private static String URL;
    private static String USER;
    private static String PASSWORD;

    public static String getURL() {
        if (!hasConfig()) {
            loadConfig();
        }
        return URL;
    }

    public static String getUSER() {
        if (!hasConfig()) {
            loadConfig();
        }
        return USER;
    }

    public static String getPASSWORD() {
        if (!hasConfig()) {
            loadConfig();
        }
        return PASSWORD;
    }

    private DatabaseConnection(){}

    public static Connection getConnection() throws SQLException {

        if (!hasConfig()) {
            loadConfig();
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static boolean hasConfig() {
        return URL != null || USER != null || PASSWORD != null;
    }

    private static void loadConfig() {

        try (InputStream propsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
            if (propsStream == null) {
                throw new RuntimeException("application.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(propsStream);

            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}