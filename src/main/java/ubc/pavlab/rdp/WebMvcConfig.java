package ubc.pavlab.rdp;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.context.annotation.Profile;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//import org.springframework.web.accept.ContentNegotiationConfigurer;
//org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mjacobson on 16/01/18.
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

//public class WebMvcConfig{


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // application-prod.properties and login.properties is there for backward compatibility since
        // we used to pull locale strings from there.
        messageSource.setBasenames( "file:messages", "file:application-prod", "file:login", "classpath:messages" );
        return messageSource;
    }

//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        for (HttpMessageConverter<?> converter : converters) {
//            if (converter instanceof MappingJackson2HttpMessageConverter) {
//                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
//
//                // Set the default charset to UTF-8
//                jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
//
//                // Include media types with and without charset
//                List<MediaType> supportedMediaTypes = new ArrayList<>();
//                supportedMediaTypes.add(MediaType.APPLICATION_JSON); // application/json
//                supportedMediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8)); // application/json;charset=UTF-8
//                jsonConverter.setSupportedMediaTypes(supportedMediaTypes);
//            }
//        }
//    }

//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        System.out.println("Registered HttpMessageConverters:");
//        for (HttpMessageConverter<?> converter : converters) {
//            System.out.println("Converter: " + converter.getClass().getSimpleName());
//            if (converter instanceof AbstractHttpMessageConverter) {
//                List<MediaType> supportedMediaTypes = ((AbstractHttpMessageConverter<?>) converter).getSupportedMediaTypes();
//                System.out.println("Supported Media Types: " + supportedMediaTypes);
//            }
//        }
//    }
//
//    @Override
//    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
//        configurer.favorPathExtension(false)
//                .favorParameter(false)
//                .ignoreAcceptHeader(false)
//                .defaultContentType(MediaType.APPLICATION_JSON)
//                .mediaType("json", MediaType.APPLICATION_JSON);
//    }

//
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
////        converters.add(mappingJackson2HttpMessageConverter());
//
//
//        // Add this line to preserve default converters
//        super.configureMessageConverters(converters);
//
//        // Now add your custom converter
//        converters.add(mappingJackson2HttpMessageConverter());
//    }
//
//    // Same method from earlier
//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        converter.setDefaultCharset(StandardCharsets.UTF_8);
//        converter.setSupportedMediaTypes(Arrays.asList(
//                new MediaType("application", "json", StandardCharsets.UTF_8),
//                MediaType.APPLICATION_JSON // application/json without charset
//        ));
//        return converter;
//    }



}