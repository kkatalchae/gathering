package com.gathering.user.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.user.application.service.UsersService;
import com.gathering.user.presentation.dto.UserJoinRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UsersController {

	private final UsersService usersService;

	@RequestMapping("/join")
	public ResponseEntity<Void> join(@Valid @RequestBody UserJoinRequest request) {

		usersService.join(request);

		return ResponseEntity.noContent().build();
	}

}
