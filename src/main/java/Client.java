import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
/**
 <h1>Classe cliente</h1>
 <p>Aqui o cliente encontra numeros primos e envia para o servidor</p>
 @author Pedro Henrique Brito da Silva Miranda
*/
public class Client {
    // Endereco e porta do servidor
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 9999; // Porta do servidor
    private static final int BUFFER_SIZE = 4096; // Tamanho do buffer para receber dados

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {  // Cria um socket UDP
            while (true) {
                // Cria um objeto JSON para solicitar uma nova tarefa ao servidor
                JSONObject request = new JSONObject();
                request.put("type", "request");
                sendRequest(socket, request.toString()); // Envia a solicitacao ao servidor

                // Prepara um buffer para receber a resposta do servidor
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet); // Recebe a resposta do servidor

                // Converte a resposta recebida em uma string e depois em um objeto JSON
                String response = new String(packet.getData(), 0, packet.getLength());
                JSONObject json = new JSONObject(response);

                // Verifica se o servidor indicou que nao ha mais tarefas
                if (json.getString("type").equals("done")) {
                    System.out.println("No more tasks. Exiting...");
                    break;
                }

                // Extrai o intervalo de numeros para calcular os primos da resposta do servidor
                JSONArray range = json.getJSONArray("range");
                int start = range.getInt(0);
                int end = range.getInt(1);
                List<Integer> primes = findPrimes(start, end); // Calcula os numeros primos no intervalo

                // Cria um objeto JSON com os resultados dos numeros primos encontrados
                JSONObject result = new JSONObject();
                result.put("type", "result");
                result.put("primes", primes);
                sendRequest(socket, result.toString()); // Envia os resultados ao servidor
            }
        } catch (IOException e) {
            e.printStackTrace(); // Imprime pilha em caso de erro
        }
    }

    // Metodo para enviar uma mensagem ao servidor
    private static void sendRequest(DatagramSocket socket, String message) throws IOException {
        byte[] data = message.getBytes(); // Converte a mensagem em bytes
        // Cria um pacote UDP com os dados, endereço e porta do servidor
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
        socket.send(packet); // Envia o pacote ao servidor
    }

    // Metodo para encontrar numeros primos em um intervalo
    private static List<Integer> findPrimes(int start, int end) {
        List<Integer> primes = new ArrayList<>(); // Lista para armazenar os numeros primos encontrados
        for (int num = start; num <= end; num++) // Itera sobre um intervalo
            if (isPrime(num))
                primes.add(num); // Adiciona os numeros primos na lista
        return primes; // Retorna a lista de numeros primos
    }

    // Metodo para verificar se um numero e primo
    private static boolean isPrime(int num) {
        if (num < 2)
            return false;
        for (int i = 2; i <= Math.sqrt(num); i++)
            if (num % i == 0)
                return false;
        return true;
    }
}