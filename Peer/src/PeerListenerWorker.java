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


    PeerListenerWorker(Socket clientSocket, AtomicBoolean runngingFlag, Set<String> neighbours,
                       ConcurrentHashMap<String, List<ChildNode>> trees, String myAddress, Integer myPort) {
        this.clientSocket = clientSocket;
        this.runningFlag = runngingFlag;
        this.neighbours = neighbours;
        this.trees = trees;

        MY_ADDRESS = myAddress;
        MY_PORT = myPort;
    }

    @Override
    public void run() {
        try {// create a new BufferedReader from an existing InputStream
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // create a PrintWriter from an existing OutputStream
            PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream()), true);

            String receivedString;

            // read a few lines of text
            while ((receivedString = inFromClient.readLine()) != null/*READ*/) {
                //Utils.sleep(200);

                String stringToSend = MessageDecider(receivedString);

                // send a String then terminate the line and flush
                if (!stringToSend.isEmpty()) {
                    outToClient.println(stringToSend);//WRITE
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
        String answer = "";

        switch (components[0]) {
            case "4":
                answer = addNeighbour(components[1], components[2]);
                break;
            case "5":
                treeCreation(components[1], components[2], components[3], Integer.parseInt(components[4]));
                break;
            case "6":            	
            	String myKey = components[1] + ";" + components[2];
            	if(this.trees.get(myKey).size() == 0)
            		return "";
            	
            	while(!allChildrenResponded(components[1],components[2]));
                spreadMessage(components[1], components[2], components[3]);
                break;
            case "7":
            	acceptParentship(components[1], components[2], components[3], components[4], components[5]);
            	//poslati kad su svi primili
            	if(allChildrenResponded(components[1],components[2])) {
            		System.out.println("All children responded. Sending messages...");
            		messageBroadcast("Hello World!!!");
            		this.trees.remove(this.MY_ADDRESS+";"+this.MY_PORT);
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
        String key = rootIpAddress + ";" + rootPort;
        String sender = ipSender +";"+ portSender;

        // tree already registered
        if (trees.containsKey(key)) {
        	Utils.sendMessage(ipSender, portSender, "7;"+rootIpAddress+";"+rootPort+";"+this.MY_ADDRESS+";"+this.MY_PORT+";0");
        	return ;
        }
        //odmah poslati odgovor senderu

        trees.put(key, new ArrayList<>());

        // send information to neighbours (except to node who send this message)
        for (String neighbour : neighbours) {
            if (neighbour.equals(sender)) continue;
            trees.get(key).add(new ChildNode(neighbour));
            String sendMessage = String.format("%d;%s;%s;%s;%s", 5, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
            // send parent request
            Utils.sendMessage(neighbour, sendMessage);
        }
        
        Utils.sendMessage(ipSender,portSender , "7;"+rootIpAddress+";"+rootPort+";"+this.MY_ADDRESS+";"+this.MY_PORT+";1");
        
        /*String parentshipRequest = String.format("%d;%s;%s;%s;%s", 7, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
        String parentshipAnswer = Utils.sendMessageWithAnswer(ipSender, portSender, parentshipRequest).get(0);
        if (parentshipAnswer.startsWith("1")) {
            // when some node accept parentship, then we can save this new tree
            // TODO ?!
        }*/
    }

    // message type 6
    private void spreadMessage(String ipRoot, String portRoot, String sendMessage) {
        System.out.format("Message from %s: %s\n", portRoot, sendMessage);

        // send message to all neighbours
        for (ChildNode neighbour : trees.get(ipRoot + ";" + portRoot)) {
        	if(neighbour.getChildStatus().equals(ChildStatus.CONFIRMED))
        		Utils.sendMessage(neighbour.getTransportAddress(), String.format("%d;%s;%s;%s", 6, ipRoot, portRoot, sendMessage));
        }

        // delete tree from
        trees.remove(ipRoot + ";" + portRoot);
    }

    // message type 7
    private void acceptParentship(String rootIpAddress, String rootPort, String ipSender, String portSender, String status) {
//        String key = rootIpAddress + ";" + rootPort; // TODO ?!
        String key = "localhost" + ";" + rootPort;
//        String value = ipSender + ";" + portSender; // TODO ?!
        String sender = "localhost;" + portSender;

        for(ChildNode child:trees.get(key)) {
        	if(child.getTransportAddress().equalsIgnoreCase(sender)) {
        		if(status.equalsIgnoreCase("0"))
        			child.setChildStatus(ChildStatus.DECLINED);
        		else
        			child.setChildStatus(ChildStatus.CONFIRMED);
        	}
        		
        }

    }
    
    private boolean allChildrenResponded(String ip,String port) {
    	boolean value=true;
    	
    	List<ChildNode> test = this.trees.get(ip+";"+port);
    	if(test.isEmpty())
    		return false;
    	if(test == null)
    		return false;
    	
    	for (ChildNode ch: test) {
    		if(ch.getChildStatus().equals(ChildStatus.PENDING)) {
    			value=false;
    			break;
    		}
    	}
    	return value;
    }
    
    /**
     * Broadcast message to all neighbours.
     * @param message
     */
    private void messageBroadcast(String message) {
        String sendMessage = String.format("%d;%s;%s;%s", 6, MY_ADDRESS, MY_PORT, message);

        // multithread?!
        for (ChildNode child : trees.get(MY_ADDRESS + ";" + MY_PORT)) {//provjera
        	if(child.getChildStatus().equals(ChildStatus.CONFIRMED))
        		Utils.sendMessage(child.getTransportAddress(), sendMessage);
        }
    }
}
