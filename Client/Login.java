package Client.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class Login {
    public TextField username;
    public TextField address;

    public static String clientUsername;
    public static String serverAddress;

    public void login(ActionEvent actionEvent) {
        clientUsername = username.getText();
        serverAddress = address.getText();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Client/views/ClientWindow.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
