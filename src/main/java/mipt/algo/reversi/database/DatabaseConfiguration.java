package mipt.algo.reversi.database;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;

import javax.naming.Reference;
import javax.sql.DataSource;


/**
 * ReversiServer
 * mipt.algo.reversi.database
 * <p>
 * Created by ilya on 03.05.17.
 */
@Repository
public class DatabaseConfiguration {


    @Value("${mipt.algo.reversi.database.jdbc}") String jdbcUrl;
    @Value("${mipt.algo.reversi.database.username}") String username;
    @Value("${mipt.algo.reversi.database.password:}") String password;

    @Bean
    public DataSource databaseDataSource() {

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setServerName("localhost");
        String t = dataSource.getURL();
        dataSource.setURL("jdbc:mysql://localhost:3306/reversi?useLegacyDatetimeCode=false&serverTimezone=UTC");
        return dataSource;

        /*HikariConfig config = new HikariConfig();
        config.setDriverClassName(org.h2.Driver.class.getName());
        config.setJdbcUrl("jdbc:h2:" + jdbcUrl + ";AUTO_SERVER=TRUE");
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);*/
    }
}
