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

	public IMDbSearchClient(int port) {

		try {
			Socket clientSocket = new Socket("localhost", port);
			PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
			Scanner scanner = new Scanner(System.in);			
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

			writer.close();
			clientSocket.close();
			scanner.close();

		} catch (UnknownHostException e) {
			System.out.println("UnknownHostException found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException found.");
			e.printStackTrace();
		}
	}

	private String readFromServer(Socket clientSocket) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		String movieInfo;
		
		while (!(movieInfo = reader.readLine()).equals("EndOfFile")) {
		
			if (movieInfo.endsWith(".jpg")) {
		
				String movieTitle = movieInfo.replaceAll("\n", "");
				String imagePath = POSTER_PATH + File.separator + movieTitle;
				saveImage(clientSocket, imagePath);
				System.out.println(imagePath);
				
				break;
			}
		}		
		reader.close();
			
		return movieInfo;
	}
	
	private void saveImage(Socket clientSocket, String imagePath) throws IOException {
		ByteArrayOutputStream bufferWriter = new ByteArrayOutputStream();
		byte[] image = new byte[400_000];
		int n = 0;
		n = clientSocket.getInputStream().read(image);
		bufferWriter.write(image, 0, n);

		File myFile = new File(imagePath);
		OutputStream fileWriter = new FileOutputStream(myFile);
		fileWriter.write(image);
		
		bufferWriter.close();
		fileWriter.close();
	}

	public static void main(String[] args) {

		IMDbSearchClient client = new IMDbSearchClient(4444);
	}

}
