package mipt.algo.reversi.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * ReversiServer
 * mipt.algo.reversi.server
 * <p>
 * Created by ilya on 04.05.17.
 */
@Configuration
@PropertySource(value = {"file:${config.path}/app.properties"}, ignoreResourceNotFound = true)
public class PropertiesWithJavaConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}