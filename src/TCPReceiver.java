import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPReceiver implements Runnable {

    private final int MY_PORT;
    private final String MY_IPAdress;

    TCPReceiver(int port, String ipAdress) {
        this.MY_PORT = port;
        this.MY_IPAdress = ipAdress;
    }

    private void startListening() {
        try (ServerSocket serverSocket = new ServerSocket(MY_PORT) /*SOCKET->BIND->LISTEN*/) {

            while (true) {
                try (// create a new socket, accept and listen for a connection to be made to this socket
                     Socket clientSocket = serverSocket.accept();//ACCEPT

                     // create a new BufferedReader from an existing InputStream
                     BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                     // create a PrintWriter from an existing OutputStream
                     PrintWriter writer = new PrintWriter(
                             new OutputStreamWriter(clientSocket.getOutputStream()), true)) {

                    String rcvString;
                    // read a few lines of text
                    while ((rcvString = reader.readLine()) != null/*READ*/) {
                        System.out.println("Receiver received: " + rcvString);

                        //shutdown the server if requested
                        if (rcvString.contains("shutdown")) {
                            return;
                        }

                        // send upper case text
                        String sndString = rcvString.toUpperCase();

                        // send a String then terminate the line and flush
                        writer.println(sndString);//WRITE
                        System.out.println("Receiver sent: " + sndString);
                    }

                    break;
                } catch (IOException ex) {
                    System.err.println("Exception caught when trying to read or send data: " + ex);
                } //clientSocket CLOSE
            }
        } catch (IOException ex) {
            System.err.println("Exception caught when opening the socket or waiting for a connection: " + ex);
        } //CLOSE
    }


    @Override
    public void run() {
        startListening();
    }
}
