import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final int PORT = 4221;

    public static void main(String[] args) {
        if (args.length != 2 || !"--directory".equals(args[0])) {
            System.out.println("Usage: java MainServer --directory <path_to_directory>");
            return;
        }

        String directory = args[1];

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new HttpRequestHandler(clientSocket, directory).start();
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }
}
