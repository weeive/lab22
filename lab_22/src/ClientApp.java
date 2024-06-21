import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientApp extends JFrame {
    private final String url = "localhost";
    private final int port = 8188;

    private boolean authorized;
    private Socket socket;

    private JTextField msgInputField;

    private JTextField loginField;
    private JTextField passField;

    private JButton authButton;

    private JTextArea chatArea;

    private DataInputStream in;
    private DataOutputStream out;

    public ClientApp() {
        try {
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        prepareGUI();
    }

    public void closeConnection() {
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

    public boolean getAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public void openConnection() throws IOException {
        try {
            socket = new Socket(url, port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String strFromServer = in.readUTF();
                            if (strFromServer.startsWith("/authok")) {
                                loginField.setVisible(false);
                                passField.setVisible(false);
                                authButton.setVisible(false);
                                chatArea.append("Успешно авторизован\n");
                                setAuthorized(true);
                                break;
                            }
                            chatArea.append(strFromServer);
                            chatArea.append("\n");
                        }
                        while (true) {
                            String strFromServer = in.readUTF();
                            if (strFromServer.equalsIgnoreCase("/end")) {
                                break;
                            }
                            chatArea.append(strFromServer);
                            chatArea.append("\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onAuthClick() {
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.setText("");
            passField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMessage() {
        System.out.println(authorized);
        if (!msgInputField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(msgInputField.getText());
                msgInputField.setText("");
                msgInputField.grabFocus();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }

    public void prepareGUI() {
        // Параметры окна
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // Текстовое поле для вывода сообщений

        loginField = new JTextField();
        loginField.setToolTipText("Логин");
        passField = new JTextField();
        passField.setToolTipText("Пароль");
        JPanel topPanel = new JPanel(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);

        topPanel.add(loginField, BorderLayout.NORTH);
        topPanel.add(passField, BorderLayout.CENTER);

        authButton = new JButton("Авторизоваться");
        authButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAuthClick();
            }
        });

        topPanel.add(authButton, BorderLayout.SOUTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea),
                BorderLayout.CENTER);
        // Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton btnSendMsg = new JButton("Отправить");
        bottomPanel.add(btnSendMsg, BorderLayout.EAST);
        msgInputField = new JTextField();
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(msgInputField, BorderLayout.CENTER);
        btnSendMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        msgInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        // Настраиваем действие на закрытие окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF("/end");
                    closeConnection();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientApp();
            }
        });
    }

}
