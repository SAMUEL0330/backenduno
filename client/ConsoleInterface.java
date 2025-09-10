import java.util.Scanner;

public class ConsoleInterface {
    private Scanner scanner;
    
    public ConsoleInterface() {
        this.scanner = new Scanner(System.in);
    }
    
    public void displayWelcome() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("    CLIENTE DE PROCESAMIENTO DE DATOS GENÓMICOS");
        System.out.println("    Conexión SSL/TLS Segura Establecida");
        System.out.println("=".repeat(60));
    }
    
    public int showMainMenu() {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1. Crear Nuevo Paciente");
        System.out.println("2. Obtener Información del Paciente");
        System.out.println("3. Actualizar Paciente");
        System.out.println("4. Eliminar Paciente");
        System.out.println("5. Enviar Archivo FASTA");
        System.out.println("6. Probar Conexión");
        System.out.println("0. Salir");
        System.out.print("\nSeleccione una opción: ");
        
        try {
            if (scanner.hasNextLine()) {
                return Integer.parseInt(scanner.nextLine().trim());
            } else {
                throw new RuntimeException("No se encontró línea");
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    public boolean confirmAction(String prompt) {
        System.out.print(prompt);
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("s") || response.equals("si") || response.equals("y") || response.equals("yes");
    }
    
    public void displayMessage(String message) {
        System.out.println(message);
    }
    
    public void displaySuccess(String message) {
        System.out.println("✓ " + message);
    }
    
    public void displayError(String message) {
        System.err.println("✗ ERROR: " + message);
    }
    
    public void displayPatientInfo(String[] patientData) {
        if (patientData.length >= 12) {
            System.out.println("\n--- INFORMACIÓN DEL PACIENTE ---");
            System.out.println("ID del Paciente: " + patientData[1]);
            System.out.println("Nombre Completo: " + patientData[2]);
            System.out.println("Documento de Identidad: " + patientData[3]);
            System.out.println("Email de Contacto: " + patientData[4]);
            System.out.println("Fecha de Registro: " + patientData[5]);
            System.out.println("Edad: " + patientData[6]);
            System.out.println("Sexo: " + patientData[7]);
            System.out.println("Notas Clínicas: " + patientData[8]);
            System.out.println("Checksum FASTA: " + (patientData[9].isEmpty() ? "No enviado" : patientData[9]));
            System.out.println("Tamaño de Archivo: " + patientData[10] + " bytes");
            System.out.println("Enfermedades Detectadas: " + patientData[11]);
            System.out.println("-".repeat(30));
        } else if (patientData.length >= 11) {
            System.out.println("\n--- INFORMACIÓN DEL PACIENTE ---");
            System.out.println("ID del Paciente: " + patientData[1]);
            System.out.println("Nombre Completo: " + patientData[2]);
            System.out.println("Documento de Identidad: " + patientData[3]);
            System.out.println("Email de Contacto: " + patientData[4]);
            System.out.println("Fecha de Registro: " + patientData[5]);
            System.out.println("Edad: " + patientData[6]);
            System.out.println("Sexo: " + patientData[7]);
            System.out.println("Notas Clínicas: " + patientData[8]);
            System.out.println("Checksum FASTA: " + (patientData[9].isEmpty() ? "No enviado" : patientData[9]));
            System.out.println("Tamaño de Archivo: " + patientData[10] + " bytes");
            System.out.println("Enfermedades Detectadas: No hay datos disponibles");
            System.out.println("-".repeat(30));
        }
    }
    
    public void displayDiseaseDetection(String diseaseId, String diseaseName, String severity, String similarity) {
        System.out.println("\n  ENFERMEDAD DETECTADA ");
        System.out.println("ID de Enfermedad: " + diseaseId);
        System.out.println("Nombre de Enfermedad: " + diseaseName);
        System.out.println("Nivel de Severidad: " + severity + "/10");
        System.out.println("Similitud de Secuencia: " + similarity);
        System.out.println("-".repeat(40));
    }
}
