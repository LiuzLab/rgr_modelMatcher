package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import io.micrometer.core.instrument.MeterRegistry;
import ubc.pavlab.rdp.util.MetricsUpdater;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@CommonsLog
public class Application {

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }

//    @Bean
//    public MetricsUpdater metricsUpdater(MeterRegistry registry) {
//        return new MetricsUpdater(registry);
//    }

}
