import java.io.*;
import java.net.Socket;

public class TCPSender implements Runnable{

    private final int PORT;
    private final String IPAdress;

    TCPSender(int port, String ipAdress) {
        this.PORT = port;
        this.IPAdress = ipAdress;
    }

    private void send() {
        try (Socket socket = new Socket(this.IPAdress, this.PORT)) {
            String sendMessage = "Hello there ... ";

            // get the socket's output stream and open a PrintWriter on it
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // get the socket's input stream and open a BufferedReader on it
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send a String then terminate the line and flush
            writer.println(sendMessage); //WRITE
            System.out.println("Sender sent: " + sendMessage);

            String rcvMessage = reader.readLine(); //READ
            System.out.println("Sender received: " + rcvMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        send();
    }
}
