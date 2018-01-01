import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
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
    private static final int COORDINATOR_PORT = 8080;

    private String MY_ADDRESS;
    private int MY_PORT;

    // every neighbour is stored as string with value "ip_address;port"
    private Set<String> neighbours;
    private static final int LISTEN_SOCKET_TIMEOUT = 500; // ms
    private ServerSocket listenSocket;
    private AtomicBoolean runningFlag;

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
            MY_ADDRESS = listenSocket.getLocalSocketAddress().toString();
            MY_PORT = listenSocket.getLocalPort();

            neighbours = Collections.synchronizedSet(new TreeSet<String>()); // thread safe set (hopefully)
            runningFlag = new AtomicBoolean(true);

            System.out.println("MY ADDRESS: " + MY_ADDRESS + " :: MY PORT: " + MY_PORT + "\n");
        } catch (IOException ex) {
            System.out.println("There is problem with getting server socket!");
            System.out.println("Exception message: " + ex.getMessage());
            System.exit(-1);
        }
    }

    public void start() {
        System.out.println("Starting ... "); // TODO obrisi

//        // register on network
//        sayHelloToCoordinator(); // TODO

        // start listening tread
        Thread listenThread = new Thread(new PeerListener(neighbours, listenSocket, runningFlag));
        listenThread.start();

        // wait for user input
        waitForCommand(); // TODO: connect to network via command?!
    }

    /**
     * Sends message to coordinator so coordinator can put it to a
     * list of existing peers in network.
     */
    private void sayHelloToCoordinator() {
        String sendMessage = "1;" + MY_ADDRESS + ";" + MY_PORT;

        try (Socket socket = new Socket(COORDINATOR_IPADRESS, COORDINATOR_PORT)){

            // get the socket's output stream and open a PrintWriter on it
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // get the socket's input stream and open a BufferedReader on it
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send a message
            writer.println(sendMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Waits for user command to be executed.
     */
    private void waitForCommand() {
        System.out.println("Ready for command:");

        Scanner sc = new Scanner(System.in);
        Boolean quit = false;


        do {
            System.out.println("Enter a command:");
            String command = sc.nextLine().trim();
            char first = command.charAt(0);

            switch(first) {
            case 'q':
                quit = true;
                break;
            case 'a':
                String[] newNbrs = command.split(" ");
                for (int i = 1; i < newNbrs.length; i++) {
                    neighbours.add(newNbrs[i]);
                }
                break;
            case 'm':
                sendMessage();
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

    private void sendMessage() {
        for (String port : neighbours) {
            try (Socket clientSocket = new Socket("localhost", Integer.parseInt(port));/*SOCKET->CONNECT*/) { //TODO: promjeni localhost adresu

                String sndString = "Port " + MY_PORT + " sending to port: " + port;

                // get the socket's output stream and open a PrintWriter on it
                PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(
                        clientSocket.getOutputStream()), true);

                // get the socket's input stream and open a BufferedReader on it
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
                        clientSocket.getInputStream()));

                // send a String then terminate the line and flush
                outToServer.println(sndString);//WRITE
                System.out.println("Message sent!");

                // read a line of text
                String rcvString = inFromServer.readLine();//READ
                System.out.println("Received answer: " + rcvString);
            } catch (ConnectException e) {
                System.out.println("CANT CONNECT!!!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        new Peer(args[0]).start();
    }
}
