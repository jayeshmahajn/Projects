package com.example.jayesh.watchlist.service;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class RatingService {

	String apiUrl = "https://omdbapi.com/?apikey=a8431cee&t=";
	
	@SuppressWarnings("null")
	public String getMovieRating(String title) {
	
	try {
		
		RestTemplate tamplate = new RestTemplate();
		
		ResponseEntity<ObjectNode> response = tamplate.getForEntity(apiUrl + title, ObjectNode.class);
		
		
		ObjectNode jsonObject = response.getBody();
		
		return jsonObject.path("imdbRating").asText();
		
	}catch(Exception e) {
		
		System.out.println("Etheir movie name not available or api is down"+ e.getMessage());
		
		return null;
	}
	
	
	}
}
