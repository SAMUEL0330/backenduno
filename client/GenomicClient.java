import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.logging.*;

public class GenomicClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8443;
    private static final String TRUSTSTORE_PATH = "certs/client.jks";
    private static final String TRUSTSTORE_PASSWORD = "genomic123";
    private static final Logger logger = Logger.getLogger(GenomicClient.class.getName());
    
    private SSLSocket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ConsoleInterface console;
    
    public GenomicClient() {
        this.console = new ConsoleInterface();
        setupLogging();
    }
    
    private void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler("client/logs/genomic_client.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }
    
    public boolean connect() {
        try {
            SSLContext sslContext = createSSLContext();
            SSLSocketFactory factory = sslContext.getSocketFactory();
            socket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORT);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            String response = reader.readLine();
            if ("GENOMIC_SERVER_READY".equals(response)) {
                logger.info("Connected to Genomic Server");
                console.displayMessage("Connected to Genomic Server successfully!");
                return true;
            } else {
                console.displayError("Unexpected server response: " + response);
                return false;
            }
        } catch (Exception e) {
            console.displayError("Failed to connect to server: " + e.getMessage());
            logger.severe("Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    private SSLContext createSSLContext() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE_PATH)) {
            trustStore.load(fis, TRUSTSTORE_PASSWORD.toCharArray());
        }
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
    
    public void startInteractiveSession() {
        if (!connect()) {
            return;
        }
        
        console.displayWelcome();
        while (true) {
            try {
                int choice = console.showMainMenu();
                switch (choice) {
                    case 1:
                        createPatient();
                        break;
                    case 2:
                        getPatient();
                        break;
                    case 3:
                        updatePatient();
                        break;
                    case 4:
                        deletePatient();
                        break;
                    case 5:
                        submitFasta();
                        break;
                    case 6:
                        testConnection();
                        break;
                    case 0:
                        console.displayMessage("Goodbye!");
                        return;
                    default:
                        console.displayError("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                console.displayError("Error: " + e.getMessage());
                logger.severe("Session error: " + e.getMessage());
            }
        }
    }
    
    private void createPatient() throws IOException {
        console.displayMessage("\n=== Create New Patient ===");
        String fullName = console.getInput("Full Name: ");
        String documentId = console.getInput("Document ID: ");
        String contactEmail = console.getInput("Contact Email: ");
        String ageStr = console.getInput("Age: ");
        String sex = console.getInput("Sex (M/F): ");
        String clinicalNotes = console.getInput("Clinical Notes: ");
        try {
            int age = Integer.parseInt(ageStr);
            String command = String.join("|", "CREATE_PATIENT", fullName, documentId, 
                                       contactEmail, String.valueOf(age), sex, clinicalNotes);
            writer.println(command);
            String response = reader.readLine();
            
            if (response.startsWith("PATIENT_CREATED|")) {
                String patientId = response.split("\\|")[1];
                console.displaySuccess("Patient created successfully! Patient ID: " + patientId);
                logger.info("Patient created: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Failed to create patient: " + response.substring(6));
            }
        } catch (NumberFormatException e) {
            console.displayError("Invalid age format. Please enter a number.");
        }
    }
    
    private void getPatient() throws IOException {
        console.displayMessage("\n=== Get Patient Information ===");
        String patientId = console.getInput("Patient ID: ");
        writer.println("GET_PATIENT|" + patientId);
        String response = reader.readLine();
        
        if (response.startsWith("PATIENT_DATA|")) {
            String[] parts = response.split("\\|");
            console.displayPatientInfo(parts);
            logger.info("Patient data retrieved: " + patientId);
        } else if (response.startsWith("ERROR|")) {
            console.displayError("Failed to get patient: " + response.substring(6));
        }
    }
    
    private void updatePatient() throws IOException {
        console.displayMessage("\n=== Update Patient ===");
        String patientId = console.getInput("Patient ID: ");
        String fullName = console.getInput("New Full Name: ");
        String contactEmail = console.getInput("New Contact Email: ");
        String ageStr = console.getInput("New Age: ");
        String sex = console.getInput("New Sex (M/F): ");
        String clinicalNotes = console.getInput("New Clinical Notes: ");
        try {
            int age = Integer.parseInt(ageStr);
            String command = String.join("|", "UPDATE_PATIENT", patientId, fullName, 
                                       contactEmail, String.valueOf(age), sex, clinicalNotes);
            writer.println(command);
            String response = reader.readLine();
            if (response.startsWith("PATIENT_UPDATED|")) {
                console.displaySuccess("Patient updated successfully!");
                logger.info("Patient updated: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Failed to update patient: " + response.substring(6));
            }
        } catch (NumberFormatException e) {
            console.displayError("Invalid age format. Please enter a number.");
        }
    }
    
    private void deletePatient() throws IOException {
        console.displayMessage("\n=== Delete Patient ===");
        String patientId = console.getInput("Patient ID: ");
        if (console.confirmAction("Are you sure you want to delete patient " + patientId + "? (y/N): ")) {
            writer.println("DELETE_PATIENT|" + patientId);
            String response = reader.readLine();
            if (response.startsWith("PATIENT_DELETED|")) {
                console.displaySuccess("Patient deleted successfully!");
                logger.info("Patient deleted: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Failed to delete patient: " + response.substring(6));
            }
        } else {
            console.displayMessage("Delete operation cancelled.");
        }
    }
    
    private void submitFasta() throws IOException {
        console.displayMessage("\n=== Submit FASTA File ===");
        String patientId = console.getInput("Patient ID: ");
        String fastaFilePath = console.getInput("FASTA file path: ");
        File fastaFile = new File(fastaFilePath);
        if (!fastaFile.exists()) {
            console.displayError("FASTA file not found: " + fastaFilePath);
            return;
        }

        try {
            String fastaContent = readFileContent(fastaFile);
            if (!FastaValidator.isValidFasta(fastaContent)) {
                console.displayError("Invalid FASTA format in file: " + fastaFilePath);
                return;
            }
            
            String checksum = FastaValidator.calculateChecksum(fastaContent);
            int fileSize = (int) fastaFile.length();
            String command = String.join("|", "SUBMIT_FASTA", patientId, checksum, String.valueOf(fileSize));
            writer.println(command);
            String response = reader.readLine();
            if ("READY_FOR_FASTA".equals(response)) {
                String[] lines = fastaContent.split("\n");
                for (String line : lines){
                    writer.println(line);
                }

                writer.println("END_FASTA");
                response = reader.readLine();
                if ("FASTA_RECEIVED".equals(response)){
                    console.displaySuccess("FASTA file submitted successfully!");
                    logger.info("FASTA submitted for patient: " + patientId);
                    console.displayMessage("Analyzing genome for disease markers...");
                    listenForDiseaseDetection();
                } else if (response.startsWith("ERROR|")) {
                    console.displayError("FASTA submission failed: " + response.substring(6));
                }
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Failed to submit FASTA: " + response.substring(6));
            }
        } catch (Exception e) {
            console.displayError("Error reading FASTA file: " + e.getMessage());
        }
    }
    
    private void listenForDiseaseDetection() throws IOException {
        socket.setSoTimeout(5000);
        try {
            String response;
            boolean detectionFound = false;
            while ((response = reader.readLine()) != null) {
                if (response.startsWith("DISEASE_DETECTED|")) {
                    String[] parts = response.split("\\|");
                    if (parts.length >= 5) {
                        console.displayDiseaseDetection(parts[1], parts[2], parts[3], parts[4]);
                        detectionFound = true;
                    }
                } else {
                    break;
                }
            }
            
            if (!detectionFound) {
                console.displaySuccess("No diseases detected in the genome sequence.");
            }
        } catch (SocketTimeoutException e) {
            console.displayMessage("Disease analysis completed.");
        } finally {
            socket.setSoTimeout(0); // Reset timeout
        }
    }
    
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private void testConnection() throws IOException {
        writer.println("PING");
        String response = reader.readLine();
        if ("PONG".equals(response)) {
            console.displaySuccess("Connection test successful!");
        } else {
            console.displayError("Connection test failed. Response: " + response);
        }
    }
    
    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Disconnected from server");
        } catch (IOException e) {
            logger.warning("Error during disconnect: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        GenomicClient client = new GenomicClient();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        try {
            client.startInteractiveSession();
        } finally {
            client.disconnect();
        }
    }
}