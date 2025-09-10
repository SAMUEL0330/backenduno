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
    
    public ClientHandler(SSLSocket clientSocket, PatientManager patientManager, 
                        DiseaseDetector diseaseDetector, ServerLogger serverLogger) {
        this.clientSocket = clientSocket;
        this.patientManager = patientManager;
        this.diseaseDetector = diseaseDetector;
        this.serverLogger = serverLogger;
    }
    
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            logger.info("Manejando cliente: " + clientAddress);
            serverLogger.log("Cliente conectado: " + clientAddress);
            
            writer.println("GENOMIC_SERVER_READY");
            
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                try {
                    processCommand(inputLine);
                } catch (Exception e) {
                    logger.severe("Error procesando comando: " + e.getMessage());
                    writer.println("ERROR|" + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            logger.warning("Error de conexión de cliente: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void processCommand(String command) throws Exception {
        String[] parts = command.split("\\|", -1);
        String operation = parts[0];
        
        serverLogger.log("Procesando comando: " + operation);
        
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
                writer.println("ERROR|Comando desconocido: " + operation);
        }
    }
    
    private void handleCreatePatient(String[] parts) throws Exception {
        logger.info("CREATE_PATIENT parts length: " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            logger.info("Part[" + i + "]: '" + parts[i] + "'");
        }
        if (parts.length < 7) {
            writer.println("ERROR|Formato CREATE_PATIENT inválido");
            return;
        }
        
        String fullName = parts[1];
        String documentId = parts[2];
        String contactEmail = parts[3];
        int age = Integer.parseInt(parts[4]);
        String sex = parts[5];
        String clinicalNotes = parts[6];
        
        if (patientManager.existsByDocumentId(documentId)) {
            writer.println("ERROR|Ya existe paciente con ese documento de identidad");
            return;
        }
        
        Patient patient = patientManager.createPatient(fullName, documentId, contactEmail, age, sex, clinicalNotes);
        writer.println("PATIENT_CREATED|" + patient.getPatientId());
        
        serverLogger.log("Paciente creado: " + patient.getPatientId() + " (" + fullName + ")");
    }
    
    private void handleGetPatient(String[] parts) throws Exception {
        if (parts.length < 2) {
            writer.println("ERROR|Formato GET_PATIENT inválido");
            return;
        }
        
        String patientId = parts[1];
        Patient patient = patientManager.getPatient(patientId);
        
        if (patient == null) {
            writer.println("ERROR|Paciente no encontrado");
            return;
        }
        
        if (patient.isDeleted()) {
            writer.println("ERROR|Paciente está inactivo");
            return;
        }
        
        String diseaseInfo = getDiseaseInfoForPatient(patientId);
        
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
        response.append(patient.getFileSizeBytes()).append("|");
        response.append(diseaseInfo);
        
        writer.println(response.toString());
        serverLogger.log("Datos de paciente obtenidos: " + patientId);
    }
    
    private String getDiseaseInfoForPatient(String patientId) {
        try (BufferedReader reader = new BufferedReader(new FileReader("data/disease_reports.csv"))) {
            String line = reader.readLine();
            StringBuilder diseaseIds = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].trim().equals(patientId)) {
                    if (diseaseIds.length() > 0) {
                        diseaseIds.append(", ");
                    }
                    diseaseIds.append(parts[1].trim());
                }
            }
            
            return diseaseIds.length() > 0 ? diseaseIds.toString() : "No se detectaron enfermedades";
        } catch (Exception e) {
            logger.warning("Error leyendo reportes de enfermedades: " + e.getMessage());
            return "Error leyendo datos de enfermedades";
        }
    }
    
    private void handleUpdatePatient(String[] parts) throws Exception {
        if (parts.length < 7) {
            writer.println("ERROR|Formato UPDATE_PATIENT inválido");
            return;
        }
        
        String patientId = parts[1];
        Patient patient = patientManager.getPatient(patientId);
        
        if (patient == null || patient.isDeleted()) {
            writer.println("ERROR|Paciente no encontrado o inactivo");
            return;
        }
        
        patient.setFullName(parts[2]);
        patient.setContactEmail(parts[3]);
        patient.setAge(Integer.parseInt(parts[4]));
        patient.setSex(parts[5]);
        patient.setClinicalNotes(parts[6]);
        
        patientManager.updatePatient(patient);
        writer.println("PATIENT_UPDATED|" + patientId);
        
        serverLogger.log("Paciente actualizado: " + patientId);
    }
    
    private void handleDeletePatient(String[] parts) throws Exception {
        if (parts.length < 2) {
            writer.println("ERROR|Formato DELETE_PATIENT inválido");
            return;
        }
        
        String patientId = parts[1];
        boolean deleted = patientManager.deletePatient(patientId);
        
        if (deleted) {
            writer.println("PATIENT_DELETED|" + patientId);
            serverLogger.log("Paciente eliminado: " + patientId);
        } else {
            writer.println("ERROR|Paciente no encontrado");
        }
    }
    
    private void handleSubmitFasta(String[] parts) throws Exception {
        if (parts.length < 4) {
            writer.println("ERROR|Formato SUBMIT_FASTA inválido");
            return;
        }
        
        String patientId = parts[1];
        String checksum = parts[2];
        int fileSize = Integer.parseInt(parts[3]);
        
        Patient patient = patientManager.getPatient(patientId);
        if (patient == null || patient.isDeleted()) {
            writer.println("ERROR|Paciente no encontrado o inactivo");
            return;
        }
        
        writer.println("READY_FOR_FASTA");
        
        StringBuilder fastaContent = new StringBuilder();
        String line;
        int bytesRead = 0;
        
        while ((line = reader.readLine()) != null && !line.equals("END_FASTA")) {
            fastaContent.append(line).append("\n");
            bytesRead += line.getBytes().length + 1;
        }
        
        String fastaString = fastaContent.toString();
        if (!FastaValidator.isValidFasta(fastaString)) {
            writer.println("ERROR|Formato FASTA inválido");
            return;
        }
        
        String calculatedChecksum = FastaValidator.calculateChecksum(fastaString);
        if (!calculatedChecksum.equals(checksum)) {
            writer.println("ERROR|Checksum no coincide");
            return;
        }
        
        patient.setChecksumFasta(checksum);
        patient.setFileSizeBytes(fileSize);
        patientManager.updatePatient(patient);
        
        FileManager.saveFastaFile(patientId, fastaString);
        
        writer.println("FASTA_RECEIVED");
        
        String sequence = FastaValidator.extractSequence(fastaString);
        diseaseDetector.analyzeSequence(patientId, sequence, writer, serverLogger);
        
        serverLogger.log("Archivo FASTA procesado para paciente: " + patientId);
    }
    
    private void cleanup() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            logger.info("Conexión de cliente cerrada");
        } catch (IOException e) {
            logger.warning("Error cerrando conexión de cliente: " + e.getMessage());
        }
    }
}
