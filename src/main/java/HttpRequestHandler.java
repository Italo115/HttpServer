import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpRequestHandler implements Runnable {
    private final Socket clientSocket;
    private final String directory;

    public HttpRequestHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendBadRequest(outputStream);
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendBadRequest(outputStream);
                return;
            }

            String path = requestParts[1];
            String userAgent = "";
            String line;
            while (!(line = reader.readLine()).equals("")) {
                if (line.startsWith("User-Agent: ")) {
                    userAgent = line.substring(12);
                }
            }

            if (path.startsWith("/files/")) {
                handleFileRequest(path, outputStream);
            } else if (path.startsWith("/user-agent")) {
                handleUserAgentRequest(userAgent, outputStream);
            } else if (path.startsWith("/echo/")) {
                handleEchoRequest(path, outputStream);
            } else if (path.equals("/")) {
                sendOkResponse(outputStream);
            } else {
                sendNotFound(outputStream);
            }

            outputStream.flush();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private void handleFileRequest(String path, OutputStream outputStream) throws IOException {
        String fileName = path.substring(7);
        Path filePath = Paths.get(directory, fileName);
        if (Files.exists(filePath)) {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
                    fileBytes.length + "\r\n\r\n";
            outputStream.write(response.getBytes());
            outputStream.write(fileBytes);
        } else {
            sendNotFound(outputStream);
        }
    }

    private void handleUserAgentRequest(String userAgent, OutputStream outputStream) throws IOException {
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                userAgent.length() + "\r\n\r\n" + userAgent;
        outputStream.write(response.getBytes());
    }

    private void handleEchoRequest(String path, OutputStream outputStream) throws IOException {
        String randomString = path.substring(6);
        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                randomString.length() + "\r\n\r\n" + randomString;
        outputStream.write(response.getBytes());
    }

    private void sendOkResponse(OutputStream outputStream) throws IOException {
        outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
    }

    private void sendBadRequest(OutputStream outputStream) throws IOException {
        outputStream.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
        outputStream.flush();
    }

    private void sendNotFound(OutputStream outputStream) throws IOException {
        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        outputStream.flush();
    }
}
