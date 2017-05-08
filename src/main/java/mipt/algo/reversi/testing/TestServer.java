package mipt.algo.reversi.testing;

import mipt.algo.reversi.database.SolutionStore;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ReversiServer
 * mipt.algo.reversi.server
 * <p>
 * Created by ilya on 03.05.17.
 */
@Component
public class TestServer {

    private Logger log = LoggerFactory.getLogger(TestServer.class);

    @Autowired
    private SolutionStore solutionStore;

    private ExecutorService executorPool;

    @Value("${mipt.algo.reversi.testing.home}")
    private String HOME_DIRECTORY;

    @Value("${mipt.algo.reversi.testing.builddir}")
    private String BUILD_DIR;


    @Value("${mipt.algo.reversi.testing.threads}")
    private Integer threads;

    private static final Random random = new Random();

    private Queue<Integer> portPool;

    @PostConstruct
    public void postConstruct() {
        executorPool = Executors.newFixedThreadPool(threads);
        portPool = new LinkedList<>();
        for (int i = 4242; i < 5000; i++) {
            portPool.add(i);
        }
    }

    public void startTesting(Integer firstId, Integer secondId) {

        executorPool.submit(() -> testingTask(firstId, secondId));
    }

    private String generateToken() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            stringBuilder.append(random.nextInt());
        }
        return stringBuilder.toString();
    }

    private void testingTask(Integer firstId, Integer secondId) {
        Integer port = portPool.poll();
        String token = generateToken();


        String logFile = HOME_DIRECTORY + "/logs/" + firstId + "vs" + secondId +
                "t_" + Calendar.getInstance().getTime().getTime();

        GameServer server = new GameServer(port, token, solutionStore, logFile);
        String firstExecPath = solutionStore.getSolutionExecPath(firstId);
        String secondExecPath = solutionStore.getSolutionExecPath(secondId);

        server.createListeningServer();
        Process first = null;
        Process second = null;
        final int MAGIC_CONST = 1000000000;
        try {
            String params = "127.0.0.1" + " " + port.toString() + " " + token;
            first = Runtime.getRuntime().exec(firstExecPath + " " + params + " " + firstId);
            second = Runtime.getRuntime().exec(secondExecPath + " " + params + " " + (secondId + MAGIC_CONST));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        server.testGame(firstId, secondId + MAGIC_CONST, true);
        if (first != null) {
            first.destroyForcibly();
        }
        if (second != null) {
            second.destroyForcibly();
        }
        server.close();
        portPool.add(port);
    }

    public void compileSolution(Integer userId, String solutionPath, String solutionLanguage) {
        String solutionName = solutionLanguage + "_" +  Calendar.getInstance().getTime().getTime() + "_" + userId;
        String fullPath = HOME_DIRECTORY + File.separator + userId.toString() + File.separator;
        switch (solutionLanguage) {
            case "cpp":
                try {
                    String execPath = "/bin/bash " + BUILD_DIR + "/make.sh " + solutionPath + " " + fullPath + " " + solutionName;
                    Process pr = Runtime.getRuntime().exec(execPath);

                    pr.waitFor();
                    Scanner s = new Scanner(pr.getInputStream()).useDelimiter("\\A");
                    while (s.hasNext()) {
                        log.info(s.next());
                    }
                    s = new Scanner(pr.getErrorStream()).useDelimiter("\\A");
                    while (s.hasNext()) {
                        log.info(s.next());
                    }
                    solutionStore.saveSolution(userId, fullPath + solutionName);
                } catch (InterruptedException  | IOException e) {
                    log.error(e.getMessage());
                }
                break;
            case "python":
                fullPath = "python " + fullPath;
                solutionStore.saveSolution(userId, fullPath + solutionName);
                break;
            default:
                log.error("Can't find " + solutionLanguage);
                break;
        }
    }
}
