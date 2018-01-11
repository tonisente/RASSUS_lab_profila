import java.io.*;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents one node (peer) in network. When node is created, first, it should
 * contact a central coordinator node (which network adress is well known) few times for adresses of a few node to connect with.
 * Optimal number of connection for every peer should be around 2 and 5 nodes.
 * After startup, the main thread begins to listen for user commands (such as send message, disconnect,
 * print neighbour nodes), and another thread starts to listen for incoming messages.
 */
public class Peer {
    private static final String COORDINATOR_IPADRESS = "localhost";
    private static final int COORDINATOR_PORT = 50000;

    private String MY_ADDRESS;
    private int MY_PORT;

    // every neighbour is stored as string with value "ip_address;port"
    private Set<String> neighbours;
    private static final int LISTEN_SOCKET_TIMEOUT = 500; // ms
    private ServerSocket listenSocket;
    private AtomicBoolean runningFlag;
    private ConcurrentHashMap<String, List<String>> trees;


//       TODO:  DONT DELETE THIS CONSTUCTOR!! IT SHOULD BE USED IN FINAL VERSION.
//    Peer() {
//        try {
//            listenSocket = new ServerSocket();
//            neighbours = Collections.synchronizedSet(new TreeSet<String>()); // thread safe set (hopefully)
//
//            // get network address from "main" socket
//            MY_ADDRESS = listenSocket.getInetAddress().getHostAddress();
//            MY_PORT = listenSocket.getLocalPort();
//
//        } catch (IOException ex) {
//            System.out.println("There is problem with getting server socket!");
//            System.out.println("Exception message: " + ex.getMessage());
//            System.exit(-1);
//        }
//    }

    Peer(String port) {
        try {
            listenSocket = new ServerSocket(Integer.parseInt(port));
            listenSocket.setSoTimeout(LISTEN_SOCKET_TIMEOUT);

            MY_ADDRESS = "localhost"; // TODO: listenSocket.getInetAddress().toString();
            MY_PORT = listenSocket.getLocalPort();

            neighbours = Collections.synchronizedSet(new TreeSet<String>()); // thread safe set (hopefully)
            runningFlag = new AtomicBoolean(true);
            trees = new ConcurrentHashMap<>();

            System.out.println("MY ADDRESS: " + MY_ADDRESS + " :: MY PORT: " + MY_PORT + "\n");
        } catch (IOException ex) {
            System.out.println("There is problem with getting server socket!");
            System.out.println("Exception message: " + ex.getMessage());
            System.exit(-1);
        }
    }

    public void start() {
        System.out.println("Starting ... ");

        // start listening tread
        Thread listenThread = new Thread(new PeerListener(neighbours, listenSocket, runningFlag, trees));
        listenThread.start();

        Utils.sleep(500);

        sayHelloToCoordinator();

        // wait for user input
        waitForCommand();
    }

    /**
     * Waits for user command to be executed.
     */
    private void waitForCommand() {
        System.out.println("Ready for command:");

        Scanner sc = new Scanner(System.in);
        Boolean quit = false;


        do {
            System.out.print("> ");
            String command = sc.nextLine().trim();
            char first = command.charAt(0);

            switch(first) {
            case 'q': // quit
                quit = true;
                break;
            case 'a': // add
                addNeighboursManualy(command);
                break;
            case 'm': // message
            	treeRequest();
                Utils.sleep(1000); // wait a little bit for tree to spread
            	messageBroadcast("Hello world!!!");
            	trees.remove(MY_ADDRESS + ";" + MY_PORT);
                break;
            case 'n': // get list of neighbours from coordinator
                Integer noNeighbours = Integer.parseInt(command.substring(2, 3));
                getNeighbours(noNeighbours);
                break;
            case 'h': // hello :)
                sayHelloToCoordinator();
                break;
            default:
                System.out.println("Unsupported command!");
                break;
            }
        } while (!quit);


        System.out.println("quitting");
        sc.close();
        System.exit(0);
    }

    /**
     * Sends message to coordinator so coordinator can put it to a
     * list of existing peers in network.
     */
    private void sayHelloToCoordinator() {
        String message = "1;" + MY_ADDRESS + ";" + MY_PORT;
        Utils.sendMessage(COORDINATOR_IPADRESS, COORDINATOR_PORT, message);
    }

    /**
     * Method used for getting n neighbor peers.
     * @param n number of requested neighbours from central coordinator
     */
    private void getNeighbours(int n) {
    	String message = "2;" + n;
    	List<String> answer = Utils.sendMessageWithAnswer(COORDINATOR_IPADRESS, COORDINATOR_PORT, message);

        neighbours.addAll(answer);

        // print all current neighbours
        System.out.println();
        System.out.println(" - neighbours -");
        for (String neighbour : neighbours) {
            System.out.println("    -> " + neighbour);
        }
        System.out.println();
    }

    /**
     * Broadcast message for tree creation.
     */
    private void treeRequest() {
        String key = MY_ADDRESS + ";" + MY_PORT;
        trees.put(key, new ArrayList<>());

        String message = String.format("%d;%s;%s;%s;%s", 5, MY_ADDRESS, MY_PORT, MY_ADDRESS, MY_PORT);

    	// TODO: parallel in multiple threads?!
        for (String neighbour : neighbours) {
            Utils.sendMessage(neighbour, message);
        }
    }

    /**
     * Broadcast message to all neighbours.
     * @param message
     */
    private void messageBroadcast(String message) {
        String sendMessage = String.format("%d;%s;%s;%s", 6, MY_ADDRESS, MY_PORT, message);

        // multithread?!
        for (String child : trees.get(MY_ADDRESS + ";" + MY_PORT)) {
            Utils.sendMessage(child, sendMessage);
        }
    }

    /**
     * Adds manualy given peer(s) as neighbours (mostly used for testing purpose).
     * @param command in format: "a port1 port2 port3 ... "
     */
    private void addNeighboursManualy(String command) {
        String[] newNbrs = command.split(" ");
        String ipAddress = "localhost";
        for (int i = 1; i < newNbrs.length; i++) {
            neighbours.add(ipAddress + ";" + newNbrs[i]);
        }
    }

    public static void main(String args[]) {
        new Peer(args[0]).start();
    }
}
