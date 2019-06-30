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
		// if the file does not exists, create a new one and fill it with information
		// from the omdb api
		if (!movieInfoFile.isFile()) {
			try {
				movieInfoFile.createNewFile();
				URL omdbURL = createURL(title);

				BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()));
				BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile));
				String inputLine;

				while ((inputLine = reader.readLine()) != null) {

					// check if the movie title is incorrect
					if (inputLine.equals(ERROR_MOVIE)) {
						// delete the .txt file and return message to the client
						writer.close();
						reader.close();
						movieInfoFile.delete();
						return "Movie not found.";
					}

					if (fieldsIndex == 0) {
						writer.write(inputLine);
					} else {
						// find the wanted fields and then find the fields' content
						findFields(inputLine, command, fieldsIndex, writer);
					}
				}
				reader.close();
				writer.close();
			} catch (MalformedURLException e) {
				return "This is not a valid URL.";
			} catch (IOException e) {
				System.out.println("IOException found in the \"get-movie\" command.");
				e.printStackTrace();
			}
		}

		return movieInfoPath;
	}

	public String getTVSeries(String[] command) {

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

			try {
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

				BufferedReader in = new BufferedReader(new InputStreamReader(omdbURL.openStream()));

				BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					// check if the series or the season are correct
					if (seasonIndex == 0) {
						if (inputLine.equals(ERROR_MOVIE)) {
							// delete the .txt file and return message to the client
							writer.close();
							in.close();
							movieInfoFile.delete();
							return "Series not found.";
						}
						// if everything is correct, take it from the api
						writer.write(inputLine);
					} else {

						if (inputLine.equals(ERROR_SEASON)) {
							// delete the .txt file and return message to the client
							writer.close();
							in.close();
							movieInfoFile.delete();
							return "Series or season not found.";
						}

						// take the titles of the episodes
						titlePerEpisode(inputLine, writer);
					}
				}
				in.close();
				writer.close();

			} catch (MalformedURLException e) {
				return "This is not a valid URL.";

			} catch (IOException e) {
				System.out.println("IOException found in the \"get-tv-series\" command.");
				e.printStackTrace();
			}

		}
		return movieInfoPath;
	}

	public String getMovies(String[] command) {

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

		// remove "," from the command
		if (genresFieldIndex != 0) {
			command[genresFieldIndex + 1] = command[genresFieldIndex + 1].replace(",", "");
		}
		command[actorsFieldIndex + 2] = command[actorsFieldIndex + 2].replace(",", "");

		String movieInfoPath = MOVIE_INFO_PATH + File.separator + "Movies with " + command[actorsFieldIndex + 1] + " " + command[actorsFieldIndex + 3]
				+ ".txt";
		// create new file using the name of the actors, because they are always include
		File movieInfoFile = new File(movieInfoPath);
		try {
			movieInfoFile.createNewFile();

		} catch (IOException e1) {
			System.out.println("Creating new file failed.");
			e1.printStackTrace();
		}

		// getting all files that ends with .txt
		Path movieInfoDir = Paths.get(MOVIE_INFO_PATH);
		double imdbRating = 0;
		Map<Double, String> movies = new HashMap<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(movieInfoDir, "*.txt")) {

			BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile));
			// finding the movies with that actors/genres and putting them in the hashMap
			for (Path entry : stream) {
				filtringMovies(entry, genresFieldIndex, actorsFieldIndex, command, movies, imdbRating);
			}

			if (movies.isEmpty()) {
				writer.close();
				return "There are none movies with these actors and gernes.";
			}

			// ordering the movies by imdbRating if the client wants it
			if (orderFieldIndex != 0) {		
				if (!orderingMovies(movies, command, orderFieldIndex, writer)) {
					return "Wrong command on field \"order\".";
				}
			} else {
				for (Map.Entry<Double, String> entry : movies.entrySet()) {
					writer.write(entry.getValue());
					writer.newLine();
				}
			}

			writer.close();
		} catch (IOException e) {
			System.out.println("IOException found in the \"get-movies\" command.");
			e.printStackTrace();
		} catch (DirectoryIteratorException ex) {
			System.out.println("DirectoryIteratorException found in the \"get-movies\" command.");
			ex.printStackTrace();
		}

		return movieInfoPath;
	}

	public String getPoster(String[] command) {

		// take the title
		String title = getTitle(command, "");
		String titlePath = replaceSpecialCharacters(title);

		// create a path and a file
		String movieInfoPath = POSTER_PATH + File.separator + titlePath + ".jpg";
		File movieInfoFile = new File(movieInfoPath);

		if (!movieInfoFile.isFile()) {

			try {
				movieInfoFile.createNewFile();
				URL omdbURL = createURL(title);

				BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()));
				String inputLine;
				String posterURL = "";

				// searching for the Poster's URL
				while ((inputLine = reader.readLine()) != null) {

					// if the title is not correct
					if (inputLine.equals(ERROR_MOVIE)) {
						// delete the .txt file and return message to the client
						reader.close();
						movieInfoFile.delete();
						return "Movie not found.";
					}

					posterURL = findPosterURL(inputLine);
				}
				reader.close();

				// download the poster
				downloadPoster(posterURL, movieInfoFile);

			} catch (MalformedURLException e) {
				return "This is not a valid URL.";
			} catch (IOException e) {
				System.out.println("IOException found in the \"get-movie-poster\" command.");
				e.printStackTrace();
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
		int crr = 0;
		for (int i = 1; i < command.length; ++i) {
			if (command[i].equals(field)) {
				crr = i;
				break;
			}
		}
		return crr;
	}

	// remove commas for get-movie command and return string to create a unique path
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

		BufferedReader br = new BufferedReader(new FileReader(entry.toString()));
		String inputLine;
		int countGenres = 0, countActors = 0;

		while ((inputLine = br.readLine()) != null) {

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
		br.close();
	}

	public boolean orderingMovies(Map<Double, String> movies, String[] command, int order, BufferedWriter writer)
			throws IOException {

		Map<Double, String> sortedMovies;

		if (command[order + 1].equals("asc")) {
			sortedMovies = new TreeMap<>(movies);
		} else if (command[order + 1].equals("desc")) {
			sortedMovies = new TreeMap<>(Collections.reverseOrder());
			for (Map.Entry<Double, String> entry : movies.entrySet()) {
				sortedMovies.put(entry.getKey(), entry.getValue());
			}
		} else {
			return false;
		}
		for (Double key : sortedMovies.keySet()) {
			writer.write(sortedMovies.get(key));
			writer.newLine();
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

		InputStream is = poster.openStream();
		OutputStream os = new FileOutputStream(movieInfoFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	}
}
