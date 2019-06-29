package bg.test;

import static org.junit.Assert.*;

import org.junit.Test;

import bg.uni.sofia.fmi.server.OMDbManager;

import java.io.File;
import java.io.IOException;

public class ExtractionTest {

	@Test
	public void getMovieTest() throws IOException {
		OMDbManager extr = new OMDbManager();
		String[] result = "get-movie Titanic --fields= Year".split(" ");
		String expect = "D:" + File.separator + "”ÌË" + File.separator + "Eclipse new" + File.separator + "Project vol.2"
				+ File.separator + "Titanic  Year .txt";
		assertEquals("get-movie command doesn't work", expect, extr.getMovie(result));
	}

	@Test
	public void getTvSeriesTest() {
		OMDbManager extr = new OMDbManager();
		String[] result = "get-tv-series Friends --season= 7".split(" ");
		String expect = "D:" + File.separator + "”ÌË" + File.separator + "Eclipse new" + File.separator + "Project vol.2"
				+ File.separator + "Friends  season 7.txt";
		assertEquals("get-tv-series command doesn't work", expect, extr.getTVSeries(result));
	}

	@Test
	public void getMoviesTest() {
		OMDbManager extr = new OMDbManager();
		String[] result = "get-movies --order= desc --actors= Leonardo DiCaprio, Kate Winslet --genres= Drama, Romance"
				.split(" ");
		String expect = "D:" + File.separator + "”ÌË" + File.separator + "Eclipse new" + File.separator + "Project vol.2"
				+ File.separator + "Movies with Leonardo Kate.txt";
		assertEquals("get-movies command doesn't work", expect, extr.getMovies(result));
	}

	@Test
	public void getPosterTest() {
		OMDbManager extr = new OMDbManager();
		String[] result = "get-movie-poster Oblivion".split(" ");
		String expect = "Oblivion .jpg";
		assertEquals("get-tv-poster command doesn't work", expect, extr.getPoster(result));
	}

	@Test
	public void isIncorrectMovie() {
		OMDbManager extr = new OMDbManager();
		String[] result = "get-movie-poster Titani".split(" ");
		String expect = "Movie not found.";
		assertEquals("get-tv-poster command doesn't work", expect, extr.getPoster(result));
	}

	@Test
	public void findPosterURL() {
		OMDbManager extr = new OMDbManager();
		String inputLine = "\"Language\":\"English, French\",\"Country\":\"USA\",\"Awards\":\"4 wins & 9 nominations.\",\"Poster\":\"https://images-na.ssl-images-amazon.com/images/M/MV5BZTk2ZmUwYmEtNTcwZS00YmMyLWFkYjMtNTRmZDA3YWExMjc2XkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_SX300.jpg\"";
		String expect = "https://images-na.ssl-images-amazon.com/images/M/MV5BZTk2ZmUwYmEtNTcwZS00YmMyLWFkYjMtNTRmZDA3YWExMjc2XkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_SX300.jpg";
		assertEquals("can't find poster URL", expect, extr.findPosterURL(inputLine));
	}

}

