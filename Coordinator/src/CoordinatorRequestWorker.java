import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * Object of this class works in a parallel thread (runnable) and communicates with
 * other node in order to: record it's joining to the system, record other node's
 * non-existance, getting current system state(present nodes).
 * 
 * @author Janko, TkoGodNastavi
 *
 */
public class CoordinatorRequestWorker implements Runnable {

	private Socket activeSocket;
	private HashMap<String, InetSocketAddress> systemNodes;
	private final Object lockingKey;

	public CoordinatorRequestWorker(Socket activeSocket, HashMap<String, InetSocketAddress> systemNodes, Object lockingKey) {
		this.activeSocket = activeSocket;
		this.systemNodes = systemNodes;
		this.lockingKey = lockingKey;
	}

	@Override
	public void run() {

		//implement TCP message parsing and following system state manipulations
		//at the end close "activeSocket" with ".close()" method

	}

}
