package bg.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import bg.uni.sofia.fmi.server.OMDbManager;
import bg.uni.sofia.fmi.server.util.OMDbManagerConstants;

import java.io.File;
import java.io.IOException;

public class OMDbManagerTest {

	@Test
	public void testGetMovie() throws IOException {
		OMDbManager omdbManager = new OMDbManager();
		String[] result = "get-movie Titanic --fields= Year".split(" ");
		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Titanic  Year .txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);
		assertEquals("get-movie command doesn't work", expectedMovieInfoFile.getAbsolutePath(), omdbManager.getMovie(result));
	}

	@Test
	public void testGetTVSeries() throws IOException {
		OMDbManager omdbManager = new OMDbManager();
		String[] result = "get-tv-series Friends --season= 7".split(" ");
		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Friends  season 7.txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);
		assertEquals("get-tv-series command doesn't work", expectedMovieInfoFile.getAbsolutePath(), omdbManager.getTVSeries(result));
	}

	@Test
	public void testGetMovies() throws IOException {
		OMDbManager omdbManager = new OMDbManager();
		String[] result = "get-movies --order= desc --actors= Leonardo DiCaprio, Kate Winslet --genres= Drama, Romance"
				.split(" ");
		String relativeMovieInfoPath = OMDbManagerConstants.MOVIE_INFO_PATH + File.separator + "Movies with Leonardo Kate.txt";
		File expectedMovieInfoFile = new File(relativeMovieInfoPath);
		assertEquals("get-movies command doesn't work", expectedMovieInfoFile.getAbsolutePath(), omdbManager.getMovies(result));
	}

	@Test
	public void testGetPoster() throws IOException {
		OMDbManager omdbManager = new OMDbManager();
		String[] result = "get-movie-poster Oblivion".split(" ");
		String expectedMoviePosterFilename = "Oblivion .jpg";
		assertEquals("get-tv-poster command doesn't work", expectedMoviePosterFilename, omdbManager.getPoster(result));
	}

	@Test
	public void testWithIncorrectMovie() throws IOException {
		OMDbManager omdbManager = new OMDbManager();
		String[] result = "get-movie-poster Titani".split(" ");
		String errorMessage = "Movie not found.";
		assertEquals("get-tv-poster command doesn't work", errorMessage, omdbManager.getPoster(result));
	}

}

