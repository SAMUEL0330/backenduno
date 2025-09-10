import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiseaseDetector {
    private static final Logger logger = Logger.getLogger(DiseaseDetector.class.getName());
    private static final String DISEASE_DB_PATH = "disease_db/";
    private static final String CATALOG_FILE = "disease_db/catalog.csv";
    private static final String REPORTS_FILE = "data/disease_reports.csv";
    private static final String REPORTS_HEADER = "patient_id,disease_id,severity,detection_date,description";
    
    private Map<String, Disease> diseases;
    
    public DiseaseDetector() {
        this.diseases = new HashMap<>();
    }
    
    public void loadDiseaseDatabase() throws Exception {
        File catalogFile = new File(CATALOG_FILE);
        if (!catalogFile.exists()) {
            createSampleDiseaseDatabase();
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(catalogFile))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String diseaseId = parts[0].trim();
                    String name = parts[1].trim();
                    int severity = Integer.parseInt(parts[2].trim());
                    
                    // Load FASTA sequence for this disease
                    String fastaFile = DISEASE_DB_PATH + diseaseId + ".fasta";
                    String sequence = loadSequenceFromFile(fastaFile);
                    
                    if (sequence != null) {
                        diseases.put(diseaseId, new Disease(diseaseId, name, severity, sequence));
                        logger.info("Enfermedad cargada: " + diseaseId + " - " + name);
                    }
                }
            }
            
            logger.info("Base de datos de enfermedades cargada con " + diseases.size() + " enfermedades");
        }
    }
    
    private String loadSequenceFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sequence = new StringBuilder();
            String line;
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine && line.startsWith(">")) {
                    firstLine = false;
                    continue;
                }
                sequence.append(line.trim());
            }
            
            return sequence.toString();
        } catch (IOException e) {
            logger.warning("No se pudo cargar secuencia desde " + filePath + ": " + e.getMessage());
            return null;
        }
    }
    
    public void analyzeSequence(String patientId, String patientSequence, 
                              java.io.PrintWriter writer, ServerLogger serverLogger) {
        for (Disease disease : diseases.values()) {
            double similarity = calculateSimilarity(patientSequence, disease.getSequence());
            
            // Consider a match if similarity is above 85%
            if (similarity >= 0.85) {
                String message = "DISEASE_DETECTED|" + disease.getDiseaseId() + "|" + 
                               disease.getName() + "|" + disease.getSeverity() + "|" + 
                               String.format("%.2f", similarity * 100) + "%";
                
                writer.println(message);
                
                // Log the detection
                String description = String.format("Sequence similarity: %.2f%%", similarity * 100);
                logDiseaseDetection(patientId, disease.getDiseaseId(), disease.getSeverity(), description);
                
                serverLogger.log("Enfermedad detectada para paciente " + patientId + ": " + 
                               disease.getName() + " (similitud: " + String.format("%.2f", similarity * 100) + "%)");
            }
        }
    }
    
    private double calculateSimilarity(String seq1, String seq2) {
        if (seq1 == null || seq2 == null || seq1.isEmpty() || seq2.isEmpty()) {
            return 0.0;
        }
        
        int minLength = Math.min(seq1.length(), seq2.length());
        int matches = 0;
        
        for (int i = 0; i < minLength; i++) {
            if (seq1.charAt(i) == seq2.charAt(i)) {
                matches++;
            }
        }
        
        return (double) matches / minLength;
    }
    
    private void logDiseaseDetection(String patientId, String diseaseId, int severity, String description) {
        try {
            File reportsFile = new File(REPORTS_FILE);
            boolean writeHeader = !reportsFile.exists();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(reportsFile, true))) {
                if (writeHeader) {
                    writer.println(REPORTS_HEADER);
                }
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.println(String.join(",", patientId, diseaseId, String.valueOf(severity), timestamp, description));
            }
        } catch (IOException e) {
            logger.severe("Error registrando detección de enfermedad: " + e.getMessage());
        }
    }
    
    private void createSampleDiseaseDatabase() throws Exception {
        File dbDir = new File(DISEASE_DB_PATH);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        
        // Create catalog file
        try (PrintWriter writer = new PrintWriter(new FileWriter(CATALOG_FILE))) {
            writer.println("disease_id,name,severity");
            writer.println("D001,Trastorno Genético Alfa,8");
            writer.println("D002,Condición Hereditaria Beta,6");
            writer.println("D003,Anormalidad Cromosómica Gamma,9");
            writer.println("D004,Síndrome Metabólico Delta,5");
            writer.println("D005,Deficiencia Inmune Epsilon,7");
        }
        
        // Create sample FASTA files
        createSampleFastaFile("D001", "ACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTA");
        createSampleFastaFile("D002", "GGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGT");
        createSampleFastaFile("D003", "TAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGG");
        createSampleFastaFile("D004", "CTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAG");
        createSampleFastaFile("D005", "AACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTAAACCGGTAGCTAGCTAGGCTAACGTACGTGGCCTTA");
        
        logger.info("Base de datos de enfermedades de muestra creada");
    }
    
    private void createSampleFastaFile(String diseaseId, String sequence) throws IOException {
        String filename = DISEASE_DB_PATH + diseaseId + ".fasta";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(">" + diseaseId);
            writer.println(sequence);
        }
    }
    
    private static class Disease {
        private String diseaseId;
        private String name;
        private int severity;
        private String sequence;
        
        public Disease(String diseaseId, String name, int severity, String sequence) {
            this.diseaseId = diseaseId;
            this.name = name;
            this.severity = severity;
            this.sequence = sequence;
        }
        
        public String getDiseaseId() { return diseaseId; }
        public String getName() { return name; }
        public int getSeverity() { return severity; }
        public String getSequence() { return sequence; }
    }
}
