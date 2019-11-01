package com.shadov.test.servicea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/a")
public class ControllerA {
	@Autowired
	private ServiceAApplication.NameService nameService;

	@GetMapping
	public String get() {
		final String first = nameService.getName();
		final String second = nameService.getName();
		return first+","+second;
	}

	@PostMapping
	public String post() {
		return "A";
	}
}
