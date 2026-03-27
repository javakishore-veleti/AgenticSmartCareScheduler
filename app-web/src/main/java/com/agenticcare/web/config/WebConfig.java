package com.agenticcare.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward portal root paths to index.html
        registry.addViewController("/portal/customer/").setViewName("forward:/portal/customer/index.html");
        registry.addViewController("/portal/customer").setViewName("forward:/portal/customer/index.html");
        registry.addViewController("/portal/admin/").setViewName("forward:/portal/admin/index.html");
        registry.addViewController("/portal/admin").setViewName("forward:/portal/admin/index.html");

        // Forward Angular SPA routes (deep links, any depth) to index.html
        registry.addViewController("/portal/customer/{path:[^\\.]*}").setViewName("forward:/portal/customer/index.html");
        registry.addViewController("/portal/customer/{path:[^\\.]*}/{sub:[^\\.]*}").setViewName("forward:/portal/customer/index.html");
        registry.addViewController("/portal/customer/{path:[^\\.]*}/{sub:[^\\.]*}/{rest:[^\\.]*}").setViewName("forward:/portal/customer/index.html");
        registry.addViewController("/portal/admin/{path:[^\\.]*}").setViewName("forward:/portal/admin/index.html");
        registry.addViewController("/portal/admin/{path:[^\\.]*}/{sub:[^\\.]*}").setViewName("forward:/portal/admin/index.html");
        registry.addViewController("/portal/admin/{path:[^\\.]*}/{sub:[^\\.]*}/{rest:[^\\.]*}").setViewName("forward:/portal/admin/index.html");

        // Root redirect to customer portal
        registry.addViewController("/").setViewName("forward:/portal/customer/index.html");
    }
}
