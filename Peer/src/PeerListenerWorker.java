import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerListenerWorker implements Runnable {

    private static final int SLEEP_TIME = 1500; // ms

    private AtomicBoolean runningFlag;
    private Socket clientSocket;

    PeerListenerWorker(Socket clientSocket, AtomicBoolean runngingFlag) {
        this.clientSocket = clientSocket;
        this.runningFlag = runngingFlag;
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
                } catch (InterruptedException ignore) {}

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
                answer = TreeCreation(components[1], components[2]);
                break;
            case "6":
                SendMessage(components[1]);
                break;
            default:
                System.out.println("Recieved message is invalid!");
                break;
        }

        return answer;
    }

    private void SendMessage(String component) {
        // send 
    }

    }    private String AddNeighbour(String component, String component1) {
        return null;
    }

    private String AddNeighbour(String component, String component1) {
        return null;


}
