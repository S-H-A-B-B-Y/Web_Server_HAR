package HarServer;

import jakarta.servlet.ServletContext;

import jakarta.servlet.ServletException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;

@WebServlet("/SensorDataServlet")
public class SensorDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SOURCE_FOLDER_LOCATION = "C:\\HAR_Server_V1\\Instance Data";
    private static volatile boolean serverRunning = false;
    private volatile boolean acceptingConnections = true;

    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public static String activityName="";
    
    private Map<String, Thread> clientThreads;
    
    // RabbitMQ connection and channel
    private static ConnectionFactory factory;
    private static Connection connection;
    private static Channel publishChannel;
    private static Channel consumeChannel;
    private static String queueName = "AMQP";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public SensorDataServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        serverRunning=true;
        clientThreads =new HashMap<String,Thread>();
        // Initialize RabbitMQ connection and channel
        try {
            factory = new ConnectionFactory();
            factory.setHost("localhost"); // Set RabbitMQ server hostname
            connection = factory.newConnection();
            // Publish channel
            publishChannel = connection.createChannel();
            publishChannel.queueDeclare(queueName, false, false, false, null);
            
            // Consume channel
            consumeChannel = connection.createChannel();
            consumeChannel.queueDeclare(queueName, false, false, false, null);
            
            // Start the consumer thread
            new Thread(new MessageConsumer()).start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        startServer();
    }
    @Override
    public void destroy(){
        super.destroy();
        // Close RabbitMQ connection and channel
        try {
        	 if (publishChannel != null) {
                 publishChannel.close();
             }
             if (consumeChannel != null) {
                 consumeChannel.close();
             }
             if (connection != null) {
                 connection.close();
             }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopServer();
    }
    private void startServer() {
    	new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);
                System.out.println("Server started on port 12345");
                while (serverRunning) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    clientThreads.put(clientAddress, clientThread);
                }
            } catch (IOException e) {
                if (serverRunning) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopServer() {
    	 serverRunning = false;
         try {
             serverSocket.close();
             for (Thread thread : clientThreads.values()) {
                 thread.join();
             }
             System.out.println("Server stopped");
         } catch (IOException | InterruptedException e) {
             e.printStackTrace();
         }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
       
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
 
    	ServletContext context = getServletContext();

        // Check if the attribute already exists
    	activityName = (String) context.getAttribute("activity");
    	
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

            String line;
            try {
            while ((line = reader.readLine()) != null) {
                System.out.println("Received data from " + clientSocket.getInetAddress() + ": " + line);
                
                // Publish the received data to the queue
                publishChannel.basicPublish("", queueName, null, line.getBytes());

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

        }
        private String[] extractSensorValues(String data) {
            Pattern pattern = Pattern.compile("Sensor Type: \\w+: (-?\\d+\\.\\d+), (-?\\d+\\.\\d+), (-?\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
            } else {
                return new String[]{"0", "0", "0"}; // Default values
            }
        }
    }
    
    private static class MessageConsumer implements Runnable {
        @Override
        public void run() {
            try {
                // Create a consumer
                Consumer consumer = new DefaultConsumer(consumeChannel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope,
                                               AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String message = new String(body, "UTF-8");
                        System.out.println("Data from Queue: "+message);
                        processMessage(message);
                    }
                };
                
                // Start consuming messages from the queue
                consumeChannel.basicConsume(queueName, true, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void processMessage(String message) {
        // Extract information from the message
        String source = extractSource(message);
        String userId = extractUserId(message);
        String sensorType = extractSensorType(message);
        String timestamp = extractTimestamp(message);
        
        // Build directory structure
        String directoryPath = getFolderPath(activityName, source, userId, sensorType, timestamp);

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
        saveDataToFile(directoryPath, userId, sensorType, timestamp, message);
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

    private static String getFolderPath(String activityName, String source, String userId, String sensorType, String timestamp) {
       
    	if(activityName==null)
    		{
    		return SOURCE_FOLDER_LOCATION + "\\" + source + "\\" + userId + "_" + sensorType;
    		}
    	else
    	{
    		return SOURCE_FOLDER_LOCATION + "\\" + activityName + "\\" + source + "\\" + userId + "_" + sensorType;
    	}
    	
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

/*Graph logic
 
  // Check if it's one of the required sensors (you can modify this condition accordingly)
                if ("ACCELEROMETER".equals(sensorType) || "GYROSCOPE".equals(sensorType) || "MAGNETOMETER".equals(sensorType)) {
                    // Extract x, y, z values
                    //String[] values = extractSensorValues(line);

                    // Store the data in memory
                    //SensorDataEntry sensorDataEntry = new SensorDataEntry(userId, sensorType, values[0], values[1], values[2], timestamp);
                    //sensorDataList.add(sensorDataEntry);
                	BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\HAR_Server_V1\\Graphs\\"+userId+".csv", true)); // Append to the file

                    // Write data to the file as-is
                    writer.write(line);
                    writer.newLine();
                    writer.close();
                	
                }
                
                private static class SensorDataEntry {
        private String userId;
        private String sensorType;
        private String x;
        private String y;
        private String z;
        private String timestamp;

        public SensorDataEntry(String userId, String sensorType, String x, String y, String z, String timestamp) {
            this.userId = userId;
            this.sensorType = sensorType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
           private static List<SensorDataEntry> sensorDataList = new ArrayList<>();
                if ("/SensorDataServlet".equals(request.getServletPath())) {
            // Handle GET requests to /SensorDataServlet
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Convert the sensorDataList to JSON using Gson library
            String jsonData = new Gson().toJson(sensorDataList);

            // Write the JSON data to the response
            response.getWriter().write(jsonData);
        } else {
}
 */
