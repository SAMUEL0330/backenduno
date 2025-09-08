import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    
    private SSLSocket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private PatientManager patientManager;
    private DiseaseDetector diseaseDetector;
    private ServerLogger serverLogger;
    
    public ClientHandler(SSLSocket clientSocket, PatientManager patientManager, DiseaseDetector diseaseDetector, ServerLogger serverLogger) {
        this.clientSocket = clientSocket;
        this.patientManager = patientManager;
        this.diseaseDetector = diseaseDetector;
        this.serverLogger = serverLogger;
    }
    
    @Override
    public void run(){
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            logger.info("Handling client: " + clientAddress);
            serverLogger.log("Client connected: " + clientAddress);
            
            writer.println("GENOMIC_SERVER_READY");
            
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                try {
                    processCommand(inputLine);
                } catch (Exception e) {
                    logger.severe("Error processing command: " + e.getMessage());
                    writer.println("ERROR|" + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.warning("Client connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void processCommand(String command) throws Exception {
        String[] parts = command.split("\\|", -1);
        String operation = parts[0];
        
        serverLogger.log("Processing command: " + operation);
        
        switch (operation) {
            case "CREATE_PATIENT":
                handleCreatePatient(parts);
                break;
            case "GET_PATIENT":
                handleGetPatient(parts);
                break;
            case "UPDATE_PATIENT":
                handleUpdatePatient(parts);
                break;
            case "DELETE_PATIENT":
                handleDeletePatient(parts);
                break;
            case "SUBMIT_FASTA":
                handleSubmitFasta(parts);
                break;
            case "PING":
                writer.println("PONG");
                break;
            default:
                writer.println("ERROR|Unknown command: " + operation);
        }
    }
    
    private void handleCreatePatient(String[] parts) throws Exception {
        if (parts.length < 8) {
            writer.println("ERROR|Invalid CREATE_PATIENT format");
            return;
        }
        
        String fullName = parts[1];
        String documentId = parts[2];
        String contactEmail = parts[3];
        int age = Integer.parseInt(parts[4]);
        String sex = parts[5];
        String clinicalNotes = parts[6];
        
        if (patientManager.existsByDocumentId(documentId)) {
            writer.println("ERROR|Patient with document ID already exists");
            return;
        }
        
        Patient patient = patientManager.createPatient(fullName, documentId, contactEmail, age, sex, clinicalNotes);
        writer.println("PATIENT_CREATED|" + patient.getPatientId());
        
        serverLogger.log("Patient created: " + patient.getPatientId() + " (" + fullName + ")");
    }
    
    private void handleGetPatient(String[] parts) throws Exception {
        if (parts.length < 2) {
            writer.println("ERROR|Invalid GET_PATIENT format");
            return;
        }
        
        String patientId = parts[1];
        Patient patient = patientManager.getPatient(patientId);
        
        if (patient == null) {
            writer.println("ERROR|Patient not found");
            return;
        }
        
        if (patient.isDeleted()) {
            writer.println("ERROR|Patient is inactive");
            return;
        }
        
        StringBuilder response = new StringBuilder("PATIENT_DATA|");
        response.append(patient.getPatientId()).append("|");
        response.append(patient.getFullName()).append("|");
        response.append(patient.getDocumentId()).append("|");
        response.append(patient.getContactEmail()).append("|");
        response.append(patient.getRegistrationDate()).append("|");
        response.append(patient.getAge()).append("|");
        response.append(patient.getSex()).append("|");
        response.append(patient.getClinicalNotes()).append("|");
        response.append(patient.getChecksumFasta() != null ? patient.getChecksumFasta() : "").append("|");
        response.append(patient.getFileSizeBytes());
        
        writer.println(response.toString());
        serverLogger.log("Patient data retrieved: " + patientId);
    }
    
    private void handleUpdatePatient(String[] parts) throws Exception {
        if (parts.length < 8) {
            writer.println("ERROR|Invalid UPDATE_PATIENT format");
            return;
        }
        
        String patientId = parts[1];
        Patient patient = patientManager.getPatient(patientId);
        
        if (patient == null || patient.isDeleted()) {
            writer.println("ERROR|Patient not found or inactive");
            return;
        }
        
        patient.setFullName(parts[2]);
        patient.setContactEmail(parts[3]);
        patient.setAge(Integer.parseInt(parts[4]));
        patient.setSex(parts[5]);
        patient.setClinicalNotes(parts[6]);
        
        patientManager.updatePatient(patient);
        writer.println("PATIENT_UPDATED|" + patientId);
        
        serverLogger.log("Patient updated: " + patientId);
    }
    
    private void handleDeletePatient(String[] parts) throws Exception {
        if (parts.length < 2) {
            writer.println("ERROR|Invalid DELETE_PATIENT format");
            return;
        }
        
        String patientId = parts[1];
        boolean deleted = patientManager.deletePatient(patientId);
        
        if (deleted) {
            writer.println("PATIENT_DELETED|" + patientId);
            serverLogger.log("Patient deleted: " + patientId);
        } else {
            writer.println("ERROR|Patient not found");
        }
    }
    
    private void handleSubmitFasta(String[] parts) throws Exception {
        if (parts.length < 4) {
            writer.println("ERROR|Invalid SUBMIT_FASTA format");
            return;
        }
        
        String patientId = parts[1];
        String checksum = parts[2];
        int fileSize = Integer.parseInt(parts[3]);
        
        Patient patient = patientManager.getPatient(patientId);
        if (patient == null || patient.isDeleted()) {
            writer.println("ERROR|Patient not found or inactive");
            return;
        }
        
        writer.println("READY_FOR_FASTA");
        
        StringBuilder fastaContent = new StringBuilder();
        String line;
        int bytesRead = 0;
        
        while ((line = reader.readLine()) != null && !line.equals("END_FASTA")) {
            fastaContent.append(line).append("\n");
            bytesRead += line.getBytes().length + 1; // +1 for newline
        }
        
        String fastaString = fastaContent.toString();
        if (!FastaValidator.isValidFasta(fastaString)) {
            writer.println("ERROR|Invalid FASTA format");
            return;
        }
        
        String calculatedChecksum = FastaValidator.calculateChecksum(fastaString);
        if (!calculatedChecksum.equals(checksum)) {
            writer.println("ERROR|Checksum mismatch");
            return;
        }
        
        patient.setChecksumFasta(checksum);
        patient.setFileSizeBytes(fileSize);
        patientManager.updatePatient(patient);
        
        FileManager.saveFastaFile(patientId, fastaString);
        
        writer.println("FASTA_RECEIVED");
        
        String sequence = FastaValidator.extractSequence(fastaString);
        diseaseDetector.analyzeSequence(patientId, sequence, writer, serverLogger);
        
        serverLogger.log("FASTA file processed for patient: " + patientId);
    }
    
    private void cleanup() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            logger.info("Client connection closed");
        } catch (IOException e) {
            logger.warning("Error closing client connection: " + e.getMessage());
        }
    }
}
