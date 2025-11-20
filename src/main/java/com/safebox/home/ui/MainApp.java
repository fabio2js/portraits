package com.safebox.home.ui;

import com.safebox.home.App;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        App.init(stage); // <--- fondamentale

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setTitle("SafeBox Home");
        stage.centerOnScreen();
        stage.setScene(scene);
        stage.show();
    }

}
