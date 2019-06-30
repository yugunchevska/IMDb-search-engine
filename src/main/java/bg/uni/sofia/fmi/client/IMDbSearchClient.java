package bg.uni.sofia.fmi.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class IMDbSearchClient {

	private static final String POSTER_PATH = "Client" + File.separator + "Posters";

	public IMDbSearchClient(int port) throws IOException {

		try (Socket clientSocket = new Socket("localhost", port);
				PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
				Scanner scanner = new Scanner(System.in)) {

			String command;
			boolean running = true;
			while (running) {
				System.out.println("Enter a command: ");

				command = scanner.nextLine();
				if (command.equals("quit")) {
					running = false;
					break;
				}

				writer.write(command);
				writer.flush();

				System.out.println(readFromServer(clientSocket));
			}
		} catch (IOException e) {
			System.out.println("Couldn't open a socket. Reason: " + e.getMessage());
			throw new IOException(e);
		}
	}

	private String readFromServer(Socket clientSocket) throws IOException {
		String movieInfo;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
			while (!(movieInfo = reader.readLine()).equals("EndOfFile")) {

				if (movieInfo.endsWith(".jpg")) {

					String movieTitle = movieInfo.replaceAll("\n", "");
					String imagePath = POSTER_PATH + File.separator + movieTitle;
					saveImage(clientSocket, imagePath);
					System.out.println(imagePath);

					break;
				}
			}

			return movieInfo;
		} catch (IOException e) {
			System.out.println("Couldn't read from the server. Reason: " + e.getMessage());
			throw new IOException(e);
		}

	}

	private void saveImage(Socket clientSocket, String imagePath) throws IOException {
		File imageFile = new File(imagePath);
		try (ByteArrayOutputStream bufferWriter = new ByteArrayOutputStream();
				OutputStream fileWriter = new FileOutputStream(imageFile)) {

			byte[] image = new byte[400_000];
			int imageLength = clientSocket.getInputStream().read(image);
			bufferWriter.write(image, 0, imageLength);

			fileWriter.write(image);
		} catch (IOException e) {
			System.out.println("Couldn't save poster. Reason: " + e.getMessage());
			throw new IOException(e);
		}
	}

	public static void main(String[] args) throws IOException {

		IMDbSearchClient client = new IMDbSearchClient(4444);
	}

}
