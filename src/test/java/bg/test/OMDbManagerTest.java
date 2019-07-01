package bg.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import bg.uni.sofia.fmi.server.OMDbManager;
import bg.uni.sofia.fmi.server.util.OMDbManagerConstants;

import java.io.File;
import java.io.IOException;

public class OMDbManagerTest {

	private OMDbManager omdbManager = new OMDbManager();

	@Test
	public void testGetMovie() throws IOException {
		String[] command = "get-movie Titanic --fields= Year".split(" ");

		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Titanic  Year .txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);

		assertEquals("get-movie command doesn't work", expectedMovieInfoFile.getAbsolutePath(),
				omdbManager.getMovie(command));
	}

	@Test
	public void testGetTVSeries() throws IOException {
		String[] command = "get-tv-series Friends --season= 7".split(" ");

		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Friends  season 7.txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);

		assertEquals("get-tv-series command doesn't work", expectedMovieInfoFile.getAbsolutePath(),
				omdbManager.getTVSeries(command));
	}

	@Test
	public void testGetMovies() throws IOException {
		String[] command = "get-movies --order= desc --actors= Leonardo DiCaprio, Kate Winslet --genres= Drama, Romance"
				.split(" ");

		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator
				+ "Movies with Leonardo Kate.txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);

		assertEquals("get-movies command doesn't work", expectedMovieInfoFile.getAbsolutePath(),
				omdbManager.getMovies(command));
	}

	@Test
	public void testGetPoster() throws IOException {
		String[] command = "get-movie-poster Oblivion".split(" ");

		String expectedMoviePosterFilename = "Oblivion .jpg";

		assertEquals("get-tv-poster command doesn't work", expectedMoviePosterFilename, omdbManager.getPoster(command));
	}

	@Test
	public void testWithIncorrectMovie() throws IOException {
		String[] command = "get-movie-poster Titani".split(" ");

		String errorMessage = "Movie not found.";

		assertEquals("get-tv-poster command doesn't work", errorMessage, omdbManager.getPoster(command));
	}

}
