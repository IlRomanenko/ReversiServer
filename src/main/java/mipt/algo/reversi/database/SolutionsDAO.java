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

    private static final String SCHEMA_NAME = "Reversi";

    private PreparedStatement saveSolutionStatement;
    private PreparedStatement addSolutionToUserStatement;
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

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA_NAME);

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS Reversi.Users (" +
                        "id INTEGER NOT NULL AUTO_INCREMENT," +
                        "username VARCHAR NOT NULL," +
                        "password VARCHAR NOT NULL," +
                        "name VARCHAR NOT NULL," +
                        "surname VARCHAR NOT NULL," +
                        "PRIMARY KEY (username)" +
                        ");"
        );

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS Reversi.Solutions (" +
                        "id INTEGER NOT NULL," +
                        "execpath VARCHAR NOT NULL);"
        );

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS Reversi.UsersSolutions (" +
                        "user_id INTEGER NOT NULL," +
                        "solution_id INTEGER NOT NULL," +
                        "PRIMARY KEY (user_id, solution_id));"
        );


        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS Reversi.TestResults (" +
                        "id INTEGER NOT NULL AUTO_INCREMENT,"+
                        "firstSolutionId INTEGER NOT NULL," +
                        "secondSolutionId INTEGER NOT NULL," +
                        "result VARCHAR NOT NULL, "+
                        "time DATETIME NOT NULL);"
        );
    }

    private void prepareStatements() {
        try {
            connection = dataSource.getConnection();

            saveSolutionStatement = connection.prepareStatement(
                    "INSERT INTO Reversi.Solutions(EXECPATH) VALUES (?);", new String[]{"id"});

            addSolutionToUserStatement = connection.prepareStatement(
                    "INSERT INTO Reversi.UsersSolutions(user_id, solution_id) VALUES (?, ?);");

            getSolutionExecPathStatement = connection.prepareStatement(
                    "SELECT EXECPATH FROM Reversi.SOLUTIONS where id = ?");

            saveTestResultStatement = connection.prepareStatement(
                    "INSERT INTO REVERSI.TestResults(firstSolutionId, secondSolutionId, result, time) " +
                            "VALUES (?, ?, ?, ?);");

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
                    path = result.getString("execpath");
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
                saveSolutionStatement.setString(1, solutionExecPath);
                saveSolutionStatement.execute();
                ResultSet result = saveSolutionStatement.getGeneratedKeys();

                if (!result.next()) {
                    return false;
                }

                int solutionId = result.getInt("id");

                addSolutionToUserStatement.setInt(1, userId);
                addSolutionToUserStatement.setInt(2, solutionId);

                addSolutionToUserStatement.execute();

            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void saveTestResult(Integer firstSolution, Integer secondSolution, String result) {
        synchronized (lockObject) {
            try {
                saveTestResultStatement.setInt(1, firstSolution);
                saveTestResultStatement.setInt(2, secondSolution);
                saveTestResultStatement.setString(3, result);

                saveTestResultStatement.setDate(4, new Date(Calendar.getInstance().getTime().getTime()));

                saveTestResultStatement.execute();

            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
    }
}
