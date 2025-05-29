package bigproject;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;

/**
 * 自定義啟動畫面，在應用程式主界面顯示前顯示
 */
public class CustomSplashScreen extends Preloader {
    
    private Stage splashStage;
    private ProgressBar progressBar;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.splashStage = stage;
        
        // 設置啟動畫面標題
        stage.setTitle("Restaurant Analyzer");
        
        // 嘗試加載圖標
        try {
            // 優先使用與主應用相同的圖標
            Image appIcon = new Image(getClass().getResourceAsStream("/icons/restaurant_icon.png"));
            if (appIcon != null && !appIcon.isError()) {
                stage.getIcons().add(appIcon);
            }
        } catch (Exception e) {
            System.err.println("啟動畫面無法載入圖標: " + e.getMessage());
        }
        
        // 建立啟動畫面UI
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        root.setPrefSize(400, 300);
        
        // 標題
        Label titleLabel = new Label("Restaurant Analyzer");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // 嘗試加載圖標作為啟動畫面圖像
        try {
            ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/icons/restaurant_icon.png")));
            logoView.setFitHeight(100);
            logoView.setFitWidth(100);
            logoView.setPreserveRatio(true);
            root.getChildren().add(logoView);
        } catch (Exception e) {
            // 如果圖標載入失敗，使用文本替代
            Label logoLabel = new Label("RA");
            logoLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #6F6732;");
            root.getChildren().add(logoLabel);
        }
        
        // 加載進度條
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        
        // 版本和版權信息
        Label versionLabel = new Label("Version 1.0.0");
        Label copyrightLabel = new Label("© 2025 Restaurant Analyzer Team");
        copyrightLabel.setStyle("-fx-font-size: 10px;");
        
        root.getChildren().addAll(titleLabel, progressBar, versionLabel, copyrightLabel);
        
        // 設置場景和顯示
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.initStyle(StageStyle.DECORATED);
        stage.show();
    }
    
    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            progressBar.setProgress(((ProgressNotification) info).getProgress());
        }
    }
    
    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            splashStage.hide();
        }
    }
} 