package com.cs.muic.backend.SimpleObject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author scheaman
 * @date 2/9/18
 */
@Configuration
public class ContentNegotiationConfiguration implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
        // Turn off suffix-based content negotiation
        configurer.favorPathExtension(false);
    }
}
