import java.io.*;
import java.net.*;

public class EquationServer {

    public static void main(String[] args) {
        String ipAddress = "172.20.10.5"; // Replace with server's IP address
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress))) {
            System.out.println("Server is listening on " + ipAddress + " port " + port);
            while (true) {
                new SolverThread(serverSocket.accept()).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class SolverThread extends Thread {
    private Socket socket;

    public SolverThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             ObjectInputStream objectInputStream = new ObjectInputStream(input);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(output)) {

            // Read the matrix (coefficients) from the client
            double[][] coefficients = (double[][]) objectInputStream.readObject();
            double[] constants = (double[]) objectInputStream.readObject();

            // Solve the equations
            double[] results = solveEquations(coefficients, constants);

            // Send back the results
            objectOutputStream.writeObject(results);

        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private double[] solveEquations(double[][] a, double[] b) {
        int n = b.length;
        double[] x = new double[n];
        double[][] augmentedMatrix = new double[n][n + 1];

        // Construct the augmented matrix
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, augmentedMatrix[i], 0, n);
            augmentedMatrix[i][n] = b[i];
        }

        // Apply Gaussian elimination
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(augmentedMatrix[j][i]) > Math.abs(augmentedMatrix[max][i])) {
                    max = j;
                }
            }

            double[] temp = augmentedMatrix[i];
            augmentedMatrix[i] = augmentedMatrix[max];
            augmentedMatrix[max] = temp;

            for (int j = i + 1; j < n; j++) {
                double factor = augmentedMatrix[j][i] / augmentedMatrix[i][i];
                for (int k = i; k < n + 1; k++) {
                    augmentedMatrix[j][k] -= factor * augmentedMatrix[i][k];
                }
            }
        }

        // Perform back substitution
        for (int i = n - 1; i >= 0; i--) {
            x[i] = augmentedMatrix[i][n] / augmentedMatrix[i][i];
            for (int j = i - 1; j >= 0; j--) {
                augmentedMatrix[j][n] -= augmentedMatrix[j][i] * x[i];
            }
        }

        return x;
    }
}
