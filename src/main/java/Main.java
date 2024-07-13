import javax.print.DocFlavor;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {

      try{
          ServerSocket serverSocket = new ServerSocket(4221);
          serverSocket.setReuseAddress(true);
          while(true){
              Socket clientSocket = serverSocket.accept();
              System.out.println("accepted new connection");
              new Thread(new HttpRequestHandler(clientSocket)).start();
              System.out.println("Success");
          }

      }catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
      }
  }
}

