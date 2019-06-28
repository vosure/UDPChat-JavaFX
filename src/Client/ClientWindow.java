package Client;

import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientWindow implements Runnable, Initializable {
    public TextArea console;
    public TextField messageField;

    private Client client;

    private boolean running = false;
    Thread listen, run;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        client = new Client(Login.clientUsername, Login.serverAddress, 6263);

        if (!client.openConnection(Login.serverAddress)) {
            send("Connection Failed", true);
            return;
        }

        String connectionInfo = "/c/" + Login.clientUsername + "/e/";
        client.send(connectionInfo.getBytes());

        running = true;
        run = new Thread(this, "Running");
        run.start();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER){
            send(messageField.getText(), true);
        }
    }

    public void buttonClick() {
        send(messageField.getText(), true);
    }

    private void printIntoChat(String message){
        console.appendText(message + "\n");
    }

    private void send(String message, boolean broadcast){
        if (message.equals(""))
            return;
        if (broadcast) {
            message = "/m/" + client.getName() + ": " + message;
           messageField.setText("");
        }
        client.send(message.getBytes());
    }

    private void listen() {
        listen = new Thread("Listen") {
            public void run() {
                while(running) {
                    String message = client.receive();
                    if (message.startsWith("/c/")) {
                        client.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
                        printIntoChat("Successfully connected to server. ID: " + client.getID());
                    }
                    else if (message.startsWith("/m/")) {
                        String text = message.substring(3).split("/e/")[0];
                        printIntoChat(text);
                    }
                    else if (message.startsWith("/i/")) {
                        String text = "/i/" + client.getID() + "/e/";
                        send(text, false);
                    }
                }
            }
        };
        listen.start();
    }

    public void run() {
        listen();
    }

}
