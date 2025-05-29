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
    private static final String APP_ICON_PNG_PATH = "/icons/restaurant_icon.png";
    private static final String APP_ICON_ICNS_PATH = "/icons/restaurant_icon.icns";
    
    /**
     * 設置應用程式視窗圖標
     * @param stage 要設置圖標的主視窗
     * @return 是否成功設置圖標
     */
    public static boolean setAppIcon(Stage stage) {
        if (stage == null) return false;
        
        try {
            // 根據不同作業系統選擇圖標格式
            String osName = System.getProperty("os.name").toLowerCase();
            String iconPath;
            
            // 檢測作業系統類型
            if (osName.contains("mac")) {
                // macOS 系統優先使用 .icns 格式
                iconPath = APP_ICON_ICNS_PATH;
                System.out.println("檢測到 macOS 系統，使用 .icns 圖標格式");
            } else {
                // 其他系統（Windows、Linux 等）使用 .png 格式
                iconPath = APP_ICON_PNG_PATH;
                System.out.println("檢測到 " + osName + " 系統，使用 .png 圖標格式");
            }
            
            // 嘗試多種方式載入圖標
            Image appIcon = null;
            
            // 方法1: 使用類載入器
            InputStream iconStream = ResourceManager.class.getResourceAsStream(iconPath);
            if (iconStream != null) {
                appIcon = new Image(iconStream);
                System.out.println("成功通過類載入器載入圖標: " + iconPath);
            } else {
                // 方法2: 使用絕對路徑
                URL iconUrl = ResourceManager.class.getResource(iconPath);
                if (iconUrl != null) {
                    appIcon = new Image(iconUrl.toString());
                    System.out.println("成功通過資源URL載入圖標: " + iconUrl);
                } else {
                    // 方法3: 嘗試使用不同的類載入器
                    iconStream = ClassLoader.getSystemResourceAsStream(iconPath.startsWith("/") ? iconPath.substring(1) : iconPath);
                    if (iconStream != null) {
                        appIcon = new Image(iconStream);
                        System.out.println("成功通過系統類載入器載入圖標");
                    } else {
                        // 方法4: 使用File路徑（僅開發階段有效）
                        String filePath = "src/main/resources" + iconPath;
                        try {
                            appIcon = new Image("file:" + filePath);
                            if (!appIcon.isError()) {
                                System.out.println("成功通過文件路徑載入圖標: " + filePath);
                            } else {
                                System.err.println("無法載入圖標文件: " + filePath);
                                
                                // 如果指定格式失敗，嘗試回退到另一種格式
                                String fallbackPath = iconPath.equals(APP_ICON_PNG_PATH) ? APP_ICON_ICNS_PATH : APP_ICON_PNG_PATH;
                                System.out.println("嘗試使用備用圖標格式: " + fallbackPath);
                                
                                iconStream = ResourceManager.class.getResourceAsStream(fallbackPath);
                                if (iconStream != null) {
                                    appIcon = new Image(iconStream);
                                    System.out.println("成功載入備用圖標格式");
                                } else {
                                return false;
                                }
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