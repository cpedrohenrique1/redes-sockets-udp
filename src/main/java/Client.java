import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            boolean running = true;
            while (running) {
                // Solicita uma tarefa ao servidor
                JSONObject request = new JSONObject();
                request.put("type", "request");
                sendRequest(socket, request.toString());

                // Recebe a resposta do servidor
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String response = new String(packet.getData(), 0, packet.getLength());
                JSONObject json = new JSONObject(response);

                if (json.getString("type").equals("done")) {
                    System.out.println("No more tasks. Exiting...");
                    running = false;
                } else if (json.getString("type").equals("task")) {
                    JSONArray range = json.getJSONArray("range");
                    int start = range.getInt(0);
                    int end = range.getInt(1);

                    // Calcula os números primos no intervalo
                    List<Integer> primes = findPrimes(start, end);

                    // Envia os resultados ao servidor
                    JSONObject result = new JSONObject();
                    result.put("type", "result");
                    result.put("primes", new JSONArray(primes));
                    sendRequest(socket, result.toString());
                }
            }
        }
    }

    private static void sendRequest(DatagramSocket socket, String message) throws IOException {
        byte[] data = message.getBytes();
        InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_PORT);
        socket.send(packet);
    }

    private static List<Integer> findPrimes(int start, int end) {
        List<Integer> primes = new ArrayList<>();
        for (int num = start; num <= end; num++) {
            if (isPrime(num)) {
                primes.add(num);
            }
        }
        return primes;
    }

    private static boolean isPrime(int num) {
        if (num < 2) {
            return false;
        }
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) {
                return false;
            }
        }
        return true;
    }
}