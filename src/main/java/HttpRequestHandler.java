import java.io.*;
import java.net.Socket;

public class HttpRequestHandler extends Thread {
    private final Socket clientSocket;

    public HttpRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
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
            if (!target.startsWith("/")) {
                sendResponse(clientOutputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
                return;
            }

            String response = handleRequest(target, clientInputStream);
            sendResponse(clientOutputStream, response);

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    private String handleRequest(String target, BufferedReader clientInputStream) throws IOException {
        if ("/".equals(target)) {
            return "HTTP/1.1 200 OK\r\n\r\n";
        }

        String[] pathParts = target.split("/");
        if (pathParts.length < 2) {
            return "HTTP/1.1 404 Not Found\r\n\r\n";
        }

        String pathType = pathParts[1];
        String response;
        switch (pathType) {
            case "echo":
                if (pathParts.length >= 3) {
                    String content = pathParts[2];
                    response = String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                            content.length(), content);
                } else {
                    response = "HTTP/1.1 400 Bad Request\r\n\r\n";
                }
                break;

            case "user-agent":
                String userAgentHeader = getHeader(clientInputStream, "User-Agent");
                if (userAgentHeader != null) {
                    String content = userAgentHeader.substring("User-Agent: ".length());
                    response = String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                            content.length(), content);
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
                break;

            default:
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
                break;
        }

        return response;
    }

    private String getHeader(BufferedReader clientInputStream, String headerName) throws IOException {
        String line;
        while ((line = clientInputStream.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith(headerName)) {
                return line;
            }
        }
        return null;
    }

    private void sendResponse(BufferedWriter clientOutputStream, String response) throws IOException {
        clientOutputStream.write(response);
        clientOutputStream.flush();
    }
}
