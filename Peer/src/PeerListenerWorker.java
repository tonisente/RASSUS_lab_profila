import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerListenerWorker implements Runnable {

	private final String MY_ADDRESS;
	private final Integer MY_PORT;

	private AtomicBoolean runningFlag;
	private Socket clientSocket;
	private Set<String> neighbours;
	private ConcurrentHashMap<String, List<ChildNode>> trees;
	
	private final Object syncKey;

	PeerListenerWorker(Socket clientSocket, AtomicBoolean runngingFlag, Set<String> neighbours,
			ConcurrentHashMap<String, List<ChildNode>> trees, String myAddress, Integer myPort, Object syncKey) {
		this.clientSocket = clientSocket;
		this.runningFlag = runngingFlag;
		this.neighbours = neighbours;
		this.trees = trees;

		MY_ADDRESS = myAddress;
		MY_PORT = myPort;
		
		this.syncKey = syncKey;
	}

	@Override
	public void run() {
		try {// create a new BufferedReader from an existing InputStream
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			// create a PrintWriter from an existing OutputStream
			PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

			String receivedString;

			// read a few lines of text
			while ((receivedString = inFromClient.readLine()) != null/* READ */) {
				// Utils.sleep(200);

				String stringToSend = MessageDecider(receivedString);

				// send a String then terminate the line and flush
				if (!stringToSend.isEmpty()) {
					outToClient.println(stringToSend);// WRITE
				}

				break; // while loop
			}
		} catch (SocketTimeoutException ignore) {
			ignore.printStackTrace();
		} catch (IOException ex) {
			System.err.println("Exception caught when trying to read or send data: " + ex);
		}
	}

	private String MessageDecider(String receivedString) {
		String[] components = receivedString.split(";");
		String rootHost = components[1];
		String rootPort = components[2];
		String answer = "";

		switch (components[0]) {
		case "4":// neighbor request response
			String neighborHost = components[1];
			String neighborPort = components[2];

			answer = addNeighbour(neighborHost, neighborPort);
			break;
		case "5":// tree creation
			String parentHost = components[3];
			String parentPort = components[4];
			
			treeCreation(rootHost, rootPort, parentHost, Integer.parseInt(parentPort));
			break;
		case "6":// message
			String message = components[3];
			System.out.println();
			String treeKey = rootHost + ";" + rootPort;

			//received message
			System.out.format("Message from %s: %s\n", rootPort, message);
			
			// check if have no children
			if(this.trees.get(treeKey).isEmpty()) {
				trees.remove(treeKey);
				break;
			}
			
			// do have children
			// waiting for tree creation
			while (!allChildrenResponded(rootHost, rootPort));

			System.out.println("Tree ("+ treeKey +") created. Sending message further...");
			spreadMessage(rootHost, rootPort, message);
			
			// delete tree from
			trees.remove(treeKey);
			
			break;
		case "7":
			String childHost = components[3];
			String childPort = components[4];
			String status = components[5];

			String myID = this.MY_ADDRESS + ";" + this.MY_PORT;
			String rootID = rootHost + ";" + rootPort;
			
			
			//Brane genije :D
			synchronized(syncKey) {
				acceptParentship(rootHost, rootPort, childHost, childPort, status);
	
				if(myID.equals(rootID))
					if(allChildrenResponded(rootHost, rootPort)) {
		
						System.out.println("ROOT: Tree created. Sending message further...");
						messageBroadcast("Hello World!!!");
						this.trees.remove(myID);
				}
			}
			break;
		default:
			System.out.println("Recieved message is invalid!");
			break;
		}

		return answer;
	}

	// message type 4
	private String addNeighbour(String ipAddress, String port) {
		neighbours.add(ipAddress + ";" + port);
		return "1";
	}

	// message type 5
	private void treeCreation(String rootIpAddress, String rootPort, String ipSender, Integer portSender) {
		String keyRoot = rootIpAddress + ";" + rootPort;
		String sender = ipSender + ";" + portSender;

		// decline parent - tree already registered
		String responseBase = "7;" + rootIpAddress + ";" + rootPort + ";" + this.MY_ADDRESS + ";" + this.MY_PORT;
		String acceptResponse = responseBase + ";1";
		String declineResponse = responseBase + ";0";

		if (trees.containsKey(keyRoot)) {
			//System.out.println("\tTree "+ keyRoot +" | Parent request from: " + sender + " | Status: DECLINED" );
			Utils.sendMessage(ipSender, portSender, declineResponse);
			return;
		}
		
		trees.put(keyRoot, new ArrayList<>());
		
		//System.out.println("\tTree "+ keyRoot +" | Parent request from: " + sender + " | Status: ACCEPTED" );
		
		// confirm parent
		Utils.sendMessage(ipSender, portSender, acceptResponse);

		

		// send information to neighbours (except to node who send this message)
		for (String neighbour : neighbours) {

			if (neighbour.equals(sender))
				continue;

			trees.get(keyRoot).add(new ChildNode(neighbour));

			// send parent request
			String sendMessage = String.format("%d;%s;%s;%s;%s", 5, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
			Utils.sendMessage(neighbour, sendMessage);
		}

		/*
		 * String parentshipRequest = String.format("%d;%s;%s;%s;%s", 7, rootIpAddress,
		 * rootPort, MY_ADDRESS, MY_PORT); String parentshipAnswer =
		 * Utils.sendMessageWithAnswer(ipSender, portSender, parentshipRequest).get(0);
		 * if (parentshipAnswer.startsWith("1")) { // when some node accept parentship,
		 * then we can save this new tree // TODO ?! }
		 */
	}

	// message type 6
	private void spreadMessage(String ipRoot, String portRoot, String sendMessage) {

		// send message to all neighbours
		List<ChildNode> children = trees.get(ipRoot + ";" + portRoot);
		for (ChildNode child : children) {
			if (child.getChildStatus().equals(ChildStatus.CONFIRMED))
				Utils.sendMessage(child.getTransportAddress(),
						String.format("%d;%s;%s;%s", 6, ipRoot, portRoot, sendMessage));
		}
	}

	// message type 7
	private synchronized void acceptParentship(String rootIpAddress, String rootPort, String ipSender, String portSender,
			String status) {
		// String key = rootIpAddress + ";" + rootPort; // TODO ?!
		String key = rootIpAddress + ";" + rootPort;
		// String value = ipSender + ";" + portSender; // TODO ?!
		String sender = ipSender + ";" + portSender;

		for (ChildNode child : trees.get(key)) {
			if (child.getTransportAddress().equalsIgnoreCase(sender)) {
				if (status.equalsIgnoreCase("0"))
					child.setChildStatus(ChildStatus.DECLINED);
				else
					child.setChildStatus(ChildStatus.CONFIRMED);
			}

		}

	}

	private synchronized boolean allChildrenResponded(String ip, String port) {

		boolean value = true;

			List<ChildNode> children = this.trees.get(ip + ";" + port);
			for (ChildNode childNode : children) {
				if (childNode.getChildStatus().equals(ChildStatus.PENDING)) {
					value = false;
					break;
				}
			}
		return value;
	}

	/**
	 * Broadcast message to all neighbours.
	 * 
	 * @param message
	 */
	private void messageBroadcast(String message) {
		String sendMessage = String.format("%d;%s;%s;%s", 6, MY_ADDRESS, MY_PORT, message);

		// multithread?!
		for (ChildNode child : trees.get(MY_ADDRESS + ";" + MY_PORT)) {// provjera
			if (child.getChildStatus().equals(ChildStatus.CONFIRMED))
				Utils.sendMessage(child.getTransportAddress(), sendMessage);
		}
	}
}
