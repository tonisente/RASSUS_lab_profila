import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server node that takes record of all the available nodes in the distributed
 * system. Takes record if node enters the system, or if node leaves the system.
 * Every node entering the system has to reach this node so it can get information about system
 * state.
 * 
 * @author Janko
 *
 */
public class CoordinatorNode {

	private HashMap<String, InetSocketAddress> systemNodes;
	private boolean runningFlag;
	
	//multi-threading
	private static final int NUMBER_OF_THREADS = 10;
	private final ExecutorService executor;
	
	public CoordinatorNode() {
		
		this.systemNodes = new HashMap<>();
		this.runningFlag = true;
		
		this.executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
	}
	
	public void start(){
		
		ServerSocket passiveSocket = null;
		openPassiveSocket(passiveSocket);
		
		while(this.runningFlag) {
			
			Socket newActiveSocket = accept(passiveSocket);
			if(newActiveSocket == null)
				continue;
			processRequest(newActiveSocket);
		}
		
		closeServerSocket(passiveSocket);
		
	}
	public void stop() {
		this.runningFlag = false;
	}
	
	public static void main(String[] args) {
		
		CoordinatorNode coordinatorNode = new CoordinatorNode();
		
		coordinatorNode.start();
		

	}
	
	//side functions
	private Socket accept(ServerSocket pasiveSocket) {
		try {
			Socket newActiveSocket = pasiveSocket.accept();
			return newActiveSocket;
		} catch (IOException e) {
			System.err.println("Unable to accept TCP request. Returning NULL.");
			return null;
		}
	}
	private void openPassiveSocket(ServerSocket socket) {
		try {
			socket = new ServerSocket();
		} catch (IOException e) {
			System.err.println("Could not open TCP pasive socket");
			System.exit(1);
		}
	}
	private void closeServerSocket(ServerSocket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Socket can not be close.");
		}
	}
	private void processRequest(Socket newActiveSocket) {
		//make new thread to serve TCP request
	}
}
