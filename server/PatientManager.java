import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class PatientManager {
    private static final Logger logger = Logger.getLogger(PatientManager.class.getName());
    private static final String PATIENTS_CSV = "server/data/patients.csv";
    private static final String CSV_HEADER = "patient_id,full_name,document_id,contact_email,registration_date,age,sex,clinical_notes,checksum_fasta,file_size_bytes,deleted";
    
    private Map<String, Patient> patients;
    private Map<String, String> documentIdToPatientId;
    private AtomicLong patientIdCounter;
    
    public PatientManager() {
        this.patients = new ConcurrentHashMap<>();
        this.documentIdToPatientId = new ConcurrentHashMap<>();
        this.patientIdCounter = new AtomicLong(1);
        loadPatientsFromCsv();
    }
    
    public Patient createPatient(String fullName, String documentId, String contactEmail, 
                               int age, String sex, String clinicalNotes) throws Exception {
        if (existsByDocumentId(documentId)) {
            throw new IllegalArgumentException("Patient with document ID already exists");
        }
        
        String patientId = "P" + String.format("%06d", patientIdCounter.getAndIncrement());
        Patient patient = new Patient(patientId, fullName, documentId, contactEmail, age, sex, clinicalNotes);
        
        patients.put(patientId, patient);
        documentIdToPatientId.put(documentId, patientId);
        
        savePatientsToCSV();
        logger.info("Created patient: " + patientId);
        
        return patient;
    }
    
    public Patient getPatient(String patientId) {
        return patients.get(patientId);
    }
    
    public boolean existsByDocumentId(String documentId) {
        return documentIdToPatientId.containsKey(documentId);
    }
    
    public void updatePatient(Patient patient) throws Exception {
        patients.put(patient.getPatientId(), patient);
        savePatientsToCSV();
        logger.info("Updated patient: " + patient.getPatientId());
    }
    
    public boolean deletePatient(String patientId) throws Exception {
        Patient patient = patients.get(patientId);
        if (patient != null && !patient.isDeleted()) {
            patient.setDeleted(true);
            savePatientsToCSV();
            logger.info("Deleted patient: " + patientId);
            return true;
        }
        return false;
    }
    
    private void loadPatientsFromCsv() {
        File csvFile = new File(PATIENTS_CSV);
        if (!csvFile.exists()) {
            createDataDirectory();
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine();
            long maxId = 0;
            
            while ((line = reader.readLine()) != null) {
                Patient patient = parsePatientFromCsv(line);
                if (patient != null) {
                    patients.put(patient.getPatientId(), patient);
                    documentIdToPatientId.put(patient.getDocumentId(), patient.getPatientId());                    
                    String numericPart = patient.getPatientId().substring(1);
                    long id = Long.parseLong(numericPart);
                    maxId = Math.max(maxId, id);
                }
            }
            
            patientIdCounter.set(maxId + 1);
            logger.info("Loaded " + patients.size() + " patients from CSV");
            
        } catch (IOException e) {
            logger.severe("Error loading patients from CSV: " + e.getMessage());
        }
    }
    
    private Patient parsePatientFromCsv(String csvLine) {
        try {
            List<String> fields = parseCsvLine(csvLine);
            if (fields.size() != 11) {
                logger.warning("Invalid CSV line format: " + csvLine);
                return null;
            }
            
            return new Patient(
                fields.get(0), 
                fields.get(1), 
                fields.get(2), 
                fields.get(3), 
                fields.get(4),
                Integer.parseInt(fields.get(5)), 
                fields.get(6), 
                fields.get(7),
                fields.get(8).isEmpty() ? null : fields.get(8), 
                Integer.parseInt(fields.get(9)), 
                Boolean.parseBoolean(fields.get(10)) 
            );
        } catch (Exception e) {
            logger.warning("Error parsing CSV line: " + csvLine + " - " + e.getMessage());
            return null;
        }
    }
    
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
    
    private void savePatientsToCSV() throws Exception {
        createDataDirectory();
        try (PrintWriter writer = new PrintWriter(new FileWriter(PATIENTS_CSV))) {
            writer.println(CSV_HEADER);
            
            for (Patient patient : patients.values()) {
                writer.println(patient.toCsvString());
            }
            
            logger.info("Saved " + patients.size() + " patients to CSV");
        } catch (IOException e) {
            logger.severe("Error saving patients to CSV: " + e.getMessage());
            throw new Exception("Failed to save patient data", e);
        }
    }
    
    private void createDataDirectory() {
        File dataDir = new File("server/data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    public Collection<Patient> getAllPatients() {
        return patients.values();
    }
    
    public int getPatientCount() {
        return (int) patients.values().stream().filter(p -> !p.isDeleted()).count();
    }
}