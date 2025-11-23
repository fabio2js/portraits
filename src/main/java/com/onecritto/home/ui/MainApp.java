package com.onecritto.home.ui;

import com.onecritto.home.App;
import com.onecritto.home.security.TempVaultFiles;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        TempVaultFiles.cleanupTempDirSecure();

        App.init(stage);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml")
        );


        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/onecritto-theme.css")).toExternalForm()
        );
        stage.setTitle("OneCritto");
        stage.centerOnScreen();
        stage.setScene(scene);
        stage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/icons/onecritto_white_key_256x256.png"))
        ));
        stage.show();

        // Cleanup in caso di chiusura app
        Runtime.getRuntime().addShutdownHook(new Thread(TempVaultFiles::cleanupTempDirSecure));
    }


}
