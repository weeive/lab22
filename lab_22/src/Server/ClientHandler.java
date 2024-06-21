package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Statement;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String name;

    public String getName() {
        return name;
    }

    private void setName(String name) {
        Statement stmt = myServer.getAuthService().getStmt();
        String query = "update user set name='"+name+"' where name='" + this.name + "'";
        try {
            stmt.execute(query);
            this.name = name;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";

            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nick != null)
                    if (!myServer.isNickBusy(nick)) {
                        sendMsg("/authok " + nick);
                        name = nick;
                        myServer.broadcastMsg(name + " зашел в чат");
                        myServer.subscribe(this);
                        return;
                    } else sendMsg("Учетная запись уже исользуется");
            } else {
                sendMsg("Неверные логин/пароль");
            }
        }
    }

    public void readMessages() throws IOException {
        while (true) {
            String strFromClient = in.readUTF();
            if (strFromClient.startsWith("/w ")) {
                String[] arr = strFromClient.split(" ", 3);
                myServer.broadcastIndividual(arr[1], arr[2], name);
                System.out.println("от " + name + " кому " + arr[1] + ": " + arr[2]);
                continue;
            }
            if (strFromClient.startsWith("/changenick ")) {
                String[] arr = strFromClient.split(" ");
                String newName = arr[1];
                myServer.broadcastMsg(name + " changed name to " + newName);
                setName(arr[1]);
                continue;
            }
            if (name.equals("admin")) {
                if (strFromClient.startsWith("/adduser ")) {
                    String[] arr = strFromClient.split(" ");
                    String login = arr[1];
                    String pass = arr[2];
                    String nick = arr[3];
                    myServer.getAuthService().createUser(login, pass, nick);
                    myServer.broadcastIndividual(name, "user " + login + " added", "Server");
                    continue;
                }
                if (strFromClient.startsWith("/deluser ")) {
                    String[] arr = strFromClient.split(" ");
                    String login = arr[1];
                    boolean isInDB = myServer.getAuthService().findUserByNick(login);
                    boolean isOnline = myServer.isNickBusy(login);
                    if (isInDB) {
                        myServer.getAuthService().deleteUserByNick(login);
                        myServer.broadcastIndividual(name, "user " + login + " deleted", "Server");
                        if (isOnline) {
                            myServer.kickUser(login);
                        }
                    } else {
                        myServer.broadcastIndividual(name, "user " + login + " not found", "Server");
                    }
                    continue;
                }
            }
            System.out.println("от "+ name + ": " + strFromClient);
            if (strFromClient.equals("/end")) {
                return;
            }
            myServer.broadcastMsg(name + ": " + strFromClient);
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kick() {
        myServer.broadcastMsg(name + " был кикнут из чата");
        myServer.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
