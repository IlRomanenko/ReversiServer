package mipt.algo.reversi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * ReversiServer
 * mipt.algo.reversi.server
 * <p>
 * Created by ilya on 03.05.17.
 */
@SpringBootApplication
public class Main extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Main.class);
        application.run(args);
    }
}
