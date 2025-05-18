package bigproject;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * 應用程式啟動器 - 直接啟動餐廳分析應用程式
 */
public class AppLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 直接啟動主應用程式
            compare compareApp = new compare();
            compareApp.start(primaryStage);
        } catch (Exception ex) {
            System.err.println("無法啟動應用程式: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        // 設置應用程式在系統中顯示的名稱
        System.setProperty("apple.awt.application.name", "Restaurant Analyzer");
        System.setProperty("javafx.preloader", "bigproject.CustomSplashScreen");
        
        launch(args);
    }
} 