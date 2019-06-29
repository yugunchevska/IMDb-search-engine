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

	private final String MAIN_PATH = "D:" + File.separator + "”ÌË" + File.separator + "Eclipse new" + File.separator
			+ "Project vol.2" + File.separator + "Client";

	public IMDbSearchClient(int port) {

		try {
			Socket s = new Socket("localhost", port);
			PrintWriter pw = new PrintWriter(s.getOutputStream());
			Scanner sc = new Scanner(System.in);

			BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String readLine;

			boolean foo = true;

			while (foo) {
				System.out.println("Enter a command: ");

				readLine = sc.nextLine();
				if (readLine.equals("quit")) {
					foo = false;
					break;
				}

				pw.write(readLine);
				pw.flush();
				
				String str = readFromServer(s, br);
				System.out.println(str);
			}

			pw.close();
			br.close();
			s.close();
			sc.close();

		} catch (UnknownHostException e) {
			System.out.println("UnknownHostException found.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException found.");
			e.printStackTrace();
		}
	}

	private String readFromServer(Socket s, BufferedReader br) throws FileNotFoundException, IOException {
		String line;

		while (!(line = br.readLine()).equals("yuli")) {

			if (line.endsWith(".jpg")) {

				String title = line.replaceAll("\n", "");
				String path = MAIN_PATH + File.separator + title;
				saveImage(s, path);
				System.out.println(path);
				
				break;
			}
		}
		
	  return line;
	}
	
	private void saveImage(Socket s, String path) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[400_000];
		int n = 0;
		n = s.getInputStream().read(buffer);
		baos.write(buffer, 0, n);

		File myFile = new File(path);
		OutputStream os = new FileOutputStream(myFile);
		os.write(buffer);
		
		os.close();
	}

	public static void main(String[] args) {

		IMDbSearchClient client = new IMDbSearchClient(4444);
	}

}
