package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BaseAuthService implements AuthService {
    final static String connectionUrl = "jdbc:sqlite:main.sqlite";
    private static Connection connection;
    private static Statement stmt;

    @Override
    public void start() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e ) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(connectionUrl);
            stmt = connection.createStatement();
            System.out.println("Connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Сервис аутентификации запущен");
    }

    @Override
    public void stop() {
        System.out.println("Сервис аутентификации остановлен");
    }

    @Override
    public synchronized boolean createUser (String login, String pass, String nick) {
        try {
            String query = "INSERT into user (login, password, name) VALUES ('"+login+"', '"+pass+"', '"+nick+"')";
            stmt.execute(query);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean deleteUserByNick (String login) {
        try {
            String query = "DELETE from user where name='"+login+"'";
            stmt.execute(query);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean findUserByNick (String nick) {
        String nickname = "";
        try {
            String query = "select * from user where name='"+nick+"'";
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                nickname = rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return !nickname.equals("");
    }

    @Override
    public synchronized String getNickByLoginPass (String login, String pass) {
        String nickReturn = null;
        try {
            String query = "select * from user where login='" + login + "'and password='" + pass + "'";
            System.out.println(query);
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                nickReturn = rs.getString("name");
                System.out.println(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (nickReturn.equals(" "))
            return null;
        return nickReturn;
    }

    public Statement getStmt () {
        return stmt;
    }
}
