package com.github.fge.grappa.debugger.h2.db;

import com.github.fge.grappa.debugger.ZipTraceDbFactory;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ParametersAreNonnullByDefault
public final class H2TraceDbFactory
    implements ZipTraceDbFactory
{
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";

    private static final String H2_JDBC_URL_FORMAT
        = "jdbc:h2:%s;LOG=0;LOCK_MODE=0;UNDO_LOG=0;CACHE_SIZE=131072";

    @Override
    public H2TraceDb create(final Path arg)
        throws IOException
    {
        final Path dbpath
            = Files.createTempDirectory("grappa-debugger").toRealPath();

        final String jdbcUrl
            = String.format(H2_JDBC_URL_FORMAT, dbpath.resolve("db"));

        initdb(jdbcUrl);

        final JdbcConnectionPool pool = JdbcConnectionPool.create(jdbcUrl,
            H2_USERNAME, H2_PASSWORD);

        final ConnectionProvider provider = new H2ConnectionProvider(pool);

        final Configuration cfg = new DefaultConfiguration();
        cfg.set(provider);
        cfg.set(SQLDialect.H2);

        final DSLContext jooq = DSL.using(cfg);

        return new H2TraceDb(arg, dbpath, jooq, pool);
    }

    private void initdb(final String jdbcUrl)
    {
        final Flyway flyway = new Flyway();

        flyway.setLocations("classpath:db/h2");
        flyway.setDataSource(jdbcUrl, H2_USERNAME, H2_PASSWORD);

        flyway.migrate();
    }
}
