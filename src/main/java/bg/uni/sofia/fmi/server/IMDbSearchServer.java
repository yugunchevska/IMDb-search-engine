package bg.uni.sofia.fmi.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import bg.uni.sofia.fmi.server.util.IMDbSearchUtil;

public class IMDbSearchServer implements AutoCloseable {

	private Selector selector;
	private OMDbManager omdbManager = new OMDbManager();

	public IMDbSearchServer(int port) throws IOException {
		ServerSocketChannel socketChannel = ServerSocketChannel.open();
		socketChannel.configureBlocking(false);

		ServerSocket serverSocket = socketChannel.socket();
		serverSocket.bind(new InetSocketAddress(port));

		selector = Selector.open();
		socketChannel.register(selector, SelectionKey.OP_ACCEPT);
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
					acceptKey(key);
				}

				String command = IMDbSearchUtil.getCommand(key);
				if(command == null || command.isEmpty()) {
					continue;
				}
				String movieInfoPath = IMDbSearchUtil.getMovieInfoFilepath(command, omdbManager);
				IMDbSearchUtil.sendContentToClient(movieInfoPath);

				iteratorKeys.remove();
			}
		}

	}

	public void acceptKey(SelectionKey key) throws IOException {

		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = ssc.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);

		System.out.println("Client " + socketChannel + " connected.");
	}

	@Override
	public void close() {
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				System.out.println("Couldn't close the Selector. Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {

		try (IMDbSearchServer server = new IMDbSearchServer(4444)) {
			server.start();
		} catch (Exception e) {
			System.out.println("Exception found.");
			e.printStackTrace();
		}

	}

}