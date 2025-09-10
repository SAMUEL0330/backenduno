import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.logging.*;

public class GenomicClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8443;
    private static final String TRUSTSTORE_PATH = "../certs/client.jks";
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
            FileHandler fileHandler = new FileHandler("logs/genomic_client.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Falló configuración de logging: " + e.getMessage());
        }
    }
    
    public boolean connect() {
        try {
            // Create SSL context with truststore
            SSLContext sslContext = createSSLContext();
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            socket = (SSLSocket) factory.createSocket(SERVER_HOST, SERVER_PORT);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Wait for server ready message
            String response = reader.readLine();
            if ("GENOMIC_SERVER_READY".equals(response)) {
                logger.info("Conectado al Servidor Genómico");
                console.displayMessage("¡Conectado al Servidor Genómico exitosamente!");
                return true;
            } else {
                console.displayError("Respuesta inesperada del servidor: " + response);
                return false;
            }
            
        } catch (Exception e) {
            console.displayError("Falló conexión al servidor: " + e.getMessage());
            logger.severe("Conexión falló: " + e.getMessage());
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
                        console.displayMessage("¡Adiós!");
                        return;
                    default:
                        console.displayError("Opción inválida. Por favor intente de nuevo.");
                }
            } catch (Exception e) {
                console.displayError("Error: " + e.getMessage());
                logger.severe("Error de sesión: " + e.getMessage());
            }
        }
    }
    
    private void createPatient() throws IOException {
        console.displayMessage("\n=== Crear Nuevo Paciente ===");
        
        String fullName = console.getInput("Nombre Completo: ");
        String documentId = console.getInput("Documento de Identidad: ");
        String contactEmail = console.getInput("Email de Contacto: ");
        String ageStr = console.getInput("Edad: ");
        String sex = console.getInput("Sexo (M/F): ");
        String clinicalNotes = console.getInput("Notas Clínicas: ");
        
        try {
            int age = Integer.parseInt(ageStr);
            
            String command = String.join("|", "CREATE_PATIENT", fullName, documentId, 
                                       contactEmail, String.valueOf(age), sex, clinicalNotes);
            System.out.println("Comando enviado: " + command);
            writer.println(command);
            String response = reader.readLine();
            
            if (response.startsWith("PATIENT_CREATED|")) {
                String patientId = response.split("\\|")[1];
                console.displaySuccess("¡Paciente creado exitosamente! ID del Paciente: " + patientId);
                logger.info("Paciente creado: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Falló al crear paciente: " + response.substring(6));
            }
            
        } catch (NumberFormatException e) {
            console.displayError("Formato de edad inválido. Por favor ingrese un número.");
        }
    }
    
    private void getPatient() throws IOException {
        console.displayMessage("\n=== Obtener Información del Paciente ===");
        
        String patientId = console.getInput("ID del Paciente: ");
        
        writer.println("GET_PATIENT|" + patientId);
        String response = reader.readLine();
        
        if (response.startsWith("PATIENT_DATA|")) {
            String[] parts = response.split("\\|");
            console.displayPatientInfo(parts);
            logger.info("Datos del paciente obtenidos: " + patientId);
        } else if (response.startsWith("ERROR|")) {
            console.displayError("Falló al obtener paciente: " + response.substring(6));
        }
    }
    
    private void updatePatient() throws IOException {
        console.displayMessage("\n=== Actualizar Paciente ===");
        
        String patientId = console.getInput("ID del Paciente: ");
        String fullName = console.getInput("Nuevo Nombre Completo: ");
        String contactEmail = console.getInput("Nuevo Email de Contacto: ");
        String ageStr = console.getInput("Nueva Edad: ");
        String sex = console.getInput("Nuevo Sexo (M/F): ");
        String clinicalNotes = console.getInput("Nuevas Notas Clínicas: ");
        
        try {
            int age = Integer.parseInt(ageStr);
            
            String command = String.join("|", "UPDATE_PATIENT", patientId, fullName, 
                                       contactEmail, String.valueOf(age), sex, clinicalNotes);
            
            writer.println(command);
            String response = reader.readLine();
            
            if (response.startsWith("PATIENT_UPDATED|")) {
                console.displaySuccess("¡Paciente actualizado exitosamente!");
                logger.info("Paciente actualizado: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Falló al actualizar paciente: " + response.substring(6));
            }
            
        } catch (NumberFormatException e) {
            console.displayError("Formato de edad inválido. Por favor ingrese un número.");
        }
    }
    
    private void deletePatient() throws IOException {
        console.displayMessage("\n=== Eliminar Paciente ===");
        
        String patientId = console.getInput("ID del Paciente: ");
        
        if (console.confirmAction("¿Está seguro que desea eliminar el paciente " + patientId + "? (s/N): ")) {
            writer.println("DELETE_PATIENT|" + patientId);
            String response = reader.readLine();
            
            if (response.startsWith("PATIENT_DELETED|")) {
                console.displaySuccess("¡Paciente eliminado exitosamente!");
                logger.info("Paciente eliminado: " + patientId);
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Falló al eliminar paciente: " + response.substring(6));
            }
        } else {
            console.displayMessage("Operación de eliminación cancelada.");
        }
    }
    
    private void submitFasta() throws IOException {
        console.displayMessage("\n=== Enviar Archivo FASTA ===");
        
        String patientId = console.getInput("ID del Paciente: ");
        String fastaFilePath = console.getInput("Ruta del archivo FASTA: ");
        
        File fastaFile = new File(fastaFilePath);
        if (!fastaFile.exists()) {
            console.displayError("Archivo FASTA no encontrado: " + fastaFilePath);
            return;
        }
        
        try {
            String fastaContent = readFileContent(fastaFile);
            
            if (!FastaValidator.isValidFasta(fastaContent)) {
                console.displayError("Formato FASTA inválido en archivo: " + fastaFilePath);
                return;
            }
            
            String checksum = FastaValidator.calculateChecksum(fastaContent);
            int fileSize = (int) fastaFile.length();
            
            // Send FASTA submission command
            String command = String.join("|", "SUBMIT_FASTA", patientId, checksum, String.valueOf(fileSize));
            writer.println(command);
            
            String response = reader.readLine();
            if ("READY_FOR_FASTA".equals(response)) {
                // Send FASTA content
                String[] lines = fastaContent.split("\n");
                for (String line : lines) {
                    writer.println(line);
                }
                writer.println("END_FASTA");
                
                // Wait for confirmation
                response = reader.readLine();
                if ("FASTA_RECEIVED".equals(response)) {
                    console.displaySuccess("¡Archivo FASTA enviado exitosamente!");
                    logger.info("FASTA enviado para paciente: " + patientId);
                    
                    // Listen for disease detection results
                    console.displayMessage("Analizando genoma en busca de marcadores de enfermedad...");
                    listenForDiseaseDetection();
                    
                } else if (response.startsWith("ERROR|")) {
                    console.displayError("Envío de FASTA falló: " + response.substring(6));
                }
            } else if (response.startsWith("ERROR|")) {
                console.displayError("Falló al enviar FASTA: " + response.substring(6));
            }
            
        } catch (Exception e) {
            console.displayError("Error leyendo archivo FASTA: " + e.getMessage());
        }
    }
    
    private void listenForDiseaseDetection() throws IOException {
        // Set a timeout for disease detection results
        socket.setSoTimeout(5000); // 5 seconds timeout
        
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
                    break; // No more disease detection messages
                }
            }
            
            if (!detectionFound) {
                console.displaySuccess("No se detectaron enfermedades en la secuencia genómica.");
            }
            
        } catch (SocketTimeoutException e) {
            console.displayMessage("Análisis de enfermedades completado.");
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
            console.displaySuccess("¡Prueba de conexión exitosa!");
        } else {
            console.displayError("Prueba de conexión falló. Respuesta: " + response);
        }
    }
    
    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Desconectado del servidor");
        } catch (IOException e) {
            logger.warning("Error durante desconexión: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        GenomicClient client = new GenomicClient();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        
        try {
            client.startInteractiveSession();
        } finally {
            client.disconnect();
        }
    }
}
