package com.gathering.user.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsersViewController {

	@GetMapping("/login")
	public String login() {
		return "/user/login";
	}

	@GetMapping("/signup")
	public String signup() {
		return "/user/signup";
	}

	@GetMapping("/my-info")
	public String myInfo() {
		return "/user/my-info";
	}

}
