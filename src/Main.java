public class Main {

    public static void main(String[] args) {
        TCPReceiver receiver = new TCPReceiver(8080, "localhost");
        TCPSender sender = new TCPSender(8080, "localhost");

        new Thread(receiver).start();

        try {
            System.out.println("zzz .... ");
            Thread.sleep(2000);
        } catch (InterruptedException ignorable) {}

        new Thread(sender).start();
    }
}
