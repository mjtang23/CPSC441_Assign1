import java.util.Scanner;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;

public class Server1 {

	public static void main(String[] args) {
		
		  String s;
		  Scanner inputStream = null;
		  PrintWriter outputStream = null;
		  ServerSocket serverSocket = null;
		  try
		  {
			 // Wait for connection on port 6789
			 System.out.println("Waiting for a connection.");
			 serverSocket = new ServerSocket(6789);
			 Socket socket = serverSocket.accept();
			 
			 // Connection made, set up streams
			 inputStream = new Scanner(new InputStreamReader(socket.getInputStream()));
		 	 outputStream = new PrintWriter(new
		 			 DataOutputStream(socket.getOutputStream()));
		 	 
		 	// Read a line from the client
			 s = inputStream.nextLine();
			 System.out.println(s);


			 // Output text to the client
			 outputStream.println(s);
		     outputStream.flush();
		     System.out.println("Closing connection");
		     inputStream.close();
		     outputStream.close();
		  }
		  catch (Exception e)
		  {
		     // If any exception occurs, display it
		     System.out.println("Error " + e);
		  }
		 

	}

}
