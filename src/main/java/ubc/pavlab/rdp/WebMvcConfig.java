package ubc.pavlab.rdp;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import ubc.pavlab.rdp.util.OntologyMessageSource;

/**
 * Created by mjacobson on 16/01/18.
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MessageSource messageSource( OntologyMessageSource ontologyMessageSource ) {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // if it cannot be resolved in messages.properties, then lookup some of our built-in resolution for
        // ontology-related patterns
        messageSource.setParentMessageSource( ontologyMessageSource );
        // application-prod.properties and login.properties is there for backward compatibility since
        // we used to pull locale strings from there.
        messageSource.setBasenames( "file:messages", "file:application-prod", "file:login", "classpath:messages" );
        return messageSource;
    }

}