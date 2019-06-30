package bg.uni.sofia.fmi.server.util;

import java.io.File;

public interface OMDbManagerConstants {

	static final String MOVIE_INFO_PATH = "Server" + File.separator + "MoviesInforamtion";
	static final String POSTER_PATH = "Server" + File.separator + "Posters";
	
	static final String FIELDS = "--fields=";
	static final String SEASON_FIELD = "--season=";
	static final String ACTORS_FIELD = "--actors="; 
	static final String ORDER_FIELD = "--order=";
	static final String GENRES_FIELD = "--genres=";
	
	static final String ERROR_MOVIE = "{\"Response\":\"False\",\"Error\":\"Movie not found!\"}";
	static final String ERROR_SEASON = "{\"Response\":\"False\",\"Error\":\"Series or season not found!\"}";
	
}
