package com.onecritto.home.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class UIUtils {



    public static void showError(String message) {
        showError(null, message);
    }

    public static void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), unit);
    }


    public static void showToast(Node anchorNode, String message) {

        // Recupera lo Stage partendo da un nodo qualsiasi
        Scene scene = anchorNode.getScene();
        Stage stage = (Stage) scene.getWindow();
        Parent root = scene.getRoot();

        // Se il root non è uno StackPane, lo wrappiamo
        if (!(root instanceof StackPane)) {
            StackPane stack = new StackPane();
            stack.getChildren().add(root);
            scene.setRoot(stack);
            root = stack;
        }

        StackPane stackRoot = (StackPane) root;

        Label toast = new Label(message);
        toast.setStyle("-fx-background-color: #323232; -fx-text-fill: white; "
                + "-fx-padding: 10; -fx-background-radius: 5;");
        toast.setOpacity(0);

        stackRoot.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setToValue(1);

        PauseTransition delay = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
        fadeOut.setToValue(0);

        fadeIn.setOnFinished(e -> delay.play());
        delay.setOnFinished(e -> fadeOut.play());
        fadeOut.setOnFinished(e -> stackRoot.getChildren().remove(toast));

        fadeIn.play();
    }

}
