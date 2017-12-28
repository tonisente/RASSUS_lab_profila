import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Server node that takes record of all the available nodes in the distributed
 * system. Takes record if node enters the system, or if node leaves the system.
 * Every node entering the system has to reach this node so it can get
 * information about system state. Server node is listening for TCP connections.
 * When TCP connection is accepted new request worker is initialized to process
 * that request. Server can process three types of requests: 1 - entering the
 * system, 2 - leaving system, 3 - getting new neighbors.
 * 
 * @author Janko
 *
 */
public class CoordinatorNode {

	private HashMap<String, InetSocketAddress> systemNodes;
	private boolean runningFlag;

	// multi-threading
	private static final int NUMBER_OF_THREADS = 10;
	private final ExecutorService executor;
	private final Object lockingKey = new Object();

	/**
	 * Default constructor. Initializes hash map for system nodes storing and sets
	 * running flag on TRUE. Also initializes fixed pool of threads which will host
	 * workers for processing upcoming TCP connections.
	 */
	public CoordinatorNode() {

		this.systemNodes = new HashMap<>();
		this.runningFlag = true;

		this.executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
	}

	/**
	 * Starts coordinator server node by opening passive TCP socket and listening to
	 * connections on this passive socket.
	 */
	public void start() {

		ServerSocket passiveSocket = null;
		openPassiveSocket(passiveSocket);

		while (this.runningFlag) {

			Socket newActiveSocket = accept(passiveSocket);
			if (newActiveSocket == null)
				continue;
			if(processRequestInNewThread(newActiveSocket) == false )
				closeSocket(newActiveSocket);
		}

		closePassiveSocket(passiveSocket);

	}

	/**
	 * Sets server node running flag to FALSE. It will result in finishing last
	 * iteration of accepting TCP connection and after that stop working.
	 */
	public void stop() {
		this.runningFlag = false;
	}

	/* MAIN */
	public static void main(String[] args) {

		CoordinatorNode coordinatorNode = new CoordinatorNode();

		coordinatorNode.start();

	}
	/* END MAIN */

	/* SIDE FUNCTIONS */
	/**
	 * Wrapper for standard TCP accept. Catches IOException if thrown.
	 * 
	 * @param pasiveSocket
	 *            Socket for accepting the TCP connection.
	 * @return Socket if connection is accepted, or null if exception is thrown.
	 */
	private Socket accept(ServerSocket pasiveSocket) {
		try {
			Socket newActiveSocket = pasiveSocket.accept();
			return newActiveSocket;
		} catch (IOException e) {
			System.err.println("Unable to accept TCP request. Returning NULL.");
			return null;
		}
	}

	/**
	 * Wrapper for server socket creation. If the IOException is thrown prints message and
	 * exits the system with error code 1.
	 * @param socket
	 */
	private void openPassiveSocket(ServerSocket socket) {
		try {
			socket = new ServerSocket();
		} catch (IOException e) {
			System.err.println("Could not open TCP pasive socket");
			System.exit(1);
		}
	}

	/**
	 * Wrapper for serverSocket.close() function.
	 * @param socket ServerSocket we want to close.
	 */
	private void closePassiveSocket(ServerSocket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Socket can not be close.");
		}
	}
	/**
	 * Wrapper for socket.close() function.
	 * @param socket Socket we want to close.
	 */
	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Socket can not be close.");
		}
	}

	/**
	 * Starts worker in a new thread. Worker's job is to process TCP request.
	 * @param newActiveSocket
	 */
	private boolean processRequestInNewThread(Socket newActiveSocket) {
		// make new thread to serve TCP request
		CoordinatorRequestWorker newWorker = new CoordinatorRequestWorker(newActiveSocket, this.systemNodes, this.lockingKey);
		try {
			this.executor.execute(newWorker);
			return true;
		}
		catch(RejectedExecutionException ree) {
			System.err.println("Thread creation rejected.");
			return false;
		}
	}
}
