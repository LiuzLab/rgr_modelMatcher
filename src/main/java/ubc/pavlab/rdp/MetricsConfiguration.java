package ubc.pavlab.rdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;


@Configuration
@Profile("beta")
public class MetricsConfiguration {

    @Value("${cloudwatch.namespace}")
    private String cloudwatchNamespace;

    @Value("${cloudwatch.access_key}")
    private String ACCESS_KEY;

    @Value("${cloudwatch.secret_key}")
    private String SECRET_KEY;

    @Value("${cloudwatch.region}")
    private String CLOUDWATCH_REGION;

    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry() {
        CloudWatchConfig cloudWatchConfig = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use default properties or override them here
            }

            @Override
            public String namespace() {
                return cloudwatchNamespace;
            }
        };

        CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder()
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .credentialsProvider(
//                        EnvironmentVariableCredentialsProvider.create()
//                )
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .region(Region.of(CLOUDWATCH_REGION))
                .build();

        return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchClient);
    }
}
