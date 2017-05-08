package mipt.algo.reversi.server;

import mipt.algo.reversi.testing.TestServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ReversiServer
 * mipt.algo.reversi.server
 * <p>
 * Created by ilya on 03.05.17.
 */
@RestController
public class TestingController {

    @Autowired
    private TestServer server;

    @RequestMapping(path = "/test/{firstSolutionId}&{secondSolutionId}", method = RequestMethod.GET)
    public void testSolutions(@PathVariable Integer firstSolutionId,
                              @PathVariable Integer secondSolutionId) {
        server.startTesting(firstSolutionId, secondSolutionId);
    }

    @RequestMapping(path = "/compile/{userId}&{solutionLanguage}", method = RequestMethod.POST)
    public void compileSolution(@PathVariable Integer userId,
                                @PathVariable String solutionLanguage,
                                @RequestBody String solutionPath) {
        server.compileSolution(userId, solutionPath, solutionLanguage);
    }

}
