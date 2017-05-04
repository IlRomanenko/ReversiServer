package mipt.algo.reversi.testing;

import mipt.algo.reversi.database.SolutionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
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

    @Value("${mipt.algo.reversi.testing.threads}")
    private Integer threads;

    private static final Random random = new Random();

    private Queue<Integer> portPool;

    @PostConstruct
    public void postConstruct() {
        executorPool = Executors.newFixedThreadPool(threads);
        portPool = new LinkedList<>();
        for (int i = 4242; i < 32000; i++) {
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

        GameServer server = new GameServer(portPool.poll(), token, solutionStore);
        String firstExecPath = solutionStore.getSolutionExecPath(firstId);
        String secondExecPath = solutionStore.getSolutionExecPath(secondId);

        server.createListeningServer();
        try {
            Runtime.getRuntime().exec(firstExecPath, new String[] { port.toString(), token });
            Runtime.getRuntime().exec(secondExecPath, new String[] { port.toString(), token });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        server.testGame(firstId, secondId);
        portPool.add(port);
    }

    public void compileSolution(Integer userId, String solutionPath, String solutionLanguage) {
        String solutionName = "_" + solutionLanguage;
        String fullPath;
        switch (solutionLanguage) {
            case "cpp":
                try {
                    Runtime.getRuntime().exec("make", new String[]{""});

                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                break;
            case "python":

                solutionName += Calendar.getInstance().getTime().getTime() + "_" + userId;
                fullPath = "python " + HOME_DIRECTORY + File.pathSeparator + userId.toString() + File.pathSeparator + solutionName;
                solutionStore.saveSolution(userId, fullPath);
                break;
            default:
                log.error("Can't find " + solutionLanguage);
                break;
        }
    }
}
