package com.gathering.file.infra;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile({"default", "test"})
@RequiredArgsConstructor
public class FileUploadWebMvcConfig implements WebMvcConfigurer {

	private final FileUploadConfig config;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler(config.getUrlPrefix() + "/**")
			.addResourceLocations("file:" + config.getLocalUploadPath() + "/");
	}
}
