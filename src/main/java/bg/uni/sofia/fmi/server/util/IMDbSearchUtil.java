package bg.uni.sofia.fmi.server.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import bg.uni.sofia.fmi.server.OMDbManager;

public class IMDbSearchUtil {

	private static SocketChannel socket;

	public static String getCommand(SelectionKey key) throws IOException {
		
		String command = null;

		if (key.isReadable()) {
			socket = (SocketChannel) key.channel();
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(10_000);
				buffer.clear();
				int r = socket.read(buffer);
				if (r <= 0) {
					break;
				}
				buffer.flip();

				command = StandardCharsets.UTF_8.decode(buffer).toString();
			}
		}

		return command;
	}

	public static String getMovieInfoFilepath(String command, OMDbManager omdbManager) throws IOException {
		
		String[] commands = command.split(" ");

		switch (commands[0]) {
		case ("get-movie"):
			return omdbManager.getMovie(commands);
		case ("get-movies"):
			return omdbManager.getMovies(commands);
		case ("get-tv-series"):
			return omdbManager.getTVSeries(commands);
		case ("get-movie-poster"):
			return omdbManager.getPoster(commands);
		default:
			return "Wrong commmand: " + command;
		}

	}

	public static void sendContentToClient(String movieInfoPath) throws IOException {
		
		File movieInfoFile = new File(movieInfoPath);

		if (movieInfoPath.endsWith(".txt")) {

			try (BufferedReader reader = new BufferedReader(new FileReader(movieInfoFile))) {
				String line;

				while ((line = reader.readLine()) != null) {

					socket.write(ByteBuffer.wrap((line + "\n").getBytes()));
				}
				socket.write(ByteBuffer.wrap(("EndOfFile" + "\n").getBytes()));

			} catch (IOException e) {
				System.out.println("Could't send information about the movie. Reason: " + e.getMessage());
				throw new IOException(e);
			}
		} else if (movieInfoPath.endsWith(".jpg")) {

			socket.write(ByteBuffer.wrap((movieInfoPath + "\n" + "EndOfFile" + "\n").getBytes()));

			try (InputStream reader = new FileInputStream(movieInfoFile)) {
				byte[] imageBuffer = new byte[reader.available()];
				reader.read(imageBuffer);

				ByteBuffer image = ByteBuffer.allocate(400_000);
				image.clear();
				image.put(imageBuffer);
				image.flip();
				socket.write(image);
			} catch (IOException e) {
				System.out.println("Couldn't send movie's poster. Reason: " + e.getMessage());
				throw new IOException(e);
			}

		} else {
			socket.write(ByteBuffer.wrap((movieInfoPath + "\n" + "EndOfFile" + "\n").getBytes()));
		}
	}

}
