package bg.uni.sofia.fmi.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class Server implements AutoCloseable {

	private Selector selector;
	private Extraction extraction = new Extraction();

	public Server(int port) throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ServerSocket ss = ssc.socket();
		ss.bind(new InetSocketAddress(port));

		selector = Selector.open();
		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void start() throws IOException {

		while (true) {
			int readyChannels = selector.select();

			if (readyChannels == 0) {
				continue;
			}

			Set<SelectionKey> selectionKyes = selector.selectedKeys();
			Iterator<SelectionKey> iteratorKeys = selectionKyes.iterator();

			while (iteratorKeys.hasNext()) {
				SelectionKey key = iteratorKeys.next();

				if (key.isAcceptable()) {
					this.accept(key);
				}

				readFromClient(key);

				iteratorKeys.remove();
			}
		}

	}

	public void accept(SelectionKey key) throws IOException {

		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);

		System.out.println("Client " + sc + " connected.");
	}

	public void close() {
		if (selector != null)
			try {
				selector.close();
			} catch (IOException e) {
				System.out.println("Exception found on closing the server.");
				e.printStackTrace();
			}
	}

	public void readFromClient(SelectionKey key) throws IOException {

		if (key.isReadable()) {

			SocketChannel sc = (SocketChannel) key.channel();

			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(10_000);
				buffer.clear();
				int r = sc.read(buffer);

				if (r <= 0) {
					break;
				}

				buffer.flip();

				String path = getPath(buffer);

				if (path.endsWith(".txt") || path.endsWith(".jpg")) {

					File myFile = new File(path);

					// transfer the image to the client
					if (path.endsWith(".jpg")) {

						sc.write(ByteBuffer.wrap((path + "\n" + "yuli" + "\n").getBytes()));

						InputStream is = new FileInputStream(myFile);
						byte[] imageBuffer = new byte[is.available()];
						is.read(imageBuffer);

						ByteBuffer image = ByteBuffer.allocate(400_000);
						image.clear();
						image.put(imageBuffer);
						image.flip();
						sc.write(image);

						is.close();
					} else { // read from the text file and provide the contain to the client

						try (BufferedReader br = new BufferedReader(new FileReader(myFile))) {
							String line;

							while ((line = br.readLine()) != null) {

								sc.write(ByteBuffer.wrap((line + "\n").getBytes()));
							}
							sc.write(ByteBuffer.wrap(("yuli" + "\n").getBytes()));

						} catch (Exception e) {
							System.out.println("Exception found on the transfer.");
							e.printStackTrace();
						}
					}
				} else { // the title or command is incorrect
					sc.write(ByteBuffer.wrap((path + "\n" + "yuli" + "\n").getBytes()));
				}
			}
		}

	}

	public String getPath(ByteBuffer buffer) throws IOException {

		// transform the buffer to a string
		CharBuffer cb = StandardCharsets.UTF_8.decode(buffer);
		String str = cb.toString();
		String[] result = str.split(" ");

		switch (result[0]) {
		case ("get-movie"):
			return extraction.getMovie(result);
		case ("get-movies"):
			return extraction.getMovies(result);
		case ("get-tv-series"):
			return extraction.getTVShow(result);
		case ("get-movie-poster"):
			return extraction.getPoster(result);
		default:
			return "Wrong commmand";
		}

	}

	public static void main(String[] args) {

		try (Server server = new Server(4444)) {
			server.start();
		} catch (Exception e) {
			System.out.println("Exception found.");
			e.printStackTrace();
		}

	}

}