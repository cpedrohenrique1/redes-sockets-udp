import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Server {
    private static final int PORT = 9999;
    private static final int BUFFER_SIZE = 4096;
    private static final int RANGE_SIZE = 5000;
    private static final int START = 1;
    private static final int END = 100000;

    private static List<Integer> primes = Collections.synchronizedList(new ArrayList<>());
    private static Queue<int[]> tasks = new LinkedList<>();
    private static Map<InetAddress, Integer> clientPorts = new HashMap<>();

    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            for (int i = START; i <= END; i += RANGE_SIZE) {
                tasks.offer(new int[]{i, Math.min(i + RANGE_SIZE - 1, END)});
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength());
                JSONObject json = new JSONObject(request);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                clientPorts.put(clientAddress, clientPort);

                if (json.getString("type").equals("request")) {
                    if (!tasks.isEmpty()) {
                        int[] range = tasks.poll();
                        JSONObject response = new JSONObject();
                        response.put("type", "task");
                        response.put("range", new JSONArray(range));
                        sendResponse(socket, response.toString(), clientAddress, clientPort);
                    } else {
                        JSONObject response = new JSONObject();
                        response.put("type", "done");
                        sendResponse(socket, response.toString(), clientAddress, clientPort);
                    }
                } else if (json.getString("type").equals("result")) {
                    JSONArray primesArray = json.getJSONArray("primes");
                    synchronized (primes) {
                        for (int i = 0; i < primesArray.length(); i++) {
                            primes.add(primesArray.getInt(i));
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    for (Map.Entry<InetAddress, Integer> entry : clientPorts.entrySet()) {
                        JSONObject response = new JSONObject();
                        response.put("type", "done");
                        sendResponse(socket, response.toString(), entry.getKey(), entry.getValue());
                    }
                    savePrimesToFile();
                    System.out.println("Total primes found: " + primes.size());
                    break;
                }
            }
        }
    }

    private static void sendResponse(DatagramSocket socket, String message, InetAddress address, int port) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    private static void savePrimesToFile() {
        try (PrintWriter writer = new PrintWriter("primes.txt")) {
            for (int prime : primes) {
                writer.println(prime);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}