package bg.uni.sofia.fmi.server.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class OMDbManagerUtil {
	
	public static String getTitle(String[] command, String stopword) {
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

	public static String replaceSpecialCharacters(String title) {
		return title.replaceAll(":|\\?|\\*|\"", "_");
	}
	
	public static String getMovieInfoPath(int fieldsIndex, String titleSpecification, String titleFilename) {		
		String movieInfoPath;
		if (fieldsIndex == 0) {
			movieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + titleFilename + ".txt";
		} else {
			movieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + titleFilename + titleSpecification + ".txt";
		}
		
		return movieInfoPath;
	}

	public static int getIndex(String[] command, String field) {
		int fieldIndex = 0;
		for (int i = 1; i < command.length; ++i) {
			if (command[i].equals(field)) {
				fieldIndex = i;
				break;
			}
		}
		return fieldIndex;
	}

	public static String removeCommas(int crr, String[] command) {
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

	public static URL createURL(String title) throws MalformedURLException, UnsupportedEncodingException {
		
		String query = String.format(OMDbManagerConstants.OMDB_API_KEY, URLEncoder.encode(title, "UTF-8"));
		return new URL(OMDbManagerConstants.OMDB_URL + "?" + query);
	}
	
	public static void findFields(String inputLine, String[] command, int crr, BufferedWriter writer) throws IOException {

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
	
	public static void titlePerEpisode(String inputLine, BufferedWriter writer) throws IOException {

		inputLine = inputLine.replaceAll("\\{|\\}|\\[", "");
		String[] word = inputLine.split(":|\\,");

		for (int i = 1; i < word.length; ++i) {
			word[i] = word[i].replaceAll("\"", "");

			if (word[i].equals(OMDbManagerConstants.TITLE)) {

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

	public static void filtringMovies(Path entry, int genresFieldIndex, int actorsFieldIndex, String[] command, Map<Double, String> movies,
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
	
					if (genresFieldIndex != 0) {
						if (words[i].equals(command[genresFieldIndex + 1]) || words[i].equals(command[genresFieldIndex + 2])) {
							++countGenres;
						}
					}
	
					if (words[i].equals(command[actorsFieldIndex + 1] + command[actorsFieldIndex + 2])
							|| words[i].equals(command[actorsFieldIndex + 3] + command[actorsFieldIndex + 4])) {
						++countActors;
					}
	
					if (words[i].equals("imdbRating")) {
						words[i + 1] = words[i + 1].replaceAll("\"", "");
						imdbRating = Double.parseDouble(words[i + 1].replace(",", "."));
					}
				}
	
				if (genresFieldIndex != 0 && countGenres == 2) {
					++counter;
				}
	
				if ((countActors == 2 && genresFieldIndex != 0 && counter != 0) || (countActors == 2 && genresFieldIndex == 0)) {
					movies.put(imdbRating, entry.getFileName().toString().replaceFirst("[.][^.]+$", ""));
				}
			}
		}
	}

	public static void orderingMovies(Map<Double, String> movies, String[] command, int orderIndex, File movieInfoFile)
			throws IOException {

		Map<Double, String> sortedMovies;

		if (command[orderIndex + 1].equals("asc")) {
			sortedMovies = new TreeMap<>(movies);
		} else {
			sortedMovies = new TreeMap<>(Collections.reverseOrder());
			for (Map.Entry<Double, String> entry : movies.entrySet()) {
				sortedMovies.put(entry.getKey(), entry.getValue());
			}
		} 
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile))) {
			for (Double key : sortedMovies.keySet()) {
				writer.write(sortedMovies.get(key));
				writer.newLine();
			}
		}
	}
	
	public static String findPosterURL(String inputLine) {

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

	public static void downloadPoster(String posterURL, File movieInfoFile) throws MalformedURLException, IOException {
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
