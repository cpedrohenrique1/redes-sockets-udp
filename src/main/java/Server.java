import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
/**
 <h1>Classe Server</h1>
 <p>Aqui o server recebe os numeros primos, salva em um arquivo</p>
 @author Pedro Henrique Brito da Silva Miranda
*/
public class Server {
    // Porta do servidor, tamanho do buffer, tamanho do intervalo de busca e limites do intervalo total
    private static final int PORT = 9999;
    private static final int BUFFER_SIZE = 4096;
    private static final int RANGE_SIZE = 5000;
    private static final int START = 1;
    private static final int END = 100000;

    // Lista para armazenar os numeros primos encontrados e fila de tarefas a serem processadas
    private static List<Integer> primes = Collections.synchronizedList(new ArrayList<>());
    private static Queue<int[]> tasks = new LinkedList<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) { // Cria um socket UDP na porta especificada
            System.out.println("Server started on port " + PORT);

            // Divide o intervalo total em tarefas menores e as adiciona a fila de tarefas
            for (int i = START; i <= END; i += RANGE_SIZE) {
                tasks.offer(new int[]{i, Math.min(i + RANGE_SIZE - 1, END)});
            }

            // Prepara um buffer para receber pacotes de dados
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Loop principal do servidor
            while (true) {
                socket.receive(packet); // Recebe um pacote de dados do cliente
                String request = new String(packet.getData(), 0, packet.getLength()); // Converte os dados recebidos em uma string
                JSONObject json = new JSONObject(request); // Converte a string em um objeto JSON

                // Obtem o endereço e a porta do cliente
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                // Processa a solicitacao do cliente
                if (json.getString("type").equals("request")) { // Se a solicitacao for de uma nova tarefa
                    if (!tasks.isEmpty()) { // Se ainda houver tarefas na fila
                        int[] range = tasks.poll(); // Obtem a proxima tarefa da fila
                        JSONObject response = new JSONObject();
                        response.put("type", "task"); // Define o tipo da resposta como "task"
                        response.put("range", range); // Adiciona o intervalo da tarefa a resposta
                        sendResponse(socket, response.toString(), clientAddress, clientPort); // Envia a resposta ao cliente
                    } else { // Se nao houver mais tarefas
                        JSONObject response = new JSONObject();
                        response.put("type", "done"); // Define o tipo da resposta como "done"
                        sendResponse(socket, response.toString(), clientAddress, clientPort); // Envia a resposta ao cliente
                    }
                } else if (json.getString("type").equals("result")) { // Se a solicitacao for um resultado de calculo de primos
                    JSONArray primesArray = json.getJSONArray("primes"); // Obtem a lista de numeros primos do resultado
                    synchronized (primes) {  // Sincroniza o acesso a lista de numeros primos
                        for (int i = 0; i < primesArray.length(); i++) {
                            primes.add(primesArray.getInt(i)); // Adiciona os numeros primos a lista global
                        }
                    }
                }

                if (tasks.isEmpty()) { // Se nao houver mais tarefas a serem processadas
                    savePrimesToFile(); // Salva os numeros primos encontrados em um arquivo
                    System.out.println("Total primes found: " + primes.size()); // Imprime a quantidade total de numeros primos encontrados
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo para enviar uma resposta ao cliente
    private static void sendResponse(DatagramSocket socket, String message, InetAddress address, int port) throws IOException {
        byte[] data = message.getBytes(); // Converte a mensagem em bytes
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port); // Cria um pacote UDP com os dados, endereco e porta do cliente
        socket.send(packet); // Envia o pacote ao cliente
    }

    // Metodo para salvar os numeros primos encontrados em um arquivo
    private static void savePrimesToFile() {
        try (PrintWriter writer = new PrintWriter("primes.txt")) { // Cria um escritor de arquivo
            for (int prime : primes) { // Itera sobre a lista de numeros primos
                writer.println(prime);  // Escreve cada numero primo em uma linha do arquivo
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}