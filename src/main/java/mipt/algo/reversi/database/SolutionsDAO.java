package mipt.algo.reversi.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Calendar;

/**
 * ReversiServer
 * mipt.algo.reversi.database
 * <p>
 * Created by ilya on 03.05.17.
 */
@Repository
public class SolutionsDAO implements SolutionStore {

    private static final Logger log = LoggerFactory.getLogger(SolutionsDAO.class);

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private Connection connection;

    private PreparedStatement saveSolutionStatement;
    private PreparedStatement getSolutionExecPathStatement;
    private PreparedStatement saveTestResultStatement;

    private final Object lockObject = new Object();

    @PostConstruct
    public void postConstruct() {
        jdbcTemplate = new JdbcTemplate(dataSource, false);
        initSchema();
        prepareStatements();
    }

    private void initSchema() {
        log.info("Initializing schema");

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS Solutions (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "user_id INT NOT NULL,"+
                        "execpath VARCHAR(1024) NOT NULL);"
        );

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS TestResults (" +
                        "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY ,"+
                        "firstSolutionId INT NOT NULL," +
                        "secondSolutionId INT NOT NULL," +
                        "firstScore INT NOT NULL, " +
                        "secondScore INT NOT NULL, " +
                        "result VARCHAR(255) NOT NULL, " +
                        "logFile VARCHAR(1024) NOT NULL, " +
                        "time DATETIME NOT NULL);"
        );
    }

    private void prepareStatements() {
        try {
            connection = dataSource.getConnection();

            saveSolutionStatement = connection.prepareStatement(
                    "INSERT INTO Solutions(user_id, execpath) VALUES (?, ?);", new String[]{"id"});

            getSolutionExecPathStatement = connection.prepareStatement(
                    "SELECT execpath FROM Solutions where id = ?");

            saveTestResultStatement = connection.prepareStatement(
                    "INSERT INTO TestResults(firstSolutionId, secondSolutionId, firstScore, " +
                            "secondScore, result, logFile, time) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?);");

        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public String getSolutionExecPath(Integer solutionId) {
        String path = "";
        synchronized (lockObject) {
            try {
                getSolutionExecPathStatement.setInt(1, solutionId);
                ResultSet result = getSolutionExecPathStatement.executeQuery();

                if (result.next()) {
                    path = result.getString(1);
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        return path;
    }

    @Override
    public boolean saveSolution(Integer userId, String solutionExecPath) {
        synchronized (lockObject) {
            try {
                saveSolutionStatement.setInt(1, userId);
                saveSolutionStatement.setString(2, solutionExecPath);
                saveSolutionStatement.execute();
                ResultSet result = saveSolutionStatement.getGeneratedKeys();

                if (!result.next()) {
                    return false;
                }

                result.getInt(1);

            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void saveTestResult(Integer firstSolution, Integer secondSolution,
                               Integer firstScore, Integer secondScore, String result, String logFile) {
        synchronized (lockObject) {
            try {
                saveTestResultStatement.setInt(1, firstSolution);
                saveTestResultStatement.setInt(2, secondSolution);
                saveTestResultStatement.setInt(3, firstScore);
                saveTestResultStatement.setInt(4, secondScore);
                saveTestResultStatement.setString(5, result);
                saveTestResultStatement.setString(6, logFile);

                saveTestResultStatement.setDate(7, new Date(Calendar.getInstance().getTime().getTime()));

                saveTestResultStatement.execute();

            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
    }
}
