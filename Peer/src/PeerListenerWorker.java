import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerListenerWorker implements Runnable {

    private static final int SLEEP_TIME = 1500; // ms
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
                // sleep a little bit for simulation
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException ignore) {
                }

                String stringToSend = MessageDecider(receivedString);

                // send a String then terminate the line and flush
                if (!stringToSend.isEmpty()) {
                    outToClient.println(stringToSend);//WRITE
                }

                break; // while loop
            }
        } catch (SocketTimeoutException ignore_and_continue) {
        } catch (IOException ex) {
            System.err.println("Exception caught when trying to read or send data: " + ex);
        }
    }

    private String MessageDecider(String receivedString) {
        String[] components = receivedString.split(";");
        String answer = "";

        switch (components[0]) {
            case "4":
                answer = AddNeighbour(components[1], components[2]);
                break;
            case "5":
                TreeCreation(components[1], components[2], components[3], components[4]);
                break;
            case "6":
                SpreadMessage(components[1], components[2], components[3]);
                break;
            case "7":
                answer = AcceptParentship(components[1], components[2], components[3], components[4]);
                break;
            default:
                System.out.println("Recieved message is invalid!");
                break;
        }

        return answer;
    }

    // message type 4
    private String AddNeighbour(String ipAddress, String port) {
        neighbours.add(ipAddress + ";" + port);

        return "1";
    }

    // message type 5
    private void TreeCreation(String rootIpAddress, String rootPort, String ipSender, String portSender) {
        String key = rootIpAddress + ";" + rootPort;
        String sender = ipSender + portSender;

        // tree already registered
        if (trees.containsKey(key)) return;

        // send information to neighbours (except to node who send this message)
        for (String neighbour : neighbours) {
            if (neighbour.equals(sender)) continue;

            String sendMessage = String.format("%d;%s;%s;%s;%s", 5, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
            SendMessage(neighbour, sendMessage);
        }

        // send parentship request
        String parentshipRequest = String.format("%d;%s;%s;%s;%s", 7, rootIpAddress, rootPort, MY_ADDRESS, MY_PORT);
        String parentshipAnswer = SendMessageWithAnswer(ipSender + ";" + portSender, parentshipRequest);
        if (parentshipAnswer.startsWith("1")) {
            // when some node accept parentship, then we can save this new tree
            trees.put(key, new ArrayList<>());
        }
    }

    // message type 6
    private void SpreadMessage(String ipRoot, String portRoot, String sendMessage) {
        // send message to all neighbours
        for (String neighbour : trees.get(ipRoot + ";" + portRoot)) {
            SendMessage(neighbour, String.format("%d;%s;%s;%s", 6, ipRoot, portRoot, sendMessage));
        }

        // delete tree from
        trees.remove(ipRoot + ";" + portRoot);
    }

    // message type 7
    private String AcceptParentship(String rootIpAddress, String rootPort, String ipSender, String portSender) {
        trees.get(rootIpAddress + ";" + rootPort).add(ipSender + ";" + portSender);

        return "1";
    }

    /**
     * Sends a message using socket with random transport address.
     * After the message is sent, tcp connection closes.
     * Answer from receiver is not expected.
     *
     * @param destinationIpAddress
     * @param destinationPort
     * @param message
     */
    public static void SendMessage(String destinationIpAddress, Integer destinationPort, String message) {
        try (Socket clientSocket = new Socket(destinationIpAddress, destinationPort)) {
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

            outToServer.println(message);//WRITE
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message using socket with random transport address.
     * After the message is sent, tcp connection closes.
     * Answer from receiver is not expected.
     *
     * @param transportAddress in "ipAddress;port" format
     * @param message content
     */
    public static void SendMessage(String transportAddress, String message) {
        String[] components = transportAddress.split(";");
        SendMessage(components[0], Integer.parseInt(components[1]), message);
    }

    /**
     * Sends a message using random socket.
     * After the message is sent, socket expects answer from receiver.
     *
     * @param destinationIpAddress
     * @param destinationPort
     * @param message
     * @return receivers answer
     */
    public static String SendMessageWithAnswer(String destinationIpAddress, Integer destinationPort, String message) {
        String receivedAnsewer = "";

        try (Socket clientSocket = new Socket(destinationIpAddress, destinationPort)) {
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            outToServer.println(message);//WRITE

            receivedAnsewer = reader.readLine();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return receivedAnsewer;
    }

    /**
     * Sends a message using random socket.
     * After the message is sent, socket expects answer from receiver.
     *
     * @param transportAddress in "ipAddress;port" format
     * @param message content
     * @return receivers answer
     */
    public static String SendMessageWithAnswer(String transportAddress, String message) {
        String[] components = transportAddress.split(";");
        return SendMessageWithAnswer(components[0], Integer.parseInt(components[1]), message);
    }
}
