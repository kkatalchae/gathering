package com.gathering.user.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gathering.user.application.UserService;
import com.gathering.user.domain.model.UsersEntity;
import com.gathering.user.presentation.dto.UserInfoResponse;
import com.gathering.user.presentation.dto.UserJoinRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UsersController {

	private final UserService userService;

	@PostMapping("/join")
	public ResponseEntity<Void> join(@Valid @RequestBody UserJoinRequest request) {

		userService.join(request);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{tsid}")
	public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable String tsid) {
		UsersEntity user = userService.getUserInfo(tsid);
		UserInfoResponse response = UserInfoResponse.from(user);
		return ResponseEntity.ok(response);
	}

}
