package bigproject;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.InputStream;
import java.net.URL;

/**
 * 管理應用程式資源的工具類，如圖標、樣式等
 */
public class ResourceManager {
    
    // 圖標路徑常量
    private static final String APP_ICON_PATH = "/icons/restaurant_icon.png";
    
    /**
     * 設置應用程式視窗圖標
     * @param stage 要設置圖標的主視窗
     * @return 是否成功設置圖標
     */
    public static boolean setAppIcon(Stage stage) {
        if (stage == null) return false;
        
        try {
            // 嘗試多種方式載入圖標
            Image appIcon = null;
            
            // 方法1: 使用類載入器
            InputStream iconStream = ResourceManager.class.getResourceAsStream(APP_ICON_PATH);
            if (iconStream != null) {
                appIcon = new Image(iconStream);
                System.out.println("成功通過類載入器載入圖標");
            } else {
                // 方法2: 使用絕對路徑
                URL iconUrl = ResourceManager.class.getResource(APP_ICON_PATH);
                if (iconUrl != null) {
                    appIcon = new Image(iconUrl.toString());
                    System.out.println("成功通過資源URL載入圖標: " + iconUrl);
                } else {
                    // 方法3: 嘗試使用不同的類載入器
                    iconStream = ClassLoader.getSystemResourceAsStream("icons/restaurant_icon.png");
                    if (iconStream != null) {
                        appIcon = new Image(iconStream);
                        System.out.println("成功通過系統類載入器載入圖標");
                    } else {
                        // 方法4: 使用File路徑（僅開發階段有效）
                        String iconPath = "src/main/resources" + APP_ICON_PATH;
                        try {
                            appIcon = new Image("file:" + iconPath);
                            if (!appIcon.isError()) {
                                System.out.println("成功通過文件路徑載入圖標: " + iconPath);
                            } else {
                                System.err.println("無法載入圖標文件: " + iconPath);
                                return false;
                            }
                        } catch (Exception e) {
                            System.err.println("嘗試通過文件路徑載入圖標失敗: " + e.getMessage());
                        }
                    }
                }
            }
            
            if (appIcon != null && !appIcon.isError()) {
                stage.getIcons().add(appIcon);
                return true;
            } else {
                System.err.println("無法載入任何應用程式圖標");
                return false;
            }
        } catch (Exception e) {
            System.err.println("載入圖標出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 載入圖片資源
     * @param path 資源路徑
     * @return 圖片對象，如果載入失敗返回null
     */
    public static Image loadImage(String path) {
        try {
            return new Image(ResourceManager.class.getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("無法載入圖片資源 " + path + ": " + e.getMessage());
            return null;
        }
    }
} 