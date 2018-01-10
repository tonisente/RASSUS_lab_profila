import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
    private List<String> children;
    private Map<String,List> treeNeighbor;

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
            treeNeighbor=new HashMap<>();

            System.out.println("MY ADDRESS: " + MY_ADDRESS + " :: MY PORT: " + MY_PORT + "\n");
        } catch (IOException ex) {
            System.out.println("There is problem with getting server socket!");
            System.out.println("Exception message: " + ex.getMessage());
            System.exit(-1);
        }
    }

    public void start() {
        System.out.println("Starting ... "); // TODO obrisi?

//        // register on network
//        sayHelloToCoordinator();
        //getNeighbours(5);

        // start listening tread
        Thread listenThread = new Thread(new PeerListener(neighbours, listenSocket, runningFlag));
        listenThread.start();

        // TODO 2: javljanje susjedima i uspostava veza medju susjedima
        //neighbourRequest();

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

            socket.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
     *Method used for getting n neighbor peers.
     */
    private void getNeighbours(int n) {
    	String message="2;" + n;
    	try (Socket socket = new Socket(COORDINATOR_IPADRESS, COORDINATOR_PORT)){

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println(message);
            // dodavanje susjeda koji nisu ja
            //ovo radi samo sa portovima posto je adresa localhost
            String answer;
            while((answer=reader.readLine())!=null) {
            	String[] fields=answer.split(";");
            	if(Integer.parseInt(fields[1])!=this.MY_PORT) {
            		this.neighbours.add(fields[1]);
            	}
            }
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
                // TODO 3: generiranje stabla prije slanja poruke
            	//treeRequest();
                // TODO 4: slanje poruke po stablu
            	//messageSender("TEXT!")
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
    /*
     *Method used for asking pears if they are able to be this peer neighbor. 
     */
    private void neighborRequest() {
    	String message="4;"+this.MY_ADDRESS+";"+this.MY_PORT;
    	sendMessage(message);
    }
    /*
     * Method used for creating a tree.
     */
    private void treeRequest() {
    	children=new ArrayList<>();
    	String message="5;"+this.MY_ADDRESS+";"+this.MY_PORT+";"+this.MY_ADDRESS+";"+this.MY_PORT;
    	sendMessage(message);
    }
    
    private void messageSender(String text) {
    	String message="6;"+this.MY_ADDRESS+";"+this.MY_PORT+";"+text;
    	sendMessageToChildren(message);
    }

    private void sendMessage(String message) {
        for (String port : neighbours) {
            try (Socket clientSocket = new Socket("localhost", Integer.parseInt(port));/*SOCKET->CONNECT*/) { //TODO: promjeni localhost adresu

                // get the socket's output stream and open a PrintWriter on it
                PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(
                        clientSocket.getOutputStream()), true);

                // get the socket's input stream and open a BufferedReader on it
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
                        clientSocket.getInputStream()));

                // send a String then terminate the line and flush
                //slucaj kad bi slali root peeru
                if(message.startsWith("5;") && message.split(";")[3]==port)
                	continue;
                outToServer.println(message);//WRITE
                System.out.println("Message sent!");

                // read a line of text
                String rcvString = inFromServer.readLine();//READ
                System.out.println("Received answer: " + rcvString);
                //odvajamo razlièite moguænosti odgovora
                if(message.startsWith("4;")) {
                	if(rcvString.startsWith("0"))
                		this.neighbours.remove(port);//ukoliko nas cvor odbije za susjeda,brisemo ga iz liste
                }
                else if(message.startsWith("5;")) {
                	String[] tmp=rcvString.split(";");
                	if(this.treeNeighbor.containsKey(tmp[1]+";"+tmp[2])) {
                		this.treeNeighbor.get(tmp[1]+";"+tmp[2]).add(tmp[4]);
                	}
                	else {
                		this.children.add(tmp[4]);
                		this.treeNeighbor.put(tmp[1]+";"+tmp[2], children);
                	}
                }
            } catch (ConnectException e) {
                System.out.println("CANT CONNECT!!!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    private void sendMessageToChildren(String message) {
        	List<String> tmp=this.treeNeighbor.get(message.split(";")[1]+message.split(";")[2]);
        	for(String s:tmp) {
        		try (Socket clientSocket = new Socket("localhost", Integer.parseInt(s));){

        			PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    outToServer.println(message);//WRITE
                    System.out.println("Message sent!");
                    //primanje odgovora?
        		} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
    }



    public static void main(String args[]) {
        new Peer(args[0]).start();
    }
}
