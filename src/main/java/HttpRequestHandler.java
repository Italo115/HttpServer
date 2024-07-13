import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private final String fileDir;

    public RequestHandler(Socket clientSocket, String fileDir) {
        this.clientSocket = clientSocket;
        this.fileDir = (fileDir == null) ? "" : fileDir + File.separator;
    }

    @Override
    public void run() {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String requestLine = bufferedReader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendBadRequest(outputStream);
                return;
            }

            Map<String, String> requestHeaders = readHeaders(bufferedReader);
            String body = readBody(bufferedReader);

            String[] requestLinePieces = requestLine.split(" ", 3);
            String httpMethod = requestLinePieces[0];
            String requestTarget = requestLinePieces[1];

            if ("POST".equals(httpMethod)) {
                handlePostRequest(requestTarget, body, outputStream);
            } else {
                handleGetRequest(requestTarget, requestHeaders, outputStream);
            }

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

    private Map<String, String> readHeaders(BufferedReader bufferedReader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String header;
        while (!(header = bufferedReader.readLine()).isEmpty()) {
            String[] keyVal = header.split(":", 2);
            if (keyVal.length == 2) {
                headers.put(keyVal[0].trim(), keyVal[1].trim());
            }
        }
        return headers;
    }

    private String readBody(BufferedReader bufferedReader) throws IOException {
        StringBuilder bodyBuffer = new StringBuilder();
        while (bufferedReader.ready()) {
            bodyBuffer.append((char) bufferedReader.read());
        }
        return bodyBuffer.toString();
    }

    private void handlePostRequest(String requestTarget, String body, OutputStream outputStream) throws IOException {
        if (requestTarget.startsWith("/files/")) {
            File file = new File(fileDir + requestTarget.substring(7));
            if (file.createNewFile()) {
                try (FileWriter fileWriter = new FileWriter(file)) {
                    fileWriter.write(body);
                }
                sendResponse(outputStream, "HTTP/1.1 201 Created\r\n\r\n");
            } else {
                sendNotFound(outputStream);
            }
        } else {
            sendNotFound(outputStream);
        }
    }

    private void handleGetRequest(String requestTarget, Map<String, String> headers, OutputStream outputStream) throws IOException {
        boolean acceptGzip = false;
        if (headers.containsKey("Accept-Encoding")) {
            String[] encodings = headers.get("Accept-Encoding").split(",");
            for (String encoding : encodings) {
                if (encoding.trim().equalsIgnoreCase("gzip")) {
                    acceptGzip = true;
                    break;
                }
            }
        }

        if (requestTarget.equals("/")) {
            sendResponse(outputStream, "HTTP/1.1 200 OK\r\n\r\n");
        } else if (requestTarget.startsWith("/echo/")) {
            String echoString = requestTarget.substring(6);
            if (acceptGzip) {
                sendGzipResponse(outputStream, echoString, "text/plain");
            } else {
                sendResponse(outputStream, "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + echoString.length() + "\r\n\r\n" + echoString);
            }
        } else if (requestTarget.equals("/user-agent")) {
            String userAgent = headers.getOrDefault("User-Agent", "");
            if (acceptGzip) {
                sendGzipResponse(outputStream, userAgent, "text/plain");
            } else {
                sendResponse(outputStream, "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + userAgent.length() + "\r\n\r\n" + userAgent);
            }
        } else if (requestTarget.startsWith("/files/")) {
            handleFileRequest(requestTarget.substring(7), outputStream, acceptGzip);
        } else {
            sendNotFound(outputStream);
        }
    }

    private void handleFileRequest(String fileName, OutputStream outputStream, boolean acceptGzip) throws IOException {
        Path filePath = Paths.get(fileDir, fileName);
        if (Files.exists(filePath)) {
            byte[] fileBytes = Files.readAllBytes(filePath);
            if (acceptGzip) {
                sendGzipResponse(outputStream, new String(fileBytes), "application/octet-stream");
            } else {
                sendResponse(outputStream, "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + fileBytes.length + "\r\n\r\n");
                outputStream.write(fileBytes);
            }
        } else {
            sendNotFound(outputStream);
        }
    }

    private void sendGzipResponse(OutputStream outputStream, String content, String contentType) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(content.getBytes());
        }
        byte[] gzipContent = byteArrayOutputStream.toByteArray();
        String response = "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: " + contentType + "\r\nContent-Length: " + gzipContent.length + "\r\n\r\n";
        outputStream.write(response.getBytes());
        outputStream.write(gzipContent);
    }

    private void sendResponse(OutputStream outputStream, String response) throws IOException {
        outputStream.write(response.getBytes());
        outputStream.flush();
    }

    private void sendBadRequest(OutputStream outputStream) throws IOException {
        sendResponse(outputStream, "HTTP/1.1 400 Bad Request\r\n\r\n");
    }

    private void sendNotFound(OutputStream outputStream) throws IOException {
        sendResponse(outputStream, "HTTP/1.1 404 Not Found\r\n\r\n");
    }
}
