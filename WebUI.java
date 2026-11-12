import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Responsibility: To serve the Single-Page Web Application HTML/CSS/JS.
public class WebUI {

    private static String cachedHtml = null;

    public static String getHtmlContent() {
        if (cachedHtml != null) {
            return cachedHtml;
        }

        // Attempt to load from web/index.html
        Path filePath = Paths.get("web", "index.html");
        if (Files.exists(filePath)) {
            try {
                cachedHtml = Files.readString(filePath);
                return cachedHtml;
            } catch (IOException ignored) {
            }
        }

        // Fallback to current working directory
        Path directPath = Paths.get("index.html");
        if (Files.exists(directPath)) {
            try {
                cachedHtml = Files.readString(directPath);
                return cachedHtml;
            } catch (IOException ignored) {
            }
        }

        return "<h1>RailStay Express</h1><p>web/index.html not found. Please ensure web directory is present.</p>";
    }
}
