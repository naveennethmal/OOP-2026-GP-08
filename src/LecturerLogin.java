import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LecturerLogin extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("TECLMS - Lecturer Login");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Label message = new Label();

        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                message.setStyle("-fx-text-fill: red;");
                message.setText("Please enter username and password");
                return;
            }

            try {
                Lecturer lec = new LecturerDAO().login(user, pass);
                if (lec != null) {
                    message.setStyle("-fx-text-fill: green;");
                    message.setText("Welcome, " + lec.getFullName() + "!");

                    Stage dash = new Stage();
                    dash.setTitle("TECLMS - Lecturer Portal");
                    dash.setScene(new Scene(new LecturerDashboard(lec, dash), 1300, 800));
                    dash.show();
                    stage.close();

                } else {
                    message.setStyle("-fx-text-fill: red;");
                    message.setText("Wrong username or password");
                }
            } catch (Exception ex) {
                message.setStyle("-fx-text-fill: red;");
                message.setText("Database error: " + ex.getMessage());
            }
        });

        VBox root = new VBox(12, title, usernameField, passwordField, loginBtn, message);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);

        stage.setTitle("Lecturer Login");
        stage.setScene(new Scene(root, 380, 330));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}