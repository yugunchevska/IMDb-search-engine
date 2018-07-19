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

public class Extraction {

	private final String MAIN_PATH = "D:" + File.separator + "”ÌË" + File.separator + "Eclipse new" + File.separator
			+ "Project vol.2";
	private final String ERROR_MOVIE = "{\"Response\":\"False\",\"Error\":\"Movie not found!\"}";
	private final String ERROR_SEASON = "{\"Response\":\"False\",\"Error\":\"Series or season not found!\"}";

	public String getMovie(String[] result) throws IOException {

		String fields = "--fields=";
		// take the title of the movie
		String title = getTitle(result, fields);
		String titlePath = replaceSpecialCharacters(title);

		// take the index of --fields=
		int crr = getIndex(result, fields);

		// creating a path to the movie file
		String path;

		if (crr == 0) {
			path = MAIN_PATH + File.separator + titlePath + ".txt";
		} else {

			String substring = removeCommas(crr, result);
			path = MAIN_PATH + File.separator + titlePath + " " + substring + ".txt";
		}
		File myFile = new File(path);
		// if the file does not exists, create a new one and fill it with information
		// from the omdb api
		if (!myFile.isFile()) {
			try {
				myFile.createNewFile();
				URL myURL = createURL(title);

				BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
				BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
				String inputLine;

				while ((inputLine = in.readLine()) != null) {

					// check if the movie title is incorrect
					if (inputLine.equals(ERROR_MOVIE)) {
						// delete the .txt file and return message to the client
						bw.close();
						in.close();
						myFile.delete();
						return "Movie not found.";
					}

					if (crr == 0) {
						bw.write(inputLine);
					} else {
						// find the wanted fields and then find the fields' content
						findFields(inputLine, result, crr, bw);
					}
				}
				in.close();
				bw.close();
			} catch (MalformedURLException e) {
				return "This is not a valid URL.";
			} catch (IOException e) {
				System.out.println("IOException found in the \"get-movie\" command.");
				e.printStackTrace();
			}
		}

		return path;
	}

	public String getTVShow(String[] result) {

		String season = "--season=";
		// take the title
		String title = getTitle(result, season);
		String titlePath = replaceSpecialCharacters(title);

		// take the index of --season=
		int crr = getIndex(result, season);

		String path;

		if (crr == 0) {
			path = MAIN_PATH + File.separator + titlePath + ".txt";
		} else {
			path = MAIN_PATH + File.separator + titlePath + " season " + result[crr + 1] + ".txt";
		}

		File myFile = new File(path);

		if (!myFile.isFile()) {

			try {
				myFile.createNewFile();
				URL myURL;
				if (crr == 0) {
					myURL = createURL(title);

				} else {
					String url = "http://www.omdbapi.com/";
					String charset = "UTF-8";
					String query = String.format("t=%s&Season=%s&apikey=6c5a486c", URLEncoder.encode(title, charset),
							URLEncoder.encode(result[crr + 1], charset));
					myURL = new URL(url + "?" + query);
				}

				BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));

				BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					// check if the series or the season are correct
					if (crr == 0) {
						if (inputLine.equals(ERROR_MOVIE)) {
							// delete the .txt file and return message to the client
							bw.close();
							in.close();
							myFile.delete();
							return "Series not found.";
						}
						// if everything is correct, take it from the api
						bw.write(inputLine);
					} else {

						if (inputLine.equals(ERROR_SEASON)) {
							// delete the .txt file and return message to the client
							bw.close();
							in.close();
							myFile.delete();
							return "Series or season not found.";
						}

						// take the titles of the episodes
						titlePerEpisode(inputLine, bw);
					}
				}
				in.close();
				bw.close();

			} catch (MalformedURLException e) {
				return "This is not a valid URL.";

			} catch (IOException e) {
				System.out.println("IOException found in the \"get-tv-series\" command.");
				e.printStackTrace();
			}

		}
		return path;
	}

	public String getMovies(String[] result) {

		int genres = 0, actors = 0, order = 0;
		String actorsField = "--actors=", orderField = "--order=", genresField = "--genres=";

		for (int i = 1; i < result.length; ++i) {
			if (result[i].equals(actorsField)) {
				actors = i;
				continue;
			}
			if (result[i].equals(orderField)) {
				order = i;
				continue;
			}
			if (result[i].equals(genresField)) {
				genres = i;
				continue;
			}
		}

		// remove "," from the command
		if (genres != 0) {
			result[genres + 1] = result[genres + 1].replace(",", "");
		}
		result[actors + 2] = result[actors + 2].replace(",", "");

		String path = MAIN_PATH + File.separator + "Movies with " + result[actors + 1] + " " + result[actors + 3]
				+ ".txt";
		// create new file using the name of the actors, because they are always include
		File myFile = new File(path);
		try {
			myFile.createNewFile();

		} catch (IOException e1) {
			System.out.println("Creating new file failed.");
			e1.printStackTrace();
		}

		// getting all files that ends with .txt
		Path dir = Paths.get(MAIN_PATH);
		double imdbRating = 0;
		Map<Double, String> movies = new HashMap<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {

			BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
			// finding the movies with that actors/genres and putting them in the hashMap
			for (Path entry : stream) {

				filtringMovies(entry, genres, actors, result, movies, imdbRating);
			}

			if (movies.isEmpty()) {
				bw.close();
				return "There are none movies with these actors and gernes.";
			}

			// ordering the movies by imdbRating if the client want it
			if (order != 0) {
				int crr = orderingMovies(movies, result, order, bw);
				if (crr > 0) {
					return "Wrong command on field \"order\".";
				}
			} else {

				for (Map.Entry<Double, String> entry : movies.entrySet()) {
					bw.write(entry.getValue());
					bw.newLine();
				}
			}

			bw.close();
		} catch (IOException e) {
			System.out.println("IOException found in the \"get-movies\" command.");
			e.printStackTrace();
		} catch (DirectoryIteratorException ex) {
			System.out.println("DirectoryIteratorException found in the \"get-movies\" command.");
			ex.printStackTrace();
		}

		return path;
	}

	public String getPoster(String[] result) {

		// take the title
		String title = getTitle(result, "");
		String titlePath = replaceSpecialCharacters(title);

		// create a path and a file
		String path = MAIN_PATH + File.separator + titlePath + ".jpg";
		File myFile = new File(path);

		if (!myFile.isFile()) {

			try {
				myFile.createNewFile();
				URL myURL = createURL(title);

				BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
				String inputLine;
				String posterURL = "";

				// searching for the Poster's URL
				while ((inputLine = in.readLine()) != null) {

					// if the title is not correct
					if (inputLine.equals(ERROR_MOVIE)) {
						// delete the .txt file and return message to the client
						in.close();
						myFile.delete();
						return "Movie not found.";
					}

					posterURL = findPosterURL(inputLine);
				}
				in.close();

				// download the poster
				downloadPoster(posterURL, myFile);

			} catch (MalformedURLException e) {
				return "This is not a valid URL.";
			} catch (IOException e) {
				System.out.println("IOException found in the \"get-movie-poster\" command.");
				e.printStackTrace();
			}
		}

		return myFile.getName();

	}

	public String getTitle(String[] result, String stopword) {
		String title = "";

		for (int i = 1; i < result.length; ++i) {

			if (result[i].equals(stopword)) {
				break;
			}
			title += result[i];
			title += " ";
		}

		return title;
	}

	public String replaceSpecialCharacters(String title) {
		return title.replaceAll(":|\\?|\\*|\"", "_");
	}

	public int getIndex(String[] result, String field) {
		int crr = 0;
		for (int i = 1; i < result.length; ++i) {
			if (result[i].equals(field)) {
				crr = i;
				break;
			}
		}
		return crr;
	}

	// remove commas for get-movie command and return string to create a unique path
	public String removeCommas(int crr, String[] result) {
		String str = "";
		int i = crr + 1;
		while (i != result.length) {
			result[i] = result[i].replaceAll(",", "");
			str += result[i];
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

	public void findFields(String inputLine, String[] result, int crr, BufferedWriter bw) throws IOException {

		inputLine = inputLine.replaceAll("\\{|\\}", "");
		String[] word = inputLine.split(":|\\,");

		for (int i = 0; i < word.length; ++i) {
			word[i] = word[i].replaceAll("\"", "");
			int j = crr + 1;
			while (j != result.length) {
				if (word[i].equals(result[j])) {

					bw.write(word[i] + " ");
					++i;
					word[i] = word[i].replaceFirst("\"", "");

					while (word[i].indexOf("\"") != word[i].length() - 1) {
						bw.write(word[i] + ",");
						++i;
					}
					word[i] = word[i].replaceAll("\"", "");
					bw.write(word[i]);
					bw.newLine();
				}
				++j;
			}
		}
	}

	public void titlePerEpisode(String inputLine, BufferedWriter bw) throws IOException {

		inputLine = inputLine.replaceAll("\\{|\\}|\\[", "");
		String[] word = inputLine.split(":|\\,");
		String TITLE = "Title";

		for (int i = 1; i < word.length; ++i) {
			word[i] = word[i].replaceAll("\"", "");

			if (word[i].equals(TITLE)) {

				++i;
				word[i] = word[i].replaceFirst("\"", "");

				while (word[i].indexOf("\"") != word[i].length() - 1) {
					bw.write(word[i] + ",");
					++i;
				}
				word[i] = word[i].replaceAll("\"", "");
				bw.write(word[i]);
				bw.newLine();
			}
		}
	}

	public void filtringMovies(Path entry, int genres, int actors, String[] result, Map<Double, String> movies,
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

					if (words[i].equals(result[genres + 1]) || words[i].equals(result[genres + 2])) {
						++countGenres;
					}
				}

				if (words[i].equals(result[actors + 1] + result[actors + 2])
						|| words[i].equals(result[actors + 3] + result[actors + 4])) {
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

	public int orderingMovies(Map<Double, String> movies, String[] result, int order, BufferedWriter bw)
			throws IOException {

		Map<Double, String> sortedMovies;
		int counter = 0;

		if (result[order + 1].equals("asc")) {
			sortedMovies = new TreeMap<>(movies);
		} else if (result[order + 1].equals("desc")) {
			sortedMovies = new TreeMap<>(Collections.reverseOrder());
			for (Map.Entry<Double, String> entry : movies.entrySet()) {
				sortedMovies.put(entry.getKey(), entry.getValue());
			}
		} else {
			++counter;
			return counter;
		}
		for (Double key : sortedMovies.keySet()) {
			bw.write(sortedMovies.get(key));
			bw.newLine();
		}

		return counter;
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

	public void downloadPoster(String posterURL, File myFile) throws MalformedURLException, IOException {
		URL poster = new URL(posterURL);

		InputStream is = poster.openStream();
		OutputStream os = new FileOutputStream(myFile);

		byte[] b = new byte[2048];
		int length;

		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();
	}
}
