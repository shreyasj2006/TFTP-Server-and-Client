/*
 * TftpClient.java
 *Vversion: 1.0
 *
 * Author: Shreyas Jayanna
 * Date: 09/26/2014
 *
 */

// import Statements
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.net.UnknownHostException;

/**
 * Class TftpClient
 * This class defines and implements a TftpClient
 * The commands implemented by this class are:
 * 	1 connect
 *	2 get
 *	3 quit
 * 	4 ?
 * This class supports OCTET mode only, for file transfers
 */
class TftpClient {

	DatagramSocket socket;			// The datagram socket to connect to the server	
	DatagramPacket sendPacket;		// The datagram packet used to send data
	DatagramPacket receivePacket;		// The datagram packet used to receive data

	InetAddress ipAddress;			// The IP Address of the server, to which the client connects to

	int port;				// The port number of the client

	Hashtable<Integer,String> errorCodes;	// Hashtable to store error codes

	/**
	 * TftpClient method
	 * This is the constructor of the class and it initializes the instance variables
	 *
	 * @param localPort The port ID of the client
	 */
	TftpClient(int localPort) throws Exception{
		this.port = localPort;
		this.socket = new DatagramSocket(localPort);

		this.errorCodes = new Hashtable<Integer,String>();	
		this.setErrorCodes();
	}

	/**
	 * setErrorCodes method
	 * This method stores the error codes in the hashtable
 	 */
	private void setErrorCodes() {
		this.errorCodes.put(0,"Not defined, see error message (if any).");
		this.errorCodes.put(1,"File not found.");
		this.errorCodes.put(2,"Access violation.");
		this.errorCodes.put(3,"Disk full or allocation exceeded.");
		this.errorCodes.put(4,"Illegal TFTP operation.");
		this.errorCodes.put(5,"Unknown transfer ID.");
		this.errorCodes.put(6,"File already exists.");
		this.errorCodes.put(7,"No such user.");
	}

	/**
	 * terminateConnection method
	 * This method closes the socket connection and opens a new connection
	 *
	 * @throws Exception
	 */
	private void terminateConnection() throws Exception {
		this.socket.close();				// Close current socket
		this.socket = new DatagramSocket(this.port);	// Create new socket at the original port
		this.start();
	}

	/**
	 * start method
	 * This method starts the TFTP client
	 *
	 * @throws IOException
	 * @throws Exception
 	 */
	private void start() throws IOException, Exception {
		System.out.print("tftp> ");

		String[] command = this.getCommand(); 
		String[] to = new String[2];

		// If connect command is used without the hostname of the server, 
		// prompt the user to enter the hostname 
		if((command.length == 1) && (command[0].equals("connect"))) {
			System.out.print("(to) ");
			to = this.getCommand();
			if(this.connect(to[0]))
				this.request();
			else
				this.error(to[0] + ": unkown host");
		} else if(command[0].equals("connect")) {
			// If the hostname is provided with the connect command,
			// check if it is a valid host and if not, restart the socket 
			if(this.connect(command[1]))
				this.request();
			else
				this.error(to[0] + ": unkown host");
		} else if(command[0].equals("get")) {
			// If the input is get command, invoke receiveFile method
			this.receiveFile(command);
		} else if(command[0].equals("quit"))
			// If the command is quit, terminate the TFTP client program execution
			System.exit(0);
		else if(command[0].equals("?")) {
			// If the command is ?, print help information
			System.out.println("connect 	connect to remote tftp");
			System.out.println("get     	receive file");
			System.out.println("quit    	exit tftp");
			System.out.println("?       	print help information");
			this.request();
		} else {
			// If the command is none of the above, print error message
			System.out.println("Unrecognized command");
			this.request();
		}
	}

	/**
	 * error method
	 * This method prints the error message and terminates the socket connection
	 * The socket is closed and started again at the same port number
	 *
	 * @param errorMsg The error message to be displayed to the user
	 */
	private void error(String errorMsg) throws Exception {
		System.out.println(errorMsg);
		this.terminateConnection();
	}	

	/**
	 * request method
	 * This method processes the commands entered by the user
	 *
 	 * @throws IOException
	 * @throws Exception
 	 */
	private void request() throws IOException, Exception {
		System.out.print("tftp> ");
		String[] input = this.getCommand();
		String[] to = new String[2];

		// Keep asking for commands from the user till quit is entered
		while(true) {
			if(input[0].equals("get")) {
				// If the command is get, invoke receiveFile method to process 
				// the list and receive the file(s)
				this.receiveFile(input);
				System.out.print("tftp> ");
				input = this.getCommand();
			}
			else if(input[0].equals("?")) {
				// If the command is ?, print the help commands
				System.out.println("connect 	connect to remote tftp");
				System.out.println("get     	receive file");
				System.out.println("quit    	exit tftp");
				System.out.println("?       	print help information");
		
				System.out.print("tftp> ");
				input = this.getCommand();
			} 
			else if(input[0].equals("quit")) {
				// If the command is quit, terminate the execution of the TFTP client program
				System.exit(0);
			} else if((input.length == 1) && (input[0].equals("connect"))) {
				// Process connect commands
				System.out.print("(to) ");
				to = this.getCommand();
				if(this.connect(to[0]))
					this.request();
				else
					this.error(to[0] + ": unkown host");
			} else if(input[0].equals("connect")) {
				if(this.connect(input[1]))
					this.request();
				else
					this.error(to[0] + ": unkown host");
			} else {
				// Any other command(s) other than the ones specified above will
				// be responded with an error message
				System.out.println("Unrecognized command");
				this.request();
			}
		}
		
	}

	/**
	 * receiveFile method
	 * This method receives the list of files which are entered by the author and 
	 * requests those files from the TFTP server
	 *
	 * @param input The string array containing the filenames
	 *
	 * @throws IOException
	 * @throws Exception
	 */
	private void receiveFile(String[] input) throws IOException, Exception {
		// If filenames are not mentioned, prompt the user to enter the file names
		if(input.length == 1) {
			System.out.print("(files) ");
			input = this.getCommand();
		}
		// Get the file(s) list
		String[] fileList = new String[input.length - 1];
		// If connect command was not used, try to extract it from the hostname
		// that preceedes the filename
		if(this.ipAddress == null) {
			String[] host_file = input[1].split(":");
			if(!this.connect(host_file[0])) {
				return;
			}
		}
		// Extract file names to a string array
		for(int i = 0; i < fileList.length; ++i) {
			if(input[i+1].contains(":")) {
				String[] text = input[i+1].split(":");
				fileList[i] = text[1];
			} else
				fileList[i] = input[i+1];
		}
		// For each file, invoke get methdo to get file contents
		int index = 0;
		while(index < fileList.length) {
			this.get(fileList[index++]);
		}
	}

	/**
	 * get method
	 * This method sends a RRQ request to the TFTP server to start the file transfer
	 * 
	 * @param file The filename
	 *
	 * @throws Exception
	 */
	private void get(String file) throws Exception {

		int port = 69; 				// TFTP server's port to receive TFTP requests

		byte[] filename = file.getBytes(); 	// Extract filename to a byte array

		String modeValue = "OCTET";		// Set mode as OCTET
		
		byte[] mode = modeValue.getBytes();	// Convert mode value to a byte array
	
		int filenameSize = filename.length;	
		int modeSize = mode.length;
 
		int totalSize = 4 + filenameSize + modeSize; // Total size of the packet
		
		byte[] buffer = new byte[totalSize];	// Create a byte buffer of required length
		
		// Fill the byte buffer with RRQ request
		Arrays.fill(buffer,0,1,(byte)0);
		Arrays.fill(buffer,1,2,(byte)1);

		for(int j = 0; j < filenameSize; ++j)
			buffer[j+2] = filename[j];
	
		Arrays.fill(buffer,(2+filenameSize),(2+filenameSize+1),(byte)0);
	
		for(int j = 0; j < modeSize; ++j)
			buffer[j+3+filenameSize] = mode[j];
	
		Arrays.fill(buffer,totalSize-1,totalSize,(byte)0);

		// Send the packet to the TFTP server
		this.sendPacket = new DatagramPacket(buffer,buffer.length,this.ipAddress,port);	
		this.socket.send(this.sendPacket);

		// Receive the response from the server
		if(!this.receiveContents(file)) {
			this.socket.send(this.sendPacket);
			if(!this.receiveContents(file))
				this.terminateConnection();
		}
	}

	/**
	 * receiveContents method
	 * This method receives the file data from the server
	 * 
	 * @param file The filename
	 */
	private boolean receiveContents(String file) {
		try {
			// Set timeout for the socket - 3000 ms
			this.socket.setSoTimeout(3000);
			byte[] buffer = new byte[516];
			// receive the data packet from the server
			this.receivePacket = new DatagramPacket(buffer,buffer.length);
			this.socket.receive(this.receivePacket);
	
			byte[] receivedData = this.receivePacket.getData();
			// Extract opcode form the receieved packet
			int opcode = ((int)receivedData[0] << 8) | ((int) receivedData[1]);

			if((opcode == 3) || (opcode == 4))
				// If the opcode is data or ack, invoke getData method
				this.getData(opcode,file);
			if(opcode == 5) {
				// If the received packet is an error message, extract error message and
				// display it to the user and terminate the connection
				int errorCode = ((int)receivedData[2] << 8) | ((int) receivedData[3]);
				String errorMsg = this.errorCodes.get(errorCode);
				byte[] errorByte = new byte[receivedData.length-5];
				for(int i = 0; i < errorByte.length; ++i) {
					errorByte[i] = receivedData[i+4];
				}
				String errorByteMsg = new String(errorByte);
				this.error(errorCode + ": " + errorByteMsg);		
			}	// Catch exceptions
		} catch(SocketTimeoutException e) {
			return false;		
		} catch(SocketException e) {
			return false;
		} catch(Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * getData method
	 * This method receives the rest of the data packets and writes the file data into the file
	 *
	 * @param opcode The opcode received in the first packet from the server
	 * @param file 	 The filename
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws Exception
	 */
	private void getData(int opcode, String file) throws FileNotFoundException, IOException, Exception{
		// Get current time to calculate transmission time
		long initialTime = System.currentTimeMillis();	

		int numBytes = 0;
		// Create a file output stream to write to the file	
		FileOutputStream fos = new FileOutputStream(file);
		int prevBlockNum = 0;

		// If the opcode is 3 - DATA packet, extract data and store it in file
		if(opcode == 3) {
			while(true) {
				byte[] receivedData = this.receivePacket.getData();
			
				int blockNum = (int)( receivedData[2] << 8) | (receivedData[3] & 0xFF);

				byte[] data = new byte[receivedData.length - 4];
				System.arraycopy(receivedData, 4, data, 0, data.length);
			
				if(blockNum < 0)
					fos.close();
				// If this block number is previous block number + 1, store the data
				if(blockNum == (prevBlockNum + 1)) {
					fos.write(data);	 
					numBytes += this.receivePacket.getLength() - 4;
					this.sendAck(blockNum);		// Send ack for the the current data packet
					++prevBlockNum;
					if(this.receivePacket.getLength() < 516) {
						// If this is the last data packet, close the file output stream
						fos.close();
						break;		
					}
				} else	// If this block number is not previous block number + 1, send an ack
					// packet with previous block number
					this.sendAck(prevBlockNum);
				
				try {
					// Receive next packet from server
					// Timeout is set at 3000 ms
					byte[] buffer = new byte[516];
					this.receivePacket = new DatagramPacket(buffer,buffer.length);
					this.socket.setSoTimeout(3000);
					this.socket.receive(this.receivePacket);
					
				} catch(SocketTimeoutException e) {
					this.sendAck(blockNum);
				} catch(SocketException e) {
					this.terminateConnection();
				}
			}
		}
		
		// Get current time
		long finalTime = System.currentTimeMillis();
		// Calculate total time
		long totalTime = (long)(finalTime - initialTime);
		System.out.println("Transferred " + numBytes + " bytes in " + totalTime + " ms");
	}

	/**
	 * sendAck method
	 * This method sends an ack packet for the received data packets
	 *
 	 * @param blockNum The block number that is being acknowledged
	 *
	 * @throws Exception
	 */
	private void sendAck(int blockNum) throws Exception {
		// Extract the IP Address and port number of the TFTP server
		InetAddress toAddress = this.receivePacket.getAddress();
		int toPort = this.receivePacket.getPort();
		
		// Creata a byte buffer for the ack information
		byte[] buffer = new byte[4];
		Arrays.fill(buffer,0,1,(byte)0);
		Arrays.fill(buffer,1,2,(byte)4);

		buffer[2] = (byte) (blockNum >> 8);
		buffer[3] = (byte) blockNum;

		// Send the ack packet
		this.sendPacket = new DatagramPacket(buffer,buffer.length,toAddress,toPort);
		this.socket.send(this.sendPacket);	
	}

	/**
	 * connect method
	 * This method checks if the hostname is valid
	 *
	 * @param address The hostname of the server
	 */
	private boolean connect(String address) {
		boolean success = true;
		try {
			this.ipAddress = InetAddress.getByName(address);
		} catch(UnknownHostException e) {
			success = false;
		}
		return success;		// Return ture if the hostname is valid else return false
	}

	/**
	 * getCommand method
	 * This method gets the user command and returns it as a string array
	 *
	 * @return String[] array containing the user command
	 *
	 * @throws IOException
	 */
	private String[] getCommand() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = br.readLine();
		String[] command = input.split(" ");
		return command;
	}

	/**
	 * main method
	 * This is the main method
	 * It creates an object of the TftpClient class and starts the TFTP client program
	 *
	 * @param args The command line arguments
	 *
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{

		if(args.length != 1) {
			// If command line arguments are not provided correctly,
			// display an error message and terminate the execution
			System.out.println("Usage: java TftpClient <port>");
			System.out.println("<port> = allowed port number");
			System.exit(1);
		}
		int port = Integer.parseInt(args[0]);

		TftpClient client = new TftpClient(port);
		
		client.start();
	
	}

}
