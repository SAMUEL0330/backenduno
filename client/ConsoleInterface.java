import java.io.*;
import java.util.Scanner;

public class ConsoleInterface {
    private Scanner scanner;
    
    public ConsoleInterface() {
        this.scanner = new Scanner(System.in);
    }
    
    public void displayWelcome() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("    GENOMIC DATA PROCESSING CLIENT");
        System.out.println("    Secure SSL/TLS Connection Established");
        System.out.println("=".repeat(60));
    }
    
    public int showMainMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Create New Patient");
        System.out.println("2. Get Patient Information");
        System.out.println("3. Update Patient");
        System.out.println("4. Delete Patient");
        System.out.println("5. Submit FASTA File");
        System.out.println("6. Test Connection");
        System.out.println("0. Exit");
        System.out.print("\nSelect an option: ");
        
        try {
            return Integer.parseInt(scanner.nextLine().trim());
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
        return response.equals("y") || response.equals("yes");
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
        if (patientData.length >= 11) {
            System.out.println("\n--- PATIENT INFORMATION ---");
            System.out.println("Patient ID: " + patientData[1]);
            System.out.println("Full Name: " + patientData[2]);
            System.out.println("Document ID: " + patientData[3]);
            System.out.println("Contact Email: " + patientData[4]);
            System.out.println("Registration Date: " + patientData[5]);
            System.out.println("Age: " + patientData[6]);
            System.out.println("Sex: " + patientData[7]);
            System.out.println("Clinical Notes: " + patientData[8]);
            System.out.println("FASTA Checksum: " + (patientData[9].isEmpty() ? "Not submitted" : patientData[9]));
            System.out.println("File Size: " + patientData[10] + " bytes");
            System.out.println("-".repeat(30));
        }
    }
    
    public void displayDiseaseDetection(String diseaseId, String diseaseName, String severity, String similarity) {
        System.out.println("\n⚠️  DISEASE DETECTED ⚠️");
        System.out.println("Disease ID: " + diseaseId);
        System.out.println("Disease Name: " + diseaseName);
        System.out.println("Severity Level: " + severity + "/10");
        System.out.println("Sequence Similarity: " + similarity);
        System.out.println("-".repeat(40));
    }
}
