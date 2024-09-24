package ubc.pavlab.rdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("beta")
public class BetaCorsConfig extends WebMvcConfigurerAdapter {
//    @Bean
//    public WebMvcConfigurer betaCorsConfigurer() {
//        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(PathMatchConfigurer pathMatchConfigurer) {

            }

//            @Override
//            public void configureContentNegotiation(ContentNegotiationConfigurer contentNegotiationConfigurer) {
//
//            }

            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer asyncSupportConfigurer) {

            }

            @Override
            public void configureDefaultServletHandling(DefaultServletHandlerConfigurer defaultServletHandlerConfigurer) {

            }

            @Override
            public void addFormatters(FormatterRegistry formatterRegistry) {

            }

            @Override
            public void addInterceptors(InterceptorRegistry interceptorRegistry) {

            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {

            }

            @Override
            public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
//                configurer.favorPathExtension(false)
//                        .favorParameter(false)
//                        .ignoreAcceptHeader(false)
//                        .defaultContentType(MediaType.APPLICATION_JSON)
//                        .mediaType("json", MediaType.APPLICATION_JSON);
            }

            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//                for (HttpMessageConverter<?> converter : converters) {
//                    if (converter instanceof MappingJackson2HttpMessageConverter) {
//                        MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
//
//                        // Set the default charset to UTF-8
//                        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
//
//                        // Include media types with and without charset
//                        List<MediaType> supportedMediaTypes = new ArrayList<>();
//                        supportedMediaTypes.add(MediaType.APPLICATION_JSON); // application/json
//                        supportedMediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8)); // application/json;charset=UTF-8
//                        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);
//                    }
//                }
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("https://localhost:8080",
                                "http://localhost:8080", "https://localhost:8081", "http://localhost:8081",
                                "http://localhost:4200", "http://localhost:3000", "http://http://18.225.55.166:4200")  // Adjust as per your beta environment
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

            }

            @Override
            public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {

            }

            @Override
            public void configureViewResolvers(ViewResolverRegistry viewResolverRegistry) {

            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> list) {

            }

            @Override
            public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> list) {

            }

//            @Override
//            public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
//                configurer.favorPathExtension(false)
//                        .favorParameter(false) // Consider if you do not want content negotiation via URL parameters
//                        .ignoreAcceptHeader(false)
//                        .defaultContentType(MediaType.APPLICATION_JSON)
//                        .mediaType("json", MediaType.APPLICATION_JSON);
//            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                // No custom message converters; this can remain empty
            }

            @Override
            public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> list) {

            }

            @Override
            public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> list) {

            }

            @Override
            public Validator getValidator() {
                return null;
            }

            @Override
            public MessageCodesResolver getMessageCodesResolver() {
                return null;
            }
        };
//    }
//}

