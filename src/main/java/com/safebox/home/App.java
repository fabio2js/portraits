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

    public static void setRoot(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/fxml/" + fxml + ".fxml")
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento FXML: " + fxml, e);
        }
    }
}
