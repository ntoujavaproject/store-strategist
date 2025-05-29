package bigproject;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Control;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * 動畫管理器 - 提供各種視覺效果和動畫轉場
 * 可用於增強用戶體驗並使介面更加生動
 */
public class AnimationManager {
    
    // 預設動畫持續時間
    private static final Duration DEFAULT_DURATION = Duration.millis(500);
    
    /**
     * 使節點從下方滑入
     * @param node 要動畫的節點
     */
    public static void slideInFromBottom(Node node) {
        slideInFromBottom(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點從下方滑入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void slideInFromBottom(Node node, Duration duration) {
        double startY = node.getLayoutY() + 100;
        double endY = node.getLayoutY();
        
        node.setOpacity(0);
        node.setLayoutY(startY);
        
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromY(50);
        translate.setToY(0);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition transition = new ParallelTransition(translate, fade);
        transition.play();
    }
    
    /**
     * 使節點從左側滑入
     * @param node 要動畫的節點
     */
    public static void slideInFromLeft(Node node) {
        slideInFromLeft(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點從左側滑入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void slideInFromLeft(Node node, Duration duration) {
        node.setOpacity(0);
        
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromX(-50);
        translate.setToX(0);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition transition = new ParallelTransition(translate, fade);
        transition.play();
    }
    
    /**
     * 使節點從上方滑入
     * @param node 要動畫的節點
     */
    public static void slideInFromTop(Node node) {
        slideInFromTop(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點從上方滑入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void slideInFromTop(Node node, Duration duration) {
        node.setOpacity(0);
        
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromY(-50);
        translate.setToY(0);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition transition = new ParallelTransition(translate, fade);
        transition.play();
    }
    
    /**
     * 使節點從右側滑入
     * @param node 要動畫的節點
     */
    public static void slideInFromRight(Node node) {
        slideInFromRight(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點從右側滑入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void slideInFromRight(Node node, Duration duration) {
        node.setOpacity(0);
        
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromX(50);
        translate.setToX(0);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition transition = new ParallelTransition(translate, fade);
        transition.play();
    }
    
    /**
     * 使節點淡入
     * @param node 要動畫的節點
     */
    public static void fadeIn(Node node) {
        fadeIn(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點淡入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void fadeIn(Node node, Duration duration) {
        node.setOpacity(0);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    /**
     * 使節點淡出
     * @param node 要動畫的節點
     */
    public static void fadeOut(Node node) {
        fadeOut(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點淡出，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void fadeOut(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.play();
    }
    
    /**
     * 使節點放大進入
     * @param node 要動畫的節點
     */
    public static void zoomIn(Node node) {
        zoomIn(node, DEFAULT_DURATION);
    }
    
    /**
     * 使節點放大進入，自定義持續時間
     * @param node 要動畫的節點
     * @param duration 動畫持續時間
     */
    public static void zoomIn(Node node, Duration duration) {
        node.setScaleX(0.5);
        node.setScaleY(0.5);
        node.setOpacity(0);
        
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1);
        scale.setToY(1);
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ParallelTransition transition = new ParallelTransition(scale, fade);
        transition.play();
    }
    
    /**
     * 脈動效果，適合強調重要元素
     * @param node 要動畫的節點
     */
    public static void pulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), node);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();
    }
    
    /**
     * 抖動效果，適合錯誤提示
     * @param node 要動畫的節點
     */
    public static void shake(Node node) {
        TranslateTransition translate = new TranslateTransition(Duration.millis(50), node);
        translate.setFromX(0);
        translate.setByX(10);
        translate.setCycleCount(6);
        translate.setAutoReverse(true);
        translate.play();
    }
    
    /**
     * 依序顯示容器中的子節點
     * @param container 包含子節點的容器
     * @param delayBetweenChildren 子節點之間的延遲時間(毫秒)
     */
    public static void showChildrenSequentially(Pane container, int delayBetweenChildren) {
        int delay = 0;
        for (Node child : container.getChildren()) {
            child.setOpacity(0);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), child);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(delay));
            fade.play();
            
            delay += delayBetweenChildren;
        }
    }
    
    /**
     * 使圖表數據逐步顯示的動畫效果
     * @param series 圖表數據系列
     */
    public static void animateChartData(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            node.setOpacity(0);
            
            FadeTransition fade = new FadeTransition(Duration.millis(300), node);
            fade.setDelay(Duration.millis(Math.random() * 500));
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }
    
    /**
     * 載入中旋轉動畫
     * @param node 要旋轉的節點
     * @return 動畫物件，可用於控制動畫播放/停止
     */
    public static RotateTransition createSpinner(Node node) {
        RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), node);
        rotate.setFromAngle(0);
        rotate.setToAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        return rotate;
    }
    
    /**
     * 創建波紋效果 - 適合按鈕點擊或重要動作
     * @param sourceNode 波紋來源節點
     * @param container 波紋顯示的容器
     */
    public static void createRippleEffect(Node sourceNode, Pane container) {
        double centerX = sourceNode.getLayoutX() + sourceNode.getBoundsInLocal().getWidth() / 2;
        double centerY = sourceNode.getLayoutY() + sourceNode.getBoundsInLocal().getHeight() / 2;
        
        // 創建一個圓形的波紋
        javafx.scene.shape.Circle ripple = new javafx.scene.shape.Circle(centerX, centerY, 10);
        ripple.setOpacity(0.5);
        ripple.setStyle("-fx-fill: white; -fx-stroke: white;");
        
        container.getChildren().add(ripple);
        
        // 波紋動畫
        ScaleTransition scale = new ScaleTransition(Duration.millis(700), ripple);
        scale.setToX(5);
        scale.setToY(5);
        
        FadeTransition fade = new FadeTransition(Duration.millis(700), ripple);
        fade.setFromValue(0.5);
        fade.setToValue(0);
        
        ParallelTransition rippleEffect = new ParallelTransition(scale, fade);
        rippleEffect.setOnFinished(e -> container.getChildren().remove(ripple));
        rippleEffect.play();
    }
    
    /**
     * 添加懸停效果 - 當鼠標懸停時輕微放大
     * @param control 要添加效果的控制項
     */
    public static void addHoverEffect(Control control) {
        control.setOnMouseEntered(e -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), control);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            scaleUp.play();
        });
        
        control.setOnMouseExited(e -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), control);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
    }
    
    /**
     * 創建平滑的頁面切換效果
     * @param currentPage 當前頁面
     * @param newPage 新頁面
     * @param container 頁面的容器
     * @param direction 方向 (1: 從右到左, -1: 從左到右)
     */
    public static void smoothPageTransition(Node currentPage, Node newPage, Pane container, int direction) {
        // 設置新頁面初始位置
        newPage.translateXProperty().set(direction * container.getWidth());
        newPage.setOpacity(0);
        
        // 將新頁面添加到容器
        if (!container.getChildren().contains(newPage)) {
            container.getChildren().add(newPage);
        }
        
        // 動畫：當前頁面退出，新頁面進入
        TranslateTransition exitOld = new TranslateTransition(Duration.millis(600), currentPage);
        exitOld.setToX(-direction * container.getWidth());
        
        FadeTransition fadeOutOld = new FadeTransition(Duration.millis(600), currentPage);
        fadeOutOld.setToValue(0);
        
        TranslateTransition enterNew = new TranslateTransition(Duration.millis(600), newPage);
        enterNew.setToX(0);
        
        FadeTransition fadeInNew = new FadeTransition(Duration.millis(600), newPage);
        fadeInNew.setToValue(1);
        
        ParallelTransition exitTransition = new ParallelTransition(exitOld, fadeOutOld);
        ParallelTransition enterTransition = new ParallelTransition(enterNew, fadeInNew);
        
        SequentialTransition sequence = new SequentialTransition(
            new ParallelTransition(exitTransition, enterTransition)
        );
        
        sequence.setOnFinished(e -> {
            // 轉場完成後移除舊頁面
            container.getChildren().remove(currentPage);
        });
        
        sequence.play();
    }
} 