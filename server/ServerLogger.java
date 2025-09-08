import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class ServerLogger {
    private static final Logger logger = Logger.getLogger(ServerLogger.class.getName());
    private static final String LOG_FILE = "server/logs/server_operations.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ServerLogger() {
        createLogDirectory();
    }
    
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] " + message;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            logger.severe("Failed to write to log file: " + e.getMessage());
        }
        System.out.println(logEntry);
    }
    
    private void createLogDirectory() {
        File logDir = new File("server/logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
}