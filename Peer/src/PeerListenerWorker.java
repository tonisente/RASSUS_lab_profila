import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerListenerWorker implements Runnable {

    private static final int SLEEP_TIME = 3000; // ms

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

            System.out.print("INFO: ");
            System.out.print(clientSocket.getLocalPort());
            System.out.print(" :: ");
            System.out.println(clientSocket.getInetAddress());
            System.out.println();

             // create a PrintWriter from an existing OutputStream
             PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(
                     clientSocket.getOutputStream()), true);

            String receivedString;

            // read a few lines of text
            while ((receivedString = inFromClient.readLine()) != null/*READ*/) {
                System.out.println("Server received: " + receivedString);

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException ignore) {}

                String stringToSend = "Listener odgovara!";

                // send a String then terminate the line and flush
                outToClient.println(stringToSend);//WRITE
            }
        } catch (SocketTimeoutException ignore_and_continue) {
        } catch (IOException ex) {
            System.err.println("Exception caught when trying to read or send data: " + ex);
        }
    }
}
