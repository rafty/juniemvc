package guru.springframework.juniemvc.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayStandaloneTest {

    @Test
    void migrate_emptyH2Db_createsCustomerAndFk() throws Exception {
        String dbName = "flywaytest_" + System.currentTimeMillis();
        String url = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement st = conn.createStatement()) {

            // Check customer table exists
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CUSTOMER'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThan(0);
            }

            // Check beer_order.customer_id column exists
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BEER_ORDER' AND COLUMN_NAME = 'CUSTOMER_ID'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThan(0);
            }
        }
    }
}
