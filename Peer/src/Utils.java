import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    /**
     * Sends a message using socket with random transport address.
     * After the message is sent, tcp connection closes.
     * Answer from receiver is not expected.
     *
     * @param destinationIpAddress
     * @param destinationPort
     * @param message
     */
    public static void sendMessage(String destinationIpAddress, Integer destinationPort, String message) {
    	
        try (Socket clientSocket = new Socket(destinationIpAddress, destinationPort)) {
        	
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            
            outToServer.println(message);//WRITE
            
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            //e.printStackTrace();
        	System.err.println("Hitile je exc!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String sendParentRequest(String destinationIpAddress, Integer destinationPort, String message) {
    	try (Socket clientSocket = new Socket(destinationIpAddress, destinationPort)) {
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            BufferedReader inRead=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            outToServer.println(message);
            //String line=inRead.readLine();
    	} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }

    /**
     * Sends a message using socket with random transport address.
     * After the message is sent, tcp connection closes.
     * Answer from receiver is not expected.
     *
     * @param transportAddress in "ipAddress;port" format
     * @param message content
     */
    public static void sendMessage(String transportAddress, String message) {
    	
        String[] components = transportAddress.split(";");
        String host = components[0];
        int port = Integer.parseInt(components[1]);
        
        sendMessage(host, port, message);
    }

    /**
     * Sends a message using random socket.
     * After the message is sent, socket expects answer from receiver.
     *
     * @param destinationIpAddress
     * @parvam destinationPort
     * @param message
     * @return receivers answer
     */
    public static List<String> sendMessageWithAnswer(String destinationIpAddress, Integer destinationPort, String message) {
        List<String> receivedAnswer = new ArrayList<>();
        destinationIpAddress = "localhost"; // TODO delete

        try (Socket clientSocket = new Socket(destinationIpAddress, destinationPort)) {
            PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            outToServer.println(message);//WRITE

            String line=null;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) break;

                receivedAnswer.add(line);
            }
            return receivedAnswer;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return receivedAnswer;
    }

    /**
     * Sends a message using random socket.
     * After the message is sent, socket expects answer from receiver.
     *
     * @param transportAddress in "ipAddress;port" format
     * @param message content
     * @return receivers answer
     */
    public static List<String> sendMessageWithAnswer(String transportAddress, String message) {
        String[] components = transportAddress.split(";");
        return sendMessageWithAnswer(components[0], Integer.parseInt(components[1]), message);
    }

    /**
     * Method for thread sleeping
     * @param ms
     */
    public static void sleep(int ms) {
//        System.out.println(" * * *  sleeping * * *  (" + ms + " ms)");
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignore) {
        }
    }
    /*
     * Method used when a new node enters network. 
     */
    public static String messageWithAns(String ip,Integer port,String message) {
    	String answer=null;
    	try(Socket clientSocket=new Socket(ip,port);){
    		PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            outToServer.println(message);
            answer=reader.readLine();
    	} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return answer;
    }
}
