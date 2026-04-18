package com.gathering.gathering.presentation.controller.view;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 모임 화면 View Controller
 * REST API(@RestController)와 동일한 URL을 Content Negotiation으로 공유
 * 브라우저 요청(Accept: text/html) → 이 컨트롤러
 * API 요청(Accept: application/json) → GatheringsController
 */
@Controller
@RequestMapping("/gatherings")
public class GatheringsViewController {

	// 모임 목록 화면
	@GetMapping(produces = MediaType.TEXT_HTML_VALUE)
	public String list() {
		return "gathering/list";
	}

	// 모임 생성 폼 — /{tsid} 패턴보다 먼저 매칭되도록 리터럴 경로로 선언
	@GetMapping(value = "/new", produces = MediaType.TEXT_HTML_VALUE)
	public String create() {
		return "gathering/create";
	}

	// 모임 상세 화면
	@GetMapping(value = "/{tsid}", produces = MediaType.TEXT_HTML_VALUE)
	public String detail(@PathVariable String tsid) {
		return "gathering/detail";
	}

	// 모임 수정 폼
	@GetMapping(value = "/{tsid}/edit", produces = MediaType.TEXT_HTML_VALUE)
	public String edit(@PathVariable String tsid) {
		return "gathering/edit";
	}
}
