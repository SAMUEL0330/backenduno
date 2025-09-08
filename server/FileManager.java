import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

public class FileManager {
    private static final Logger logger = Logger.getLogger(FileManager.class.getName());
    private static final String FASTA_DIR = "server/data/fasta_files/";
    
    public static void saveFastaFile(String patientId, String fastaContent) throws IOException {
        File fastaDir = new File(FASTA_DIR);
        if (!fastaDir.exists()) {
            fastaDir.mkdirs();
        }
        
        String filename = FASTA_DIR + patientId + ".fasta";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(fastaContent);
        }
        
        logger.info("Saved FASTA file for patient: " + patientId);
    }
    
    public static String loadFastaFile(String patientId) throws IOException {
        String filename = FASTA_DIR + patientId + ".fasta";
        File file = new File(filename);
        
        if (!file.exists()) {
            return null;
        }
        
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
    
    public static boolean deleteFastaFile(String patientId) {
        String filename = FASTA_DIR + patientId + ".fasta";
        File file = new File(filename);
        
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Deleted FASTA file for patient: " + patientId);
            }
            return deleted;
        }
        
        return false;
    }
}
