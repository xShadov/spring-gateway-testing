package com.shadov.test.serviceb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b")
public class ControllerB {
	@GetMapping
	public String get() {
		System.out.println("called");
		return "B";
	}
}
