import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

class TftpServer {

	DatagramSocket socket;
	DatagramPacket packet;

	int port;

	TftpServer(int localPort) throws Exception{
		this.port = localPort;
		this.socket = new DatagramSocket(localPort);
	}

	public void serve() throws Exception {
	
		while(true) {
			byte[] buffer = new byte[516];
			this.packet = new DatagramPacket(buffer,buffer.length);

			this.socket.receive(packet);
			
			buffer = packet.getData();
			
			int op = buffer[0] << 8 | buffer[1];

			if(op == 1)
				this.sendFile(buffer);
			else if(op == 2)
				this.writeFile(buffer);
		}
	}

	private void sendFile(byte[] byteData) throws Exception {
		ByteArrayInputStream buffer = new ByteArrayInputStream(byteData);
		// No need to read the op code
		buffer.read(); buffer.read();
		
		// Extract filename
		int c;
		String filename = "";
		while((c = buffer.read()) != 0) {
			filename += Character.toString((char)c);
		}

		// Extract mode
		String mode = "";
		while((c = buffer.read()) != 0) {
			mode += Character.toUpperCase((char)c);
		}

		// Create new file object
		File aFile = new File(filename);
		// Check if file exists
		if(!aFile.exists()) {
			// If file doesn't exist, send error packet to client
			// and terminate the connection
			this.sendErrorPacket(1,"File not Found");
			this.terminateConnection();	
		}

		// If file is accessible, send the file
		if(this.checkFileAccessibility(aFile,"READ")) {
			Path path = Paths.get(filename);		// Get file path
			byte[] fileData = Files.readAllBytes(path);	// Read file contents into a byte array
			
			int iteration = fileData.length;
			int block = 0;

			// Fill byte array with opcode, block number and file data
			// Create packet with byte array and send it to client
			while(iteration > -1) {
				byte[] sendData;
				if((iteration / 512) > 0)
					sendData = new byte[4+512];
				else
					sendData = new byte[4+iteration];

				// 2 bytes - opcode : 3
				Arrays.fill(sendData,0,1,(byte)3);
				// 2 bytes - block number
				Arrays.fill(sendData,2,3,(byte)(block+1));

				// Fill the data block with file data 
				for(int i = 4; i < (sendData.length - 4); ++i) {
					sendData[i] = fileData[ (block*512) + (i-4) ];
				}

				InetAddress address = this.packet.getAddress();		// Get client's IP address
				int toPort = this.packet.getPort();			// Get client's port number

				// Create new packet with file data and send it through the socket
				this.packet = new DatagramPacket(sendData,sendData.length,address,port);
				this.socket.send(this.packet);

				wait for an acknowledgement
				boolean receivedAck = false;
				this.socket.setSoTimeout(2000);
				try {
					byte[] receivedData = new byte[4];
					this.packet = new DatagramPacket(receivedData,receivedData.length);
					this.socket.receive(this.packet);
					
					

				} catch(SocketTimeoutException e) {
					receivedAck = false;
				}
				
				if(receivedAck) {
					++block;			// Increment block number
					iteration -= 512;
				}
			}

			aFile.close();

		} else {
			// If file is not accessible, send an error packet and terminate the connection
			this.sendErrorPacket(2,"Not enough access permission for file");
			this.terminateConnection();
		}
	}

	private void writeFile(byte[] buffer) throws Exception {
		ByteArrayInputStream buffer = new ByteArrayInputStream(byteData);
		// No need to read the op code
		buffer.read(); buffer.read();
		
		// Extract filename
		int c;
		String filename = "";
		while((c = buffer.read()) != 0) {
			filename += Character.toString((char)c);
		}

		// Extract mode
		String mode = "";
		while((c = buffer.read()) != 0) {
			mode += Character.toUpperCase((char)c);
		}

		// Create new file object
		File aFile = new File(filename);
		// Check if file exists
		if(aFile.exists()) {
			// If file exists, send error packet to client
			// and terminate the connection
			this.sendErrorPacket(6,"File Already exists");
			this.terminateConnection();	
		}
		
		
	}

	private void sendErrorPacket(int errorCode, String errorMsg) throws Exception {
		InetAddress address = this.packet.getAddress();		// Get client's IP address
		int toPort = this.packet.getPort();			// Get client's port number

		byte[] buffer = new byte[5 + errorMsg.length()];				// Create new byte array

		Arrays.fill(buffer,0,1,(byte)05);			// Fill byte array with op code 05 = ERROR
		Arrays.fill(buffer,2,3,(byte)errorCode);		// Fill byte array with error code

		// Fill byte array with error message
		int msgLen = errorMsg.length();
		int fromIndex = 4;
		for(int i = 0; i < msgLen; ++i) {
			char c = errorMsg.charAt(i);
			Arrays.fill(buffer,fromIndex,fromIndex+1,(byte)c);
			++fromIndex;
		}

		Arrays.fill(buffer,fromIndex,fromIndex+1,(byte)0);	// Fill last byte with 0's
		
		// Create new error packet as datagram packet with client's IP address and port number
		this.packet = new DatagramPacket(buffer,buffer.length,address,toPort);
		this.socket.send(this.packet);				// Send the datagram packet to client
	} // End of sendErrorPacket

	private String getCommand() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input = br.readLine();
		return input;
	}

	private boolean checkFileAccessibility(File aFile, String accessType) {
		if(accessType.equals("Read"))				
			return aFile.canRead();				// Return true if file is readable
		else if(accessType.equals("Write"))
			return aFile.canRead() & aFile.canWrite();	// Return true if file is both readable and writable
		else return false;
	}

	private void terminateConnection() throws Exception {
		this.socket.close();				// Close current socket
		this.socket = new DatagramSocket(this.port);	// Create new socket at the original port
	}

	public static void main(String[] args) throws Exception{

		int port = Integer.parseInt(args[0]);

		TftpServer server = new TftpServer(port);

		InetAddress address = InetAddress.getLocalHost();

		System.out.println("TFTP server listening at " + address.getHostAddress() + 
			" : " + server.socket.getLocalPort());

		server.serve();
	}

}
