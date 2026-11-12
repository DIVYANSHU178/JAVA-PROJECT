import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Responsibility: To serve the Single-Page Web Application HTML/CSS/JS.
public class WebUI {

    public static String getHtmlContent() {
        Path filePath = Paths.get("web", "index.html");
        if (Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (IOException ignored) {}
        }

        Path directPath = Paths.get("index.html");
        if (Files.exists(directPath)) {
            try {
                return Files.readString(directPath);
            } catch (IOException ignored) {}
        }

        return "<h1>RailStay Express</h1><p>web/index.html not found.</p>";
    }
}
