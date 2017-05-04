package mipt.algo.reversi.database;

/**
 * ReversiServer
 * mipt.algo.reversi.database
 * <p>
 * Created by ilya on 03.05.17.
 */
public interface SolutionStore {

    String getSolutionExecPath(Integer solutionId);

    boolean saveSolution(Integer userId, String solutionExecPath);

    void saveTestResult(Integer firstSolution, Integer secondSolution, String result);
}
