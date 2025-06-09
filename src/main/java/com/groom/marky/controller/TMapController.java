package com.groom.marky.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.marky.domain.response.TMapCongestionResponse;
import com.groom.marky.service.TMapService;

@RestController
@RequestMapping("/tmap/congestions")
public class TMapController {

	private final TMapService tMapService;

	@Autowired
	public TMapController(TMapService tMapService) {
		this.tMapService = tMapService;
	}

	@GetMapping
	public ResponseEntity<?> getCongestionByLocation(
		@RequestParam double lat,
		@RequestParam double lng) {

		TMapCongestionResponse response = tMapService.getCongestion(lat, lng);
		return ResponseEntity.ok(response);
	}
}
