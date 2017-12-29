import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
		DataOutputStream outStream = null;
		DataInputStream inStream = null;

		boolean result = openTCPStreams(outStream, inStream, this.activeSocket);
		if (!result) {
			closeSocket(this.activeSocket);
			return;
		}

		String line = ReadUTF(inStream);
		if (line == null) {
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
			caseGetNeighbors(semicolonSeparated);
			break;
		default:
			System.out.println("Unknown request type.");
			closeSocket(this.activeSocket);
			return;

		}

		closeSocket(this.activeSocket);
	}

	private boolean openTCPStreams(DataOutputStream outStream, DataInputStream inStream, Socket activeSocket) {

		try {
			outStream = new DataOutputStream(activeSocket.getOutputStream());
			inStream = new DataInputStream(activeSocket.getInputStream());
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
		final int minimumParameters = 2;

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
	 * @param commaSeparated
	 */
	private void caseNewNode(String[] commaSeparated) {

		String newAddress = commaSeparated[1];
		InetSocketAddress newSocketAddress = parseAddress(newAddress);
		if(newSocketAddress == null) {
			closeSocket(this.activeSocket);
			return;
		}
		
		this.systemNodes.put(newAddress, newSocketAddress);
		return;

	}

	/**
	 * Processes given parameters towards the case of missing node.
	 * 
	 * @param commaSeparated
	 */
	private void caseMissingNode(String[] commaSeparated) {

	}

	/**
	 * Processes given parameters towards the case of getting new neighbors.
	 * 
	 * @param commaSeparated
	 *            Parameters.
	 */
	private void caseGetNeighbors(String[] commaSeparated) {

	}

}
