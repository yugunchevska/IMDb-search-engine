package bg.uni.sofia.fmi.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class OMDbManager {

	private static final String MOVIE_INFO_PATH = "Server" + File.separator + "MoviesInforamtion";
	private static final String POSTER_PATH = "Server" + File.separator + "Posters";
	
	private static final String FIELDS = "--fields=";
	private static final String SEASON_FIELD = "--season=";
	private static final String ACTORS_FIELD = "--actors="; 
	private static final String ORDER_FIELD = "--order=";
	private static final String GENRES_FIELD = "--genres=";
	
	private static final String ERROR_MOVIE = "{\"Response\":\"False\",\"Error\":\"Movie not found!\"}";
	private static final String ERROR_SEASON = "{\"Response\":\"False\",\"Error\":\"Series or season not found!\"}";

	public String getMovie(String[] command) throws IOException {
		String title = getTitle(command, FIELDS);
		String titlePath = replaceSpecialCharacters(title);
		int fieldsIndex = getIndex(command, FIELDS);

		String movieInfoPath;
		if (fieldsIndex == 0) {
			movieInfoPath = MOVIE_INFO_PATH + File.separator + titlePath + ".txt";
		} else {
			String substring = removeCommas(fieldsIndex, command);
			movieInfoPath = MOVIE_INFO_PATH + File.separator + titlePath + " " + substring + ".txt";
		}
		
		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {
			movieInfoFile.createNewFile();
			URL omdbURL = createURL(title);

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()));
			BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile));) {
				
				String inputLine;
				while ((inputLine = reader.readLine()) != null) {
					if (inputLine.equals(ERROR_MOVIE)) {
						movieInfoFile.delete();
						return "Movie not found.";
					}

					if (fieldsIndex == 0) {
						writer.write(inputLine);
					} else {
						findFields(inputLine, command, fieldsIndex, writer);
					}
				}
			} catch(IOException e) {
				System.out.println("Couldn't take information from OMDb API. Reason: " + e.getMessage());
				throw new IOException(e);
			}
		}

		return movieInfoPath;
	}

	public String getTVSeries(String[] command) throws IOException {

		String title = getTitle(command, SEASON_FIELD);
		String titlePath = replaceSpecialCharacters(title);
		int seasonIndex = getIndex(command, SEASON_FIELD);

		String movieInfoPath;
		if (seasonIndex == 0) {
			movieInfoPath = MOVIE_INFO_PATH + File.separator + titlePath + ".txt";
		} else {
			movieInfoPath = MOVIE_INFO_PATH + File.separator + titlePath + " season " + command[seasonIndex + 1] + ".txt";
		}

		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {

			movieInfoFile.createNewFile();
			URL omdbURL;
			if (seasonIndex == 0) {
				omdbURL = createURL(title);
			} else {
				String url = "http://www.omdbapi.com/";
				String charset = "UTF-8";
				String query = String.format("t=%s&Season=%s&apikey=6c5a486c", URLEncoder.encode(title, charset),
						URLEncoder.encode(command[seasonIndex + 1], charset));
				omdbURL = new URL(url + "?" + query);
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()));
					BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile))) {
				
				String inputLine;
				while ((inputLine = reader.readLine()) != null) {
					if (seasonIndex == 0) {
						if (inputLine.equals(ERROR_MOVIE)) {
							movieInfoFile.delete();
							return "Series not found.";
						}
						
						writer.write(inputLine);
					} else {

						if (inputLine.equals(ERROR_SEASON)) {
							movieInfoFile.delete();
							return "Series or season not found.";
						}

						titlePerEpisode(inputLine, writer);
					}
				}
			} catch (IOException e) {
				System.out.println("Couldn't take information from OMDb API. Reason: " + e.getMessage());
				throw new IOException(e);
			}

		}
		return movieInfoPath;
	}

	public String getMovies(String[] command) throws IOException {

		int genresFieldIndex = 0, actorsFieldIndex = 0, orderFieldIndex = 0;
		for (int i = 1; i < command.length; ++i) {
			if (command[i].equals(ACTORS_FIELD)) {
				actorsFieldIndex = i;
				continue;
			}
			if (command[i].equals(ORDER_FIELD)) {
				orderFieldIndex = i;
				continue;
			}
			if (command[i].equals(GENRES_FIELD)) {
				genresFieldIndex = i;
				continue;
			}
		}

		if (genresFieldIndex != 0) {
			command[genresFieldIndex + 1] = command[genresFieldIndex + 1].replace(",", "");
		}
		command[actorsFieldIndex + 2] = command[actorsFieldIndex + 2].replace(",", "");

		String movieInfoPath = MOVIE_INFO_PATH + File.separator + "Movies with " + command[actorsFieldIndex + 1] + " " + command[actorsFieldIndex + 3]
				+ ".txt";

		File movieInfoFile = new File(movieInfoPath);
		movieInfoFile.createNewFile();

		Path movieInfoDir = Paths.get(MOVIE_INFO_PATH);
		double imdbRating = 0;
		Map<Double, String> movies = new HashMap<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(movieInfoDir, "*.txt");
				BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile))) {

			for (Path entry : stream) {
				filtringMovies(entry, genresFieldIndex, actorsFieldIndex, command, movies, imdbRating);
			}

			if (movies.isEmpty()) {
				return "There are none movies with these actors and gernes.";
			}

			if (orderFieldIndex != 0) {		
				if (!orderingMovies(movies, command, orderFieldIndex, movieInfoFile)) {
					return "Wrong command on field \"order\".";
				}
			} else {
				for (Map.Entry<Double, String> entry : movies.entrySet()) {
					writer.write(entry.getValue());
					writer.newLine();
				}
			}
		} catch (IOException e) {
			System.out.println("Couldn't get information from the server. Reason: " + e.getMessage());
			throw new IOException(e);
		}

		return movieInfoPath;
	}

	public String getPoster(String[] command) throws IOException {

		String title = getTitle(command, "");
		String titlePath = replaceSpecialCharacters(title);

		String movieInfoPath = POSTER_PATH + File.separator + titlePath + ".jpg";
		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {

			movieInfoFile.createNewFile();
			URL omdbURL = createURL(title);

			String inputLine;
			String posterURL = "";
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()))) {

				while ((inputLine = reader.readLine()) != null) {
	
					if (inputLine.equals(ERROR_MOVIE)) {
						movieInfoFile.delete();
						return "Movie not found.";
					}
					posterURL = findPosterURL(inputLine);
				}
				
				downloadPoster(posterURL, movieInfoFile);
			} catch (IOException e) {
				System.out.println("Couldn't download the movie's poster. Reason: " + e.getMessage());
				throw new IOException(e);
			}
		}

		return movieInfoFile.getName();
	}

	public String getTitle(String[] command, String stopword) {
		String title = "";

		for (int i = 1; i < command.length; ++i) {

			if (command[i].equals(stopword)) {
				break;
			}
			title += command[i];
			title += " ";
		}

		return title;
	}

	public String replaceSpecialCharacters(String title) {
		return title.replaceAll(":|\\?|\\*|\"", "_");
	}

	public int getIndex(String[] command, String field) {
		int fieldIndex = 0;
		for (int i = 1; i < command.length; ++i) {
			if (command[i].equals(field)) {
				fieldIndex = i;
				break;
			}
		}
		return fieldIndex;
	}

	public String removeCommas(int crr, String[] command) {
		String str = "";
		int i = crr + 1;
		while (i != command.length) {
			command[i] = command[i].replaceAll(",", "");
			str += command[i];
			str += " ";
			++i;
		}
		return str;
	}

	public URL createURL(String title) throws MalformedURLException, UnsupportedEncodingException {

		String url = "http://www.omdbapi.com/";
		String charset = "UTF-8";
		String query = String.format("t=%s&apikey=6c5a486c", URLEncoder.encode(title, charset));

		return new URL(url + "?" + query);
	}

	public void findFields(String inputLine, String[] command, int crr, BufferedWriter writer) throws IOException {

		inputLine = inputLine.replaceAll("\\{|\\}", "");
		String[] word = inputLine.split(":|\\,");

		for (int i = 0; i < word.length; ++i) {
			word[i] = word[i].replaceAll("\"", "");
			int j = crr + 1;
			while (j != command.length) {
				if (word[i].equals(command[j])) {

					writer.write(word[i] + " ");
					++i;
					word[i] = word[i].replaceFirst("\"", "");

					while (word[i].indexOf("\"") != word[i].length() - 1) {
						writer.write(word[i] + ",");
						++i;
					}
					word[i] = word[i].replaceAll("\"", "");
					writer.write(word[i]);
					writer.newLine();
				}
				++j;
			}
		}
	}

	public void titlePerEpisode(String inputLine, BufferedWriter writer) throws IOException {

		inputLine = inputLine.replaceAll("\\{|\\}|\\[", "");
		String[] word = inputLine.split(":|\\,");
		String TITLE = "Title";

		for (int i = 1; i < word.length; ++i) {
			word[i] = word[i].replaceAll("\"", "");

			if (word[i].equals(TITLE)) {

				++i;
				word[i] = word[i].replaceFirst("\"", "");

				while (word[i].indexOf("\"") != word[i].length() - 1) {
					writer.write(word[i] + ",");
					++i;
				}
				word[i] = word[i].replaceAll("\"", "");
				writer.write(word[i]);
				writer.newLine();
			}
		}
	}

	public void filtringMovies(Path entry, int genres, int actors, String[] command, Map<Double, String> movies,
			double imdbRating) throws FileNotFoundException, IOException {

		int counter = 0;
		String inputLine;
		int countGenres = 0, countActors = 0;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(entry.toString()))) {	
	
			while ((inputLine = reader.readLine()) != null) {
	
				String[] words = inputLine.split(":|\\,");
	
				for (int i = 0; i < words.length; ++i) {
					words[i] = words[i].replaceAll("\"", "");
					words[i] = words[i].replaceAll(" ", "");
	
					if (genres != 0) {
	
						if (words[i].equals(command[genres + 1]) || words[i].equals(command[genres + 2])) {
							++countGenres;
						}
					}
	
					if (words[i].equals(command[actors + 1] + command[actors + 2])
							|| words[i].equals(command[actors + 3] + command[actors + 4])) {
						++countActors;
					}
	
					if (words[i].equals("imdbRating")) {
						words[i + 1] = words[i + 1].replaceAll("\"", "");
						imdbRating = Double.parseDouble(words[i + 1].replace(",", "."));
					}
				}
	
				if (genres != 0 && countGenres == 2) {
					++counter;
				}
	
				if ((countActors == 2 && genres != 0 && counter != 0) || (countActors == 2 && genres == 0)) {
					movies.put(imdbRating, entry.getFileName().toString().replaceFirst("[.][^.]+$", ""));
				}
			}
		}
	}

	public boolean orderingMovies(Map<Double, String> movies, String[] command, int orderIndex, File movieInfoFile)
			throws IOException {

		Map<Double, String> sortedMovies;

		if (command[orderIndex + 1].equals("asc")) {
			sortedMovies = new TreeMap<>(movies);
		} else if (command[orderIndex + 1].equals("desc")) {
			sortedMovies = new TreeMap<>(Collections.reverseOrder());
			for (Map.Entry<Double, String> entry : movies.entrySet()) {
				sortedMovies.put(entry.getKey(), entry.getValue());
			}
		} else {
			return false;
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile))) {
			for (Double key : sortedMovies.keySet()) {
				writer.write(sortedMovies.get(key));
				writer.newLine();
			}
		}

		return true;
	}

	public String findPosterURL(String inputLine) {

		String posterURL = "";
		String[] words = inputLine.split(":|\\,");
		for (int i = 0; i < words.length; ++i) {
			words[i] = words[i].replaceAll("\"", "");

			if (words[i].equals("Poster")) {

				++i;
				words[i] = words[i].replaceFirst("\"", "");
				posterURL += words[i];
				posterURL += ":";
				++i;
				words[i] = words[i].replace("\"", "");
				posterURL += words[i];
			}
		}
		return posterURL;
	}

	public void downloadPoster(String posterURL, File movieInfoFile) throws MalformedURLException, IOException {
		URL poster = new URL(posterURL);

		try (InputStream reader = poster.openStream();
				OutputStream writer = new FileOutputStream(movieInfoFile)) {

			byte[] b = new byte[2048];
			int length;
	
			while ((length = reader.read(b)) != -1) {
				writer.write(b, 0, length);
			}
		}
	}
}
