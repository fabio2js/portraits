package com.safebox.home;


import com.safebox.home.service.VaultContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public class App {

    @Getter
    @Setter
    private static VaultContext context;
    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }


}
