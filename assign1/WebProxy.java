/**
 * WebProxy Class
 * 
 * @author     Marcus Tang 10086730
 * @version     1.0, 20 Jan 2017
 *
 * This program will get the request from the client, and 
 * check to see if the url path exists in the local cache.
 * The program will attempt to stream the response back, but 
 * nothing happens. Instead it says gives a proxy error on client 
 * side. Not sure why the output stream doesn't work. This assignment was
 * done on Windows, and very occasionally some text will pop up on the client 
 * side 
 *
 */

import java.io.*;
import java.io.File;
import java.net.*;
import java.util.*;

public class WebProxy {
	HashMap<String, String> hmap = new HashMap<String, String>();

	ServerSocket serverSocket = null;
	
	Socket server = null;
	Socket psocket;
	
	String input;
	
	 /*
       Constructor that initalizes the server listenig port
       @param port      
	   Proxy server listening port
     */
	public WebProxy(int port) {
          
	/* Intialize server listening port */
		  try
		  {
			 // Wait for connection on port 8888
			 System.out.println("Waiting for a connection.");
			 serverSocket = new ServerSocket(port);
			 
		  }
		  catch (Exception e)
		  {
		     // If any exception occurs, display it
		     System.out.println("Error " + e);
			 e.printStackTrace();
		  }

	}

    // Where the proxy server begins
	public void start(){  
        Scanner fromClient = null;
	    PrintWriter toClient = null;	
		
		try{
		
		Socket psocket = serverSocket.accept();
		
		
		// Connection made, set up streams
		fromClient = new Scanner(new InputStreamReader(psocket.getInputStream()));
	 	toClient = new PrintWriter(new DataOutputStream(psocket.getOutputStream()));
	
		

	 	// Read a line from the client
		  input = fromClient.nextLine();
		  System.out.println(input);
		  String[] urlParse = input.split("\\ ", 3);
		  System.out.println(urlParse[1]);
		  
		  
	
        // If the request is a GET, then the url is broken up and
		//placed into the hash map
		 if(urlParse[0].equals("GET")){
		    hmap.put(urlParse[0], urlParse[1]);
			input = fromClient.nextLine();
			//splits the url where ever the colon is, and placing
			//those values into the hash map
			while(!input.isEmpty()){
				urlParse = input.split(": ", 2);
				hmap.put(urlParse[0], urlParse[1]);
				System.out.println(input);
				input = fromClient.nextLine();
			}
			
		  // Prints off and exits system if the request is not GET
	    }else{
			     System.out.println("400 Bad Request");
				 System.exit(1);
				 toClient.println("400 Bad Request\r\n");			 
				 toClient.flush();
		}
		 
		}catch (Exception e){
			System.out.println("test error" + e);
		}
		
     }
/* this will check to see if a local file was created for the local cache.
* if not, it will create a directory and can save the text file from the test
* url given.
*/

public void accessFile(){
	PrintWriter sendResponse = null;
	OutputStream sendFile = null;
	
	String currentDirectory = System.getProperty("user.dir");
	String url = hmap.get("GET");
	String[] parse = url.split("http:/");
	String filePath = currentDirectory.concat(parse[1]);
	String response;
	String httpRequest;
	String requestHeader1;
	String hostHeader2;
	
	int fileSize;
	byte[] fileObject;
	
	File file = new File(filePath);
	
	//Checks to see if user has previously been to a site
	if(file.exists() && !file.isDirectory()){
		System.out.print("\nFile exists in cache.\n");
		
		fileSize = (int) file.length();
		
		response = "HTTP/1.1 200 OK \r\n" + 
	    "Connection: close \r\n" + 
	    "Content-Length: " + fileSize + "\r\n\r\n";
	
	    System.out.println(response);
		
	    // Attempts to stream files to the browser from local cache 
		//(unsuccessful unfortunately) Else makes a call to the orgin server
		// to get files it needs.
	    FileInputStream transfer = null;
	    try{
		   Socket psocket = serverSocket.accept();
		   transfer = new FileInputStream(file);
		   fileObject = new byte[fileSize];
		   transfer.read(fileObject);
		   
		   sendResponse = new PrintWriter(new DataOutputStream(psocket.getOutputStream()));
		   sendFile = psocket.getOutputStream();
		   
		   sendResponse.write(response);
		   sendResponse.flush();
	    }
		
		catch(Exception e){
			System.out.println("error" + e);
		}
		finally{
			try{
				if(transfer != null){
					transfer.close();
				}
			}catch(Exception e){
				System.out.println("error" + e);
			}
		}
	}
	else{
		System.out.println("\nFile doesn't exist in cache, sending a request to server.\n");
		file.getParentFile().mkdirs();
		
		requestHeader1 = "GET " + hmap.get("GET") + " HTTP/1.1" + "\r\n";
		hostHeader2 = "Host: " + hmap.get("Host") + "\r\n";
		httpRequest = requestHeader1 + hostHeader2 + "\r\n";
				
		System.out.println(httpRequest);
		System.out.println(filePath);
		makeServerRequest(httpRequest, filePath);
	}
	
}

//Orgin server that asks for files to send to the clients browser
public void makeServerRequest(String request, String filePath){
		
		
		Scanner serverResponse;		//serverresponse will have the reponse to read from the orgin server
		PrintWriter serverRequest;	//serverrequest will send a request to the origin server
		
		
		byte[] respHeadBytes = new byte[2048];	//Stores the headers of the url	
		byte[] fileObject;						//Stores the object file needed for the transfer
		
		String httpResponse = "";				//Stores the http response
		String[] parseParts;					//Stores the different parts of the url
		
		int fileSize;							//file size of the object
		int offset = 0;							//Offset used to iterate through stream
		char currentChar;						//We iterate through each byte and covert it to a character
		
		try{
			
			Socket psocket = serverSocket.accept();
			
			// A socket to connect to the orgin server, taking the host as the key, and
			//the hash map value is an instance variable for the socket 
			server = new Socket(hmap.get("Host"),80);
			
			// Create one input stream: origin request, to read http response
			serverRequest = new PrintWriter(new DataOutputStream(server.getOutputStream()));

			// Create two output streams: sends http response back to the clients browser
			PrintWriter sendResponse = new PrintWriter(new DataOutputStream(psocket.getOutputStream()));
			// stream sends out file object
			OutputStream sendFile = psocket.getOutputStream();
				
			// Send httpRequest to origin sever
			serverRequest.write(request);
			serverRequest.flush();
			
			// keeps reading the http response until it encounters \r\n\r\n which seperates the header from the file object
			while(!httpResponse.contains("\r\n\r\n")){
				server.getInputStream().read(respHeadBytes, offset, 1);
				currentChar = (char) respHeadBytes[offset++];
				httpResponse += currentChar;
			};
			
			System.out.println(httpResponse);
			
			// parses through the request to find the file size 
			parseParts = httpResponse.split("Content-Length: ");
			parseParts = parseParts[1].split("\r\n");
			

			// Convert file size string to integer to create the size of the byte array
			fileSize = Integer.parseInt(parseParts[0]);
			// Create the byte array which contains our file
			fileObject = new byte[fileSize];
			offset = 0;
			// Read the input stream which contains the rest of the file into the byte array
			
			while(offset != fileSize){
				server.getInputStream().read(fileObject, offset, 1);
				offset++;
			}
			// Use the byte array and file path from before to create it in cache
			FileOutputStream stream = new FileOutputStream(filePath);
			try {
				stream.write(fileObject);
			
			}
			finally{
				stream.close();
			}
			
			//Send the reponse back to the client
			sendResponse.write(httpResponse);
			sendResponse.flush();
			//Send file object to client's browsers
			sendFile.write(fileObject);
			sendFile.flush();
			
		}catch(IOException e){
			e.printStackTrace();
		}

	}
	



/**
 * A simple test driver
*/
	public static void main(String[] args) {

                String server = "localhost"; // webproxy and client runs in the same machine
                int server_port = 0;
		try {
                // check for command line arguments
                	if (args.length == 1) {
                        	server_port = Integer.parseInt(args[0]);
                	}
                	else {
                        	System.out.println("wrong number of arguments, try again.");
                        	System.out.println("usage: java WebProxy port");
                        	System.exit(0);
                	}


                	WebProxy proxy = new WebProxy(server_port);

                	System.out.printf("Proxy server started...\n");
                	proxy.start();
					proxy.accessFile();
        	} catch (Exception e)
		{
			System.out.println("Exception in main: " + e.getMessage());
                        e.printStackTrace();
	
		}
		
	}
}
