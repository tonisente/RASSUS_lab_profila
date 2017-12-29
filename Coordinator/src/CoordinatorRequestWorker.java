import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * Object of this class works in a parallel thread (runnable) and communicates
 * with other node in order to: record it's joining to the system, record other
 * node's non-existence, getting current system state(present nodes).
 * 
 * @author Janko
 *
 */
public class CoordinatorRequestWorker implements Runnable {

	private Socket activeSocket;
	private HashMap<String, InetSocketAddress> systemNodes;
	private final Object lockingKey;

	public CoordinatorRequestWorker(Socket activeSocket, HashMap<String, InetSocketAddress> systemNodes,
			Object lockingKey) {
		this.activeSocket = activeSocket;
		this.systemNodes = systemNodes;
		this.lockingKey = lockingKey;
	}

	@Override
	public void run() {
		// implement TCP message parsing and following system state manipulations
		// at the end close "activeSocket" with ".close()" method
		PrintWriter outStream = null;
		BufferedReader inStream = null;

		boolean result = openTCPStreams(outStream, inStream, this.activeSocket);
		if (!result) {
			closeSocket(this.activeSocket);
			return;
		}

		String line = "";
		try {
			line = inStream.readLine();
		} catch (IOException e) {
			System.err.println("Exception while reading with buffered reader.");
		}
		if ("".equals(line)) {
			closeSocket(this.activeSocket);
			return; // examine this
		}

		String[] semicolonSeparated = line.split(";");
		if (!isRightFormat(semicolonSeparated)) {
			closeSocket(this.activeSocket);
			return;
		}

		int requestType = Integer.valueOf(semicolonSeparated[0]);
		switch (requestType) {
		case 1:
			caseNewNode(semicolonSeparated);
			break;
		case 2:
			caseMissingNode(semicolonSeparated);
			break;
		case 3:
			caseGetNeighbors(semicolonSeparated, outStream);
			break;
		default:
			System.out.println("Unknown request type.");
			closeSocket(this.activeSocket);
			return;

		}
	}

	private boolean openTCPStreams(PrintWriter outStream, BufferedReader inStream, Socket activeSocket) {

		try {
			outStream = new PrintWriter(new OutputStreamWriter(this.activeSocket.getOutputStream()));
			inStream = new BufferedReader(new InputStreamReader(this.activeSocket.getInputStream()));
			return true;
		} catch (IOException e) {
			System.err.println("Exception when opening output or input TCP stream.");
			return false;
		}

	}

	/**
	 * Wrapper for socket.close() function.
	 * 
	 * @param socket
	 *            Socket we want to close.
	 */
	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Socket can not be close.");
		}
	}

	/**
	 * Wrapper function for reading line from data input stream.
	 * 
	 * @param inStream
	 *            Input stream to read from.
	 * @return Read string or null if reading can not be done.
	 */
	private String ReadUTF(DataInputStream inStream) {

		try {
			return inStream.readUTF();
		} catch (IOException e) {
			System.err.println("Exception on readUTF.");
			return null;
		}
	}

	/**
	 * Method specific to this implementation. Checks if the line sent via TCP
	 * connection is in wanted format. Wanted format has minimum 2 parameters and
	 * first parameter is a number.
	 * 
	 * @param commaSeparated
	 *            Received line separated by comma.
	 * @return True if line is in right format, false if not.
	 */
	private boolean isRightFormat(String[] commaSeparated) {
		final int minimumParameters = 3;

		int length = commaSeparated.length;
		if (length < minimumParameters)
			return false;
		String type = commaSeparated[0];
		try {
			Integer.parseInt(type);
		} catch (NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	/**
	 * Parses address given as string to address as InetSocketAddress. Returns
	 * InetSocketAddress object or null if any of given parameter format is wrong.
	 * @param source String representing address.
	 * @return Address in InetSocketAddress format.
	 */
	private InetSocketAddress parseAddress(String source) {

		String[] splitted = source.split(":");
		if (splitted.length != 2)
			return null;
		String hostname = splitted[0];
		int port;
		try {
			port = Integer.valueOf(splitted[1]);
		} catch (NumberFormatException nfe) {
			return null;
		}

		InetSocketAddress destination = null;
		try {
			destination = new InetSocketAddress(hostname, port);
		} catch (IllegalArgumentException iae) {
			return null;
		}
		return destination;
	}

	/**
	 * Processes given parameters towards the case of new node in the system.
	 * 
	 * @param semicolonSeparated
	 */
	private void caseNewNode(String[] semicolonSeparated) {

		String newAddress = semicolonSeparated[1] + ":" + semicolonSeparated[2];
		InetSocketAddress newSocketAddress = parseAddress(newAddress);
		if(newSocketAddress == null) {
			closeSocket(this.activeSocket);
			return;
		}
		
		synchronized(this.lockingKey) {
			this.systemNodes.put(newAddress, newSocketAddress);
		}
		
		closeSocket(this.activeSocket);
		return;
	}

	/**
	 * Processes given parameters towards the case of missing node.
	 * 
	 * @param semicolonSeparated
	 */
	private void caseMissingNode(String[] semicolonSeparated) {
		//test
		String ip = semicolonSeparated[1];
		String port = semicolonSeparated[2];
		String key = ip + ":" + port;
		
		synchronized(this.lockingKey) {
			this.systemNodes.remove(key);
		}
		
		closeSocket(this.activeSocket);
		return;
	}

	/**
	 * Processes given parameters towards the case of getting new neighbors.
	 * 
	 * @param semicolonSeparated
	 *            Parameters.
	 */
	private void caseGetNeighbors(String[] semicolonSeparated, PrintWriter outStream) {

		int neighborsCount;
		try {
			neighborsCount = Integer.parseInt(semicolonSeparated[1]);
		}catch(NumberFormatException nfe) {
			closeSocket(this.activeSocket);
			return;
		}
		
		String[] neighbors = getNNeighbors(neighborsCount);
		for(String toSend : neighbors) {
			outStream.println(toSend);
		}
		
		closeSocket(this.activeSocket);
		return;
	}
	private String[] getNNeighbors(int count) {
		
		//TODO osmisli algoritam za dobivanje n random susjeda
		
		return null;
		
	}

}
