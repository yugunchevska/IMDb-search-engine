package bg.uni.sofia.fmi.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import bg.uni.sofia.fmi.server.util.OMDbManagerConstants;
import bg.uni.sofia.fmi.server.util.OMDbManagerUtil;

public class OMDbManager {

	public OMDbManager() {
		
		File movieInfoFolder = new File(OMDbManagerConstants.MOVIE_INFO_PATH);
		movieInfoFolder.mkdirs();
		
		File posterFolder = new File(OMDbManagerConstants.POSTER_PATH);
		posterFolder.mkdirs();
	}

	public String getMovie(String[] command) throws IOException {
		
		String title = OMDbManagerUtil.getTitle(command, OMDbManagerConstants.FIELDS);
		String titleFilename = OMDbManagerUtil.replaceSpecialCharacters(title);
		
		int fieldsIndex = OMDbManagerUtil.getIndex(command, OMDbManagerConstants.FIELDS);

		String titleSpecification = " " + OMDbManagerUtil.removeCommas(fieldsIndex, command);
		String movieInfoPath = OMDbManagerUtil.getMovieInfoPath(fieldsIndex, titleSpecification, titleFilename);

		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {
			movieInfoFile.createNewFile();

			URL omdbURL = OMDbManagerUtil.createURL(title);
			
			if (!readFromOMDbAPI(fieldsIndex, command, omdbURL, movieInfoFile)) {
				return "Movie not found.";
			}
		}

		return movieInfoPath;
	}

	public String getTVSeries(String[] command) throws IOException {
		
		String title = OMDbManagerUtil.getTitle(command, OMDbManagerConstants.SEASON_FIELD);
		String titleFilename = OMDbManagerUtil.replaceSpecialCharacters(title);
		
		int seasonIndex = OMDbManagerUtil.getIndex(command, OMDbManagerConstants.SEASON_FIELD);

		String titleSpecification = " season " + command[seasonIndex + 1];
		String movieInfoPath = OMDbManagerUtil.getMovieInfoPath(seasonIndex, titleSpecification, titleFilename);

		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {
			movieInfoFile.createNewFile();
			URL omdbURL;

			if (seasonIndex == 0) {
				omdbURL = OMDbManagerUtil.createURL(title);
			} else {
				String url = "http://www.omdbapi.com/";
				String charset = "UTF-8";
				String query = String.format("t=%s&Season=%s&apikey=6c5a486c", URLEncoder.encode(title, charset),
						URLEncoder.encode(command[seasonIndex + 1], charset));
				omdbURL = new URL(url + "?" + query);
			}

			if (!readFromOMDbAPI(seasonIndex, command, omdbURL, movieInfoFile)) {
				return "Series or season not found!";
			}
		}

		return movieInfoPath;
	}

	public String getMovies(String[] command) throws IOException {

		int genresFieldIndex = OMDbManagerUtil.getIndex(command, OMDbManagerConstants.GENRES_FIELD);
		int actorsFieldIndex = OMDbManagerUtil.getIndex(command, OMDbManagerConstants.ACTORS_FIELD);
		int orderFieldIndex = OMDbManagerUtil.getIndex(command, OMDbManagerConstants.ORDER_FIELD);

		if (orderFieldIndex != 0 && !command[orderFieldIndex + 1].equals("asc")
				&& !command[orderFieldIndex + 1].equals("desc")) {
			return "Wrong command on field \"order\".";
		}

		if (genresFieldIndex != 0) {
			command[genresFieldIndex + 1] = command[genresFieldIndex + 1].replace(",", "");
		}
		command[actorsFieldIndex + 2] = command[actorsFieldIndex + 2].replace(",", "");

		String movieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Movies with "
				+ command[actorsFieldIndex + 1] + " " + command[actorsFieldIndex + 3] + ".txt";

		File movieInfoFile = new File(movieInfoPath);
		movieInfoFile.createNewFile();

		if (!getMoviesByActors(genresFieldIndex, actorsFieldIndex, orderFieldIndex, command, movieInfoFile)) {
			return "There aren't movies with these actors.";
		}

		return movieInfoPath;
	}

	public String getPoster(String[] command) throws IOException {
		
		String title = OMDbManagerUtil.getTitle(command, "");
		String titlePath = OMDbManagerUtil.replaceSpecialCharacters(title);

		String movieInfoPath = OMDbManagerConstants.POSTER_PATH + File.separator + titlePath + ".jpg";
		File movieInfoFile = new File(movieInfoPath);
		if (!movieInfoFile.isFile()) {
			movieInfoFile.createNewFile();
			
			URL omdbURL = OMDbManagerUtil.createURL(title);

			if (!savePoster(movieInfoFile, omdbURL)) {
				return "Movie not found.";
			}
		}

		return movieInfoFile.getName();
	}

	private boolean readFromOMDbAPI(int fieldsIndex, String[] command, URL omdbURL, File movieInfoFile)
			throws IOException {

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()));
				BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile));) {
			
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				if (inputLine.equals(OMDbManagerConstants.ERROR_MOVIE)) {
					movieInfoFile.delete();
					return false;

				}

				if (fieldsIndex == 0) {
					writer.write(inputLine);
				} else {
					if (command[0].equals("")) {
						OMDbManagerUtil.findFields(inputLine, command, fieldsIndex, writer);
					} else {
						if (inputLine.equals(OMDbManagerConstants.ERROR_SEASON)) {
							movieInfoFile.delete();
							return false;
						}

						OMDbManagerUtil.titlePerEpisode(inputLine, writer);
					}
				}
			}

			return true;
		} catch (IOException e) {
			System.out.println("Couldn't take information from OMDb API. Reason: " + e.getMessage());
			throw new IOException(e);
		}

	}

	private boolean getMoviesByActors(int genresFieldIndex, int actorsFieldIndex, int orderFieldIndex, String[] command,
			File movieInfoFile) throws IOException {
		
		double imdbRating = 0;
		Map<Double, String> movies = new HashMap<>();
		Path movieInfoDir = Paths.get(OMDbManagerConstants.MOVIE_INFO_PATH);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(movieInfoDir, "*.txt");
				BufferedWriter writer = new BufferedWriter(new FileWriter(movieInfoFile))) {

			for (Path entry : stream) {
				OMDbManagerUtil.filtringMovies(entry, genresFieldIndex, actorsFieldIndex, command, movies, imdbRating);
			}

			if (movies.isEmpty()) {
				return false;
			}

			if (orderFieldIndex != 0) {
				OMDbManagerUtil.orderingMovies(movies, command, orderFieldIndex, movieInfoFile);
			} else {
				for (Map.Entry<Double, String> entry : movies.entrySet()) {
					writer.write(entry.getValue());
					writer.newLine();
				}
			}

			return true;
		} catch (IOException e) {
			System.out.println("Couldn't get information from the server. Reason: " + e.getMessage());
			throw new IOException(e);
		}
	}

	private boolean savePoster(File movieInfoFile, URL omdbURL) throws IOException {
		
		String inputLine;
		String posterURL = "";
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(omdbURL.openStream()))) {

			while ((inputLine = reader.readLine()) != null) {

				if (inputLine.equals(OMDbManagerConstants.ERROR_MOVIE)) {
					movieInfoFile.delete();
					return false;

				}
				posterURL = OMDbManagerUtil.findPosterURL(inputLine);
			}

			OMDbManagerUtil.downloadPoster(posterURL, movieInfoFile);

			return true;
		} catch (IOException e) {
			System.out.println("Couldn't download the movie's poster. Reason: " + e.getMessage());
			throw new IOException(e);
		}
	}

}
