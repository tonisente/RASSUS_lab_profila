import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerListener implements Runnable, IServer {

    private static final int MINIMAL_NUMBER_OF_NEIGHBOURS = 2;
    private static final int NUMBER_OF_THREADS = 10;

private final ExecutorService executor;
    private Set<String> neighbours;
    private ServerSocket listenSocket;
    private AtomicBoolean runningFlag;

    PeerListener(Set<String> neighbours, ServerSocket listenSocket, AtomicBoolean runningFlag) {
        this.neighbours = neighbours;
        this.listenSocket = listenSocket;
        this.runningFlag = runningFlag;

        executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }

    /**
     * Checks if number of neighbours is greater than minimal value.
     * If it is, then contact the central coordinator and for list of
     */
    private void checkNeighbourStatus() {

    }

    /* runnbale */
    @Override
    public void run() {
        startup();
        executor.shutdown();
    }

    /* IServer */
    @Override
    public void startup() {
        /* Initialization that was done already in Peer */
        loop();
    }

    @Override
    public void loop() {
        System.out.println("PeerListener up and running!");

        while (runningFlag.get()) {
            try {
                 Socket clientSocket = listenSocket.accept();//ACCEPT

                 // execute a tcp request handler in a new thread
                 Runnable worker = new PeerListenerWorker(clientSocket, runningFlag);
                 executor.execute(worker);
            } catch (SocketTimeoutException ignore_and_continue) {
            } catch (IOException ex) {
                System.err.println("Exception caught when trying to read or send data: " + ex);
            } //clientSocket CLOSE
        }
    }

    @Override
    public void shutdown() {
        // should check if there is still active connection, but in this case just skip that step
    }

    @Override
    public boolean getRunningFlag() {
        return runningFlag.get();
    }

    @Override
    public void setRunningFlag(boolean flag) {
        runningFlag.set(true);
    }
}
