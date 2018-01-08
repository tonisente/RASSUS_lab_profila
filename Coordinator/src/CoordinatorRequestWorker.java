import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Method "run()" of this runnable class starts in a parallel thread to
 * communicate with other node in order to: record it's joining to the system,
 * record other node's non-existence or sending current system state(present
 * nodes). It works with constructor given active socket and hash map of system
 * nodes. Parses line read from TCP socket and makes suitable response to it,
 * either sending response as string or closing the connection (meaning bad
 * request).
 * 
 * @author Janko
 *
 */
public class CoordinatorRequestWorker implements Runnable {

	/**
	 * Active socket to communicate with client.
	 */
	private Socket activeSocket;
	/**
	 * Hash map of all the nodes in distributed system. mapped in format:
	 * "ip_address:port" => InetSocketAddress.
	 */
	private HashMap<String, InetSocketAddress> systemNodes;
	/**
	 * Key for synchronized block to protect access to system nodes data structure.
	 */
	private final Object lockingKey;

	/**
	 * Constructor with base parameters, essential to communicate to the client.
	 * 
	 * @param activeSocket
	 *            Active socket to work with client.
	 * @param systemNodes
	 *            Hash map of nodes in distributed system mapped in format:
	 *            "ip_address:port" => InetSocketAddress.
	 * @param lockingKey
	 *            Lock key to work over map of system nodes.
	 */
	public CoordinatorRequestWorker(Socket activeSocket, HashMap<String, InetSocketAddress> systemNodes,
			Object lockingKey) {
		this.activeSocket = activeSocket;
		this.systemNodes = systemNodes;
		this.lockingKey = lockingKey;
	}

	@Override
	public void run() {

		// opening out and in stream
		PrintWriter outStream = createPrintWriter(this.activeSocket);
		BufferedReader inStream = createBufferedReader(this.activeSocket);
		if (outStream == null || inStream == null) {
			closeSocket(this.activeSocket);
			return;
		}

		// read and parse
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

		// taking the right case
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

	/**
	 * Wrapper method for opening the buffered reader.
	 * 
	 * @param activeSocket
	 *            Socket to open buffered reader over it.
	 * @return If opening succeed returning buffered reader, if not returning null.
	 */
	private BufferedReader createBufferedReader(Socket activeSocket) {
		try {
			return new BufferedReader(new InputStreamReader(this.activeSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Exception while opening buffered reader.");
			return null;
		}
	}

	/**
	 * Wrapper method for opening the print writer.
	 * 
	 * @param activeSocket
	 *            Socket to open print writer over it.
	 * @return If opening succeed returning print writer, if not returning null.
	 */
	private PrintWriter createPrintWriter(Socket activeSocket) {

		try {
			return new PrintWriter(new OutputStreamWriter(this.activeSocket.getOutputStream()));
		} catch (IOException e) {
			System.err.println("Exception while opening print writer.");
			return null;
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
	 * 
	 * @param source
	 *            String representing address.
	 * @return Address in InetSocketAddress format.
	 */
	private InetSocketAddress parseAddress(String source) {

		String[] splitted = source.split(";");
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

		String newAddress = semicolonSeparated[1] + ";" + semicolonSeparated[2];
		InetSocketAddress newSocketAddress = parseAddress(newAddress);

		if (newSocketAddress != null)
			synchronized (this.lockingKey) {
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

		final int wantedCount = 3;

		if (semicolonSeparated.length >= wantedCount) {

			String ip = semicolonSeparated[1];
			String port = semicolonSeparated[2];
			String key = ip + ":" + port;

			synchronized (this.lockingKey) {
				this.systemNodes.remove(key);
			}
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
		} catch (NumberFormatException nfe) {
			closeSocket(this.activeSocket);
			return;
		}

		String[] neighbors = getNNeighbors(neighborsCount);
		for (String toSend : neighbors) {
			if (toSend == null)
				break;
			outStream.println(toSend);
		}

		closeSocket(this.activeSocket);
		return;
	}

	/**
	 * Randomly extracts "count" number of keys in system nodes map. Key by
	 * itself is string consisting of neigbor's IP address and port.
	 * 
	 * @param count Number of neighbors we want to extract.
	 * @return String array of keys. Every key is representing one neighbor.
	 */
	private String[] getNNeighbors(int count) {

		Set<String> chosenNodes = new HashSet<>();
		String[] neighbors = new String[count];
		ArrayList<String> keys = new ArrayList<>(this.systemNodes.keySet());

		int size = keys.size();
		if (size < count) {
			count = size;
		}
		int i = 0;
		while (chosenNodes.size() != count) {
			int index = (int) (Math.random() * (size - 1));
			String neighbor = keys.get(index);
			if (chosenNodes.add(neighbor)) {
				neighbors[i] = neighbor;
				i++;
			}
		}

		return neighbors;

	}

}
