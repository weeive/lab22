package Server;

import java.sql.Statement;

public interface AuthService {
    void start();

    boolean createUser (String login, String pass, String nick);
    boolean deleteUserByNick (String login);

    boolean findUserByNick (String nick);
    String getNickByLoginPass(String login, String pass);
    void stop();

    Statement getStmt();
}
