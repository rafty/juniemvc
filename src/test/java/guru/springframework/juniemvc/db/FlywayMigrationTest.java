package guru.springframework.juniemvc.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsApplied_onEmptyDb() {
        // Verify customer table exists
        Integer customerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CUSTOMER'",
                Integer.class
        );
        assertThat(customerCount).isNotNull();
        assertThat(customerCount).isGreaterThan(0);

        // Verify beer_order.customer_id column exists
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BEER_ORDER' AND COLUMN_NAME = 'CUSTOMER_ID'",
                Integer.class
        );
        assertThat(columnCount).isNotNull();
        assertThat(columnCount).isGreaterThan(0);
    }
}
