# IMDb-search-engine

Client-server application that provides functionality for searching in IMDb using the OMDb API. There are four commands:

- get-movie <movie_name> --fields=[field_1,field_2] - print the description of the movie or a specific field from the description
- get-movies --order=[asc|desc] --genres=[genre_1, genre_2] --actors=[actor_1, actor_2] - filter movies on the genres or actors in specific order
- get-tv-series --season= - print the name of the episodes of the season
- get-movie-poster - download the movie poster on the client computer
