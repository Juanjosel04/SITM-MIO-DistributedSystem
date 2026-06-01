package app;

import javafx.application.Application;

import java.awt.GraphicsEnvironment;

public class Main {
    public static void main(String[] args) {
        if (isUiEnabled()) {
            Application.launch(SitmMioFxApplication.class, args);
        } else {
            new ApplicationInitializer().run();
        }
    }

    private static boolean isUiEnabled() {
        String property = System.getProperty("sitm.ui.enabled");
        if (property != null && "false".equalsIgnoreCase(property.trim())) {
            return false;
        }
        return !GraphicsEnvironment.isHeadless();
    }
}
