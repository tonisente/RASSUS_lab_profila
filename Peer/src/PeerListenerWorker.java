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
    private ConcurrentHashMap<String, List<String>> trees;


    PeerListenerWorker(Socket clientSocket, AtomicBoolean runngingFlag, Set<String> neighbours,
                       ConcurrentHashMap<String, List<String>> trees, String myAddress, Integer myPort) {
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
                Utils.sleep(200);

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
                spreadMessage(components[1], components[2], components[3]);
                break;
            case "7":
                answer = acceptParentship(components[1], components[2], components[3], components[4]);
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
        String sender = ipSender + portSender;

        // tree already registered
        if (trees.containsKey(key)) return;

        trees.put(key, new ArrayList<>());

        // send information to neighbours (except to node who send this message)
        for (String neighbour : neighbours) {
            if (neighbour.equals(sender)) continue;

            String sendMessage = String.format("%d;%s;%s;%s;%s", 5, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
            Utils.sendMessage(neighbour, sendMessage);
        }

        // send parentship request
        String parentshipRequest = String.format("%d;%s;%s;%s;%s", 7, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
        String parentshipAnswer = Utils.sendMessageWithAnswer(ipSender, portSender, parentshipRequest).get(0);
        if (parentshipAnswer.startsWith("1")) {
            // when some node accept parentship, then we can save this new tree
            // TODO ?!
        }
    }

    // message type 6
    private void spreadMessage(String ipRoot, String portRoot, String sendMessage) {
        System.out.format("Message from root %s: %s\n", portRoot, sendMessage);

        // send message to all neighbours
        for (String neighbour : trees.get(ipRoot + ";" + portRoot)) {
            Utils.sendMessage(neighbour, String.format("%d;%s;%s;%s", 6, ipRoot, portRoot, sendMessage));
        }

        // delete tree from
        trees.remove(ipRoot + ";" + portRoot);
    }

    // message type 7
    private String acceptParentship(String rootIpAddress, String rootPort, String ipSender, String portSender) {
//        String key = rootIpAddress + ";" + rootPort; // TODO ?!
        String key = "localhost" + ";" + rootPort;
//        String value = ipSender + ";" + portSender; // TODO ?!
        String value = "localhost;" + portSender;

        trees.get(key).add(value);

        return "1\n";
    }
}
