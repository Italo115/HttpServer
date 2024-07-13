import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
        serverSocket = new ServerSocket(4221);
        serverSocket.setReuseAddress(true);
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        InputStream input = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();
        System.out.println(line);
        String[] HttPRequest = line.split(" ", 0);
        OutputStream output = clientSocket.getOutputStream();

        if (HttPRequest[1].equals("/"))
            output.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());

        else if (HttPRequest[1].startsWith("/echo/"))
            clientSocket.getOutputStream().write(String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            HttPRequest[1].substring(6).length(), HttPRequest[1].substring(6)).getBytes());
        else if (HttPRequest[1].equals("user-agent"))
        {
            reader.readLine();
            output.write(String.format("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %s\r\n\r\n%s\r\n"
                    , reader.readLine().split("\\s+")[1].length(), reader.readLine().split("\\s+")[1]).getBytes());
        }
        else
            output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());

        System.out.println("accepted new connection");
    } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
    }
  }
}

