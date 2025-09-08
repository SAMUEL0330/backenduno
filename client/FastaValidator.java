import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class FastaValidator {
    private static final Pattern FASTA_HEADER_PATTERN = Pattern.compile("^>[\\w\\d_]+$");
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("^[ACGTN]+$");
    
    public static boolean isValidFasta(String fastaContent) {
        if (fastaContent == null || fastaContent.trim().isEmpty()) {
            return false;
        }
        
        String[] lines = fastaContent.trim().split("\n");
        
        if (lines.length < 2) {
            return false;
        }
        
        // Check header line
        if (!lines[0].startsWith(">")) {
            return false;
        }
        
        if (!FASTA_HEADER_PATTERN.matcher(lines[0]).matches()) {
            return false;
        }
        
        // Check sequence lines
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim().toUpperCase();
            if (!line.isEmpty() && !SEQUENCE_PATTERN.matcher(line).matches()) {
                return false;
            }
        }
        
        return true;
    }
    
    public static String extractSequence(String fastaContent) {
        if (fastaContent == null) {
            return "";
        }
        
        String[] lines = fastaContent.trim().split("\n");
        StringBuilder sequence = new StringBuilder();
        
        for (int i = 1; i < lines.length; i++) {
            sequence.append(lines[i].trim().toUpperCase());
        }
        
        return sequence.toString();
    }
    
    public static String calculateChecksum(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
