package Server.Controllers;

import Server.ServerClient;
import Server.UniqueID;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ServerController implements Runnable, Initializable {
    public TextField messageField;
    public TextArea chatLog;

    private ArrayList<ServerClient> clients = new ArrayList<>();
    private ArrayList<Integer> clientResponse = new ArrayList<>();

    private boolean running = false;

    private Thread run, manage, recieve, send;

    private DatagramSocket socket;

    private static final int MAX_ATTEMPTS = 5;

    private boolean printMessages = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int port = 6263;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        run = new Thread(this, "Server");
        run.start();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER){
            print();
        }

    }

    public void buttonClick(ActionEvent actionEvent) {
        print();
    }


    private void print() {
        String text = messageField.getText();
        messageField.clear();
        if (!text.startsWith("/")) {
            sendToAll("/m/Server: " + text + "/e/");
            return;
        }
        text = text.substring(1);
        if (text.equals("printMessages")) {
            printMessages = !printMessages;
        } else if (text.equals("clients")) {
            chatLog.appendText("Online Clients : \n\r");
            ServerClient client;
            for (int i = 0; i < clients.size(); i++) {
                client = clients.get(i);
                // System.out.println(client.name + " (" + client.getID() + ") " + client.address + " " + client.port);
                chatLog.appendText(client.name + " (" + client.getID() + ") " + client.address + " " + client.port + "\n\r");
            }
        } else if (text.startsWith("kick")) {
            String name = text.split(" ")[1];
            int ID = -1;
            boolean isNumber = false;
            try {
                ID = Integer.parseInt(name);
                isNumber = true;
            } catch (NumberFormatException e) {
            }
            if (isNumber) {
                boolean isExist = false;
                for (int i = 0; i < clients.size(); i++) {
                    if (clients.get(i).getID() == ID) {
                        isExist = true;
                        break;
                    }
                }
                if (isExist) {
                    disconnect(ID, true);
                } else
                    chatLog.appendText("Client with ID - " + ID + " does not exist" + "\n\r");

            } else {
                for (int i = 0; i < clients.size(); i++) {
                    ServerClient client = clients.get(i);
                    if (name.equals(client.name)) {
                        disconnect(client.getID(), true);
                    }
                }
            }
        } else {
            chatLog.appendText("Unknown command");
        }
    }

    public void run() {
        running = true;
        chatLog.appendText("Server running on port: " + 6263 + "\n\r");
        manage();
        receive();
    }

    public void manage() {
        manage = new Thread("Manage") {
            public void run() {
                while (running) {
                    sendToAll("/i/server");
                    sendStatus();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < clients.size(); i++) {
                        ServerClient client1 = clients.get(i);
                        if (!clientResponse.contains(client1.getID())) {
                            if (client1.attempt >= MAX_ATTEMPTS) {
                                disconnect(client1.getID(), false);
                            } else {
                                client1.attempt++;
                            }
                        } else {
                            clientResponse.remove(new Integer(client1.getID()));
                            client1.attempt = 0;
                        }
                    }
                }
            }
        };
        manage.start();
    }

    private void sendStatus() {
        if (clients.size() <= 0) return;
        String users = "/u/";
        for (int i = 0; i < clients.size() - 1; i++) {
            users += clients.get(i).name;
        }
        users += clients.get(clients.size() - 1).name + "/e/";
    }

    public void receive() {
        recieve = new Thread("Receive") {
            public void run() {
                while (running) {
                    byte[] data = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    processMessage(packet);
                }

            }
        };
        recieve.start();
    }

    private void send(byte[] data, InetAddress address, int port) {
        send = new Thread("Send") {
            public void run() {
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        send.start();
    }

    private void sendToAll(String message) {
        if (message.startsWith("/m/")) {
            String text = message.substring(3);
            text = text.split("/e/")[0];
            chatLog.appendText(text + "\n\r");
        }

        for (int i = 0; i < clients.size(); i++) {
            ServerClient client = clients.get(i);
            send(message.getBytes(), client.address, client.port);
        }

    }

    private void processMessage(DatagramPacket packet) {
        String string = new String(packet.getData()).trim();
        if (printMessages)
            chatLog.appendText(string + "\n\r");
        if (string.startsWith("/c/")) {
            String name = string.split("/c/|/e/")[1];
            chatLog.appendText(name + " connected!" + "\n\r");
            int id = UniqueID.getIdentifier();
            clients.add(new ServerClient(name, packet.getAddress(), packet.getPort(), id));
            String ID = "/c/" + id + "/e/";
            send(ID.getBytes(), packet.getAddress(), packet.getPort());
        } else if (string.startsWith("/m/")) {
            sendToAll(string);
        } else if (string.startsWith("/d/")) {
            disconnect(Integer.parseInt(string.split("/d/|/e/")[1]), true);
        } else if (string.startsWith("/i/")) {
            clientResponse.add(Integer.parseInt(string.split("/i/|/e/")[1]));
        } else {

        }
    }

    private void disconnect(int ID, boolean status) {
        ServerClient client = null;
        boolean exists = false;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getID() == ID) {
                client = clients.get(i);
                clients.remove(i);
                exists = true;
                break;
            }
        }
        if (!exists)
            return;
        if (status)
            chatLog.appendText("Client " + client.name + " disconnected\n\r");
        else
            chatLog.appendText("Client " + client.name + " timed out\n\r");
    }
}
