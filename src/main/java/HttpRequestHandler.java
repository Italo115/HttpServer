import java.io.*;
import java.net.Socket;

public class HttpRequestHandler extends Thread {
    private final Socket clientSocket;
    private final String directory;

    public HttpRequestHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    public void run() {
        try (BufferedReader clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter clientOutputStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String requestLine = clientInputStream.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendResponse(clientOutputStream, "HTTP/1.1 400 Bad Request\r\n\r\n");
                return;
            }

            String[] requestLineParts = requestLine.split(" ");
            if (requestLineParts.length < 2) {
                sendResponse(clientOutputStream, "HTTP/1.1 400 Bad Request\r\n\r\n");
                return;
            }

            String target = requestLineParts[1];
            if (!target.startsWith("/files/")) {
                sendResponse(clientOutputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
                return;
            }

            String filename = target.substring("/files/".length());
            File file = new File(directory, filename);

            if (file.exists() && !file.isDirectory()) {
                sendFileResponse(clientOutputStream, file);
            } else {
                sendResponse(clientOutputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    private void sendFileResponse(BufferedWriter clientOutputStream, File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] fileContent = fileInputStream.readAllBytes();
            String responseHeader = String.format(
                    "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: %d\r\n\r\n",
                    fileContent.length
            );
            clientOutputStream.write(responseHeader);
            clientOutputStream.write(new String(fileContent));
            clientOutputStream.flush();
        }
    }

    private void sendResponse(BufferedWriter clientOutputStream, String response) throws IOException {
        clientOutputStream.write(response);
        clientOutputStream.flush();
    }
}
