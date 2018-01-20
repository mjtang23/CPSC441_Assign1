import java.io.OutputStream;
import java.net.ServerSocket;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.io.File;

/*
 * 
 * @author Brian Nguyen
 * @version 1.0
 * 
 * README
 * Just a heads up!
 * 
 * This seems to work best when used with firefox with private browsing.
 * There are some times where the program throws at Exception in main: No line found
 * for some reason when there is an issue with the actual browser cache.
 * 
 * Before running the program, I had to clear the browser cache first
 * to prevent any weird browser anomalies and secondly I ran it in private browsing.
 * 
 * 
 */




public class WebProxyHelp {

	HashMap<String, String> hm = new HashMap<String, String>();

	ServerSocket proxyServer;
    Socket proxySocket;
    
    Socket originServer;

    int port;
    
    public WebProxyHelp(int port) {

        this.port = port;


    }

    public void start() {

        createProxyServer(port);
        retrieveFile();
		System.out.println("Closing connection");
    }

    public void createProxyServer(int port){

        Scanner inputStream;
        PrintWriter outputStream;

        String header;
        String[] parseParts;

        try{
			
			//This creates the server
            proxyServer = new ServerSocket(port);
            proxySocket = proxyServer.accept();

            inputStream = new Scanner(new InputStreamReader(proxySocket.getInputStream()));
            outputStream = new PrintWriter(new DataOutputStream(proxySocket.getOutputStream()));

			//Obtain the HTTP method
            header = inputStream.nextLine();
            System.out.println(header);

            parseParts = header.split(" ",3);


			//This checks if it is a GET request
            if(!parseParts[0].equals("GET")){
                System.out.println("400 Bad Request");
                System.exit(1);
                outputStream.println("400 Bad Request");
                outputStream.flush();
            }

			//Put string into hashmap for further parsing
            hm.put(parseParts[0],parseParts[1]);
            header = inputStream.nextLine();


			//This iterates through the header
            while(!header.isEmpty()){

                parseParts = header.split(": ",2);
                hm.put(parseParts[0],parseParts[1]);

               System.out.println(header);
                header = inputStream.nextLine();

            }

        }
        catch(IOException e){
            e.printStackTrace();
        }
    }


	public void retrieveFile(){
	
			String currentDirectory = System.getProperty("user.dir");	
			String url = hm.get("GET");
			String[] parseParts = url.split("http:/");
			String filePath = currentDirectory.concat(parseParts[1]);
			
			
			String httpRequest;
			String requestHeader1;
			String requestHeader2;
			
			String httpResponse;
			String httpOk;
			String httpClose;
			String httpFileSize;
			int fileSize;
			byte[] fileObject;
			
			File file = new File(filePath);
			
			//Check if file exists
			
			if(file.exists() && !file.isDirectory()){
				System.out.println("\nFile exists in cache.");	
				
				fileSize = (int) file.length();
				
				//Assemble response
				httpOk = "HTTP/1.1 200 OK\r\n";
				httpClose = "Connection: close\r\n";
				httpFileSize = "Content-Length: " + fileSize + "\r\n\r\n";
				
				httpResponse = httpOk + httpClose + httpFileSize;
				System.out.println(httpResponse);
				
				
				//Credit: https://examples.javacodegeeks.com/core-java/io/fileinputstream/read-file-in-byte-array-with-fileinputstream/
				//I used this online tutorial to read a file given a path into a byte array.
				//I used this byte array to send the object file to the browser acting as the client.
				FileInputStream fin = null;
				try{
					fin = new FileInputStream(file);					
					fileObject = new byte[fileSize];
					fin.read(fileObject);
					
					PrintWriter sendHttpResponse = new PrintWriter(new DataOutputStream(proxySocket.getOutputStream()));
					OutputStream sendHttpFile = proxySocket.getOutputStream();
				
					sendHttpResponse.write(httpResponse);
					sendHttpResponse.flush();
			
					sendHttpFile.write(fileObject);
					sendHttpFile.flush();
					
					
				}
				catch(IOException e){
					System.out.println("Exception when reading file");
				}
				finally{
					try{
						if(fin != null){
							fin.close();
						}
					}
					catch(IOException ioe){
						System.out.println("Error while closing stream");
					}
				}
				
					
			}
			else{
				
				System.out.println("\nFile does not exist in cache, sending request to origin server.\n");
				file.getParentFile().mkdirs();
				
				
				//Assemble HTTP request
				//Create socket
				//Send request using origin server socket
				//Retrieve response from origin server input stream
				//Parse respone for header and file
				//Save file in cache & assemble response
				
				requestHeader1 = "GET " + hm.get("GET") + " HTTP/1.1" + "\r\n";
				requestHeader2 = "Host: " + hm.get("Host") + "\r\n";
				httpRequest = requestHeader1 + requestHeader2 + "\r\n";
				
				System.out.println(httpRequest);
				originServerFetch(httpRequest, filePath);
			}
	}
	
	public void originServerFetch(String httpRequest, String filePath){
		
		
		Scanner originResponse;		//Origin response is used to read the Http Response from Origin Server
		PrintWriter originRequest;	//Origin request is used to send the http request to the Origin server
		
		
		byte[] respHeadBytes = new byte[2048];	//This is used to store the header file of the http response	
		byte[] fileObject;						//This is used to store the file object you are downloading
		
		String httpResponse = "";				//This is used to store the http response
		String[] parseParts;					//This is used to obtain the file size
		
		int fileSize;							//We find the fileSize by parsing the http response, we use it to grab the file object
		int offset = 0;							//Offset used to iterate through stream
		char currentChar;						//We iterate through each byte and covert it to a character
		
		try{
			//Basically just create a socket to the origin server, I pull the host from the hash map, the hash map is an instance variable
			originServer = new Socket(hm.get("Host"),80);
			
			//Create one input stream: origin request, to read http response
			originRequest = new PrintWriter(new DataOutputStream(originServer.getOutputStream()));
			//Create two output streams, one is to send http response back to browser
			PrintWriter sendHttpResponse = new PrintWriter(new DataOutputStream(proxySocket.getOutputStream()));
			//This output stream is used to send file object
			OutputStream sendHttpFile = proxySocket.getOutputStream();
				
			//Send httpRequest to origin sever
			originRequest.write(httpRequest);
			originRequest.flush();
			
			
			

			//Basically keeps reading the http response until it encounters \r\n\r\n which seperates the header from the file object
			while(!httpResponse.contains("\r\n\r\n")){
				originServer.getInputStream().read(respHeadBytes, offset, 1);
				currentChar = (char) respHeadBytes[offset++];
				httpResponse += currentChar;
			};
			
			System.out.println(httpResponse);
			
			//Basically finding the file size through parsing
			parseParts = httpResponse.split("Content-Length: ");
			parseParts = parseParts[1].split("\r\n");
			

			//Convert file size string to integer
			fileSize = Integer.parseInt(parseParts[0]);
			//Create the byte array which contains our file
			fileObject = new byte[fileSize];
			offset = 0;
			//Read the input stream which contains the rest of the file into the byte array
			while(offset != fileSize){
				originServer.getInputStream().read(fileObject, offset, 1);
				offset++;
			}
			//Use the byte array and file path from before to create it in cache
			FileOutputStream stream = new FileOutputStream(filePath);
			try {
				stream.write(fileObject);
			}
			finally{
				stream.close();
			}
			
			//Send httpResponse to browser client
			sendHttpResponse.write(httpResponse);
			sendHttpResponse.flush();
			//Send file object to browser client
			sendHttpFile.write(fileObject);
			sendHttpFile.flush();
			
		}
		catch(IOException e){
			e.printStackTrace();
		}

	}

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
                    System.out.println("\n");
                    proxy.start();
            } catch (Exception e)
        {
            System.out.println("Exception in main: " + e.getMessage());
                        e.printStackTrace();

        }

    }



}
