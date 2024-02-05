package HarServer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/SensorDataServlet")
public class SensorDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SOURCE_FOLDER_LOCATION = "C:\\HAR_Server_V1\\Instance Data";
    private static volatile boolean serverRunning = false;
    private volatile boolean acceptingConnections = true;

    private ServerSocket serverSocket;
    private ExecutorService executorService;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SensorDataServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        startServer();
    }
    @Override
    public void destroy(){
    	//stopServer();
        super.destroy();
    }
    private synchronized void startServer() {
        if (!serverRunning) {
            executorService = Executors.newFixedThreadPool(10);
            System.out.println("Server running.");
            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(12345);
                    System.out.println("Server listening on port 12345...");

                    // Set the server status to running
                    serverRunning = true;

                    while (acceptingConnections) {
                        try {
                            Socket socket = serverSocket.accept();
                            if (acceptingConnections) {
                                System.out.println("Client connected: " + socket.getInetAddress());

                                // Create a new thread for each client connection
                                executorService.submit(new ClientHandler(socket));
                            } else {
                                // Close the socket if acceptingConnections is set to false
                                socket.close();
                            }
                        } catch (SocketException e) {
                        	// If it's a socket closed exception, exit the loop and thread
                            if (e.getMessage().equalsIgnoreCase("Socket closed")) {
                                break;
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private synchronized void stopServer() {
        if (serverRunning) {
        	 try {
        		 
                 // Set the flag to stop accepting new connections
                 acceptingConnections = false;
                 serverRunning = false;
                 
                 // Attempt to stop all actively executing tasks
                 executorService.shutdownNow();
                 
                 // Wait for the executor service to terminate
                 while (!executorService.isTerminated()) {
                     Thread.sleep(100); // Wait for termination
                 }

                 serverSocket.close();
                 System.out.println("Server stopped.");
             } catch (IOException | InterruptedException e) {
                 e.printStackTrace();
             }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().append("Server Status: ").append(serverRunning ? "Running" : "Not Running");
        //String action = request.getParameter("action");
        //if (action != null && "stop".equals(action)) {
        //    destroy();
        //} 
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                handleClient();
            } catch (IOException e) {
                 e.printStackTrace();
            }
        }

        private void handleClient() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Connected to Client: " + clientSocket.getInetAddress());
            String line;
            try {
            while ((line = reader.readLine()) != null) {
                System.out.println("Received data from " + clientSocket.getInetAddress() + ": " + line);

                // Extract information from the current line
                String source = extractSource(line);
                String userId = extractUserId(line);
                String sensorType = extractSensorType(line);
                String timestamp = extractTimestamp(line);

                // Build directory structure
                String directoryPath = getFolderPath(source, userId, sensorType, timestamp);

                // Create directories if they don't exist
                boolean success = new java.io.File(directoryPath).mkdirs();
                if (success || new java.io.File(directoryPath).exists()) {
                    // Directories were created successfully or already exist
                    System.out.println("Directories exist or were created successfully. " + directoryPath);
                } else {
                    // Failed to create directories
                    System.out.println("Failed to create directories. " + directoryPath);
                    return;
                }

                // Save data to a file in CSV format
                saveDataToFile(directoryPath, userId, sensorType, timestamp, line);
            }
            }catch (SocketException e) {
                // Handle SocketException gracefully
                if (e.getMessage().equalsIgnoreCase("Socket closed")) {
                    System.out.println("Socket closed. Connection closed for " + clientSocket.getInetAddress());
                } else {
                    e.printStackTrace();
                }
            } finally {
                // Close the socket after processing data
                clientSocket.close();
                System.out.println("Connection closed for " + clientSocket.getInetAddress());
            }
            // Close the socket after processing data
            //clientSocket.close();
            //System.out.println("Connection closed for " + clientSocket.getInetAddress());
            //BufferedWriter writer = (new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
            //writer.write(200);
            //writer.close();
        }
    }

    private static void saveDataToFile(String directoryPath, String userId, String sensorType, String timestamp,
            String data) {
        try {
            // Create a file name using user ID and timestamp
            String fileName = userId + "_" + sensorType + ".csv";
            String filePath = directoryPath + "\\" + fileName;
            System.out.println("Opening file at " + filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true)); // Append to the file

            // Write data to the file as-is
            writer.write(data);
            writer.newLine();
            writer.close();

            System.out.println("Data saved to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFolderPath(String source, String userId, String sensorType, String timestamp) {
        // return SOURCE_FOLDER_LOCATION + "\\" + source + "\\" + userId + "_" +
        // sensorType + "_" + timestamp;
        return SOURCE_FOLDER_LOCATION + "\\" + source + "\\" + userId + "_" + sensorType;
    }

    private static String extractSource(String data) {
        Pattern pattern = Pattern.compile("Source: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownSource";
        }
    }

    private static String extractUserId(String data) {
        Pattern pattern = Pattern.compile("User ID: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownUser";
        }
    }

    private static String extractSensorType(String data) {
        Pattern pattern = Pattern.compile("Sensor Type: (\\w+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownSensor";
        }
    }

    private static String extractTimestamp(String data) {
        Pattern pattern = Pattern.compile("Timestamp: (\\d+)");
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "UnknownTimestamp";
        }
    }
}
