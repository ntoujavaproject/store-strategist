package bigproject.ai;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * AI 進度對話框測試程式
 */
public class AIProgressDialogTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("AI 進度條測試");
        
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        Button testButton = new Button("測試 AI 初始化進度條");
        testButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        testButton.setOnAction(e -> {
            AIProgressDialog dialog = AIProgressDialog.show(primaryStage, "AI 功能初始化測試");
            dialog.startAIInitialization(new AIProgressDialog.ProgressCallback() {
                @Override
                public void onProgress(double progress, String status, String detail) {
                    System.out.println("進度: " + (int)(progress * 100) + "% - " + status);
                }
                
                @Override
                public void onComplete(boolean success) {
                    Platform.runLater(() -> {
                        System.out.println("初始化完成: " + (success ? "成功" : "失敗"));
                        dialog.close();
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        System.out.println("初始化錯誤: " + error);
                        dialog.close();
                    });
                }
            });
        });
        
        root.getChildren().add(testButton);
        
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 