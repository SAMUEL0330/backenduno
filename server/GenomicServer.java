import javax.net.ssl.*;  
import java.io.*;                 
import java.security.*;           
import java.util.concurrent.*;   
import java.util.logging.*;       

public class GenomicServer {
    private static final int PORT = 8443;                             
    private static final String KEYSTORE_PATH = "certs/server.jks";   
    private static final String KEYSTORE_PASSWORD = "genomic123";    
    private static final Logger logger = Logger.getLogger(GenomicServer.class.getName()); 
    private SSLServerSocket serverSocket;     
    private ExecutorService threadPool;       
    private PatientManager patientManager;     
    private DiseaseDetector diseaseDetector;   
    private ServerLogger serverLogger;         
    private volatile boolean running = false;  
    
    public GenomicServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.patientManager = new PatientManager();
        this.diseaseDetector = new DiseaseDetector();
        this.serverLogger = new ServerLogger();        
        setupLogging();
    }
    
    private void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler("server/logs/genomic_server.log", true);            
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }
    
    public void start() throws Exception {
        SSLContext sslContext = createSSLContext();
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();        
        serverSocket = (SSLServerSocket) factory.createServerSocket(PORT);        
        serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());        
        running = true;
        logger.info("Genomic Server started on port " + PORT);
        System.out.println("Genomic Server started on port " + PORT);
        diseaseDetector.loadDiseaseDatabase();
        logger.info("Disease database loaded successfully");
        
        while (running) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getRemoteSocketAddress());
                
                threadPool.submit(new ClientHandler(clientSocket, patientManager, diseaseDetector, serverLogger));
            } catch (IOException e) {
                if (running) {
                    logger.severe("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    private SSLContext createSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        
        try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());        
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());        
        SSLContext sslContext = SSLContext.getInstance("TLS");        
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            threadPool.shutdown();            
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            
            logger.info("Genomic Server stopped");
        } catch (Exception e) {
            logger.severe("Error stopping server: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        GenomicServer server = new GenomicServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        try {
            server.start();
        } catch (Exception e) {
            logger.severe("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
