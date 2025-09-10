import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Patient {
    private String patientId;
    private String fullName;
    private String documentId;
    private String contactEmail;
    private String registrationDate;
    private int age;
    private String sex;
    private String clinicalNotes;
    private String checksumFasta;
    private int fileSizeBytes;
    private String diseaseId;
    private boolean deleted;
    
    public Patient(String patientId, String fullName, String documentId, String contactEmail, 
                   int age, String sex, String clinicalNotes) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.documentId = documentId;
        this.contactEmail = contactEmail;
        this.age = age;
        this.sex = sex;
        this.clinicalNotes = clinicalNotes;
        this.registrationDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.deleted = false;
        this.fileSizeBytes = 0;
        this.diseaseId = "";
    }
    
    // Constructor for loading from CSV
    public Patient(String patientId, String fullName, String documentId, String contactEmail,
                   String registrationDate, int age, String sex, String clinicalNotes,
                   String checksumFasta, int fileSizeBytes, String diseaseId, boolean deleted) {
        this.patientId = patientId;
        this.fullName = fullName;
        this.documentId = documentId;
        this.contactEmail = contactEmail;
        this.registrationDate = registrationDate;
        this.age = age;
        this.sex = sex;
        this.clinicalNotes = clinicalNotes;
        this.checksumFasta = checksumFasta;
        this.fileSizeBytes = fileSizeBytes;
        this.diseaseId = diseaseId;
        this.deleted = deleted;
    }
    
    // Getters
    public String getPatientId() { return patientId; }
    public String getFullName() { return fullName; }
    public String getDocumentId() { return documentId; }
    public String getContactEmail() { return contactEmail; }
    public String getRegistrationDate() { return registrationDate; }
    public int getAge() { return age; }
    public String getSex() { return sex; }
    public String getClinicalNotes() { return clinicalNotes; }
    public String getChecksumFasta() { return checksumFasta; }
    public int getFileSizeBytes() { return fileSizeBytes; }
    public String getDiseaseId() { return diseaseId; }
    public boolean isDeleted() { return deleted; }
    
    // Setters
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public void setAge(int age) { this.age = age; }
    public void setSex(String sex) { this.sex = sex; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public void setChecksumFasta(String checksumFasta) { this.checksumFasta = checksumFasta; }
    public void setFileSizeBytes(int fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public void setDiseaseId(String diseaseId) { this.diseaseId = diseaseId; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    
    public String toCsvString() {
        return String.join(",",
            escapeCommas(patientId),
            escapeCommas(fullName),
            escapeCommas(documentId),
            escapeCommas(contactEmail),
            escapeCommas(registrationDate),
            String.valueOf(age),
            escapeCommas(sex),
            escapeCommas(clinicalNotes),
            escapeCommas(checksumFasta != null ? checksumFasta : ""),
            String.valueOf(fileSizeBytes),
            escapeCommas(diseaseId != null ? diseaseId : ""),
            String.valueOf(deleted)
        );
    }
    
    private String escapeCommas(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    @Override
    public String toString() {
        return "Patient{" +
                "patientId='" + patientId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", documentId='" + documentId + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                ", age=" + age +
                ", sex='" + sex + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
