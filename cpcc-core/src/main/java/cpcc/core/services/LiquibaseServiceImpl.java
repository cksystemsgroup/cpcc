// This code is part of the CPCC-NG project.
//
// Copyright (c) 2015 Clemens Krainer <clemens.krainer@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software Foundation,
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

package cpcc.core.services;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.apache.tapestry5.ioc.annotations.Symbol;
import org.slf4j.Logger;

import cpcc.core.base.CoreConstants;

/**
 * LiquibaseServiceImpl
 */
public class LiquibaseServiceImpl implements LiquibaseService
{
    private static final String JNDI_COMP_ENV = "java:/comp/env";

    private final Logger logger;
    private String changeLog;
    private String dbResourceName;

    /**
     * @param logger the system logger.
     * @param changeLog the Liquibase change log.
     * @param databaseUrl the Liquibase database URL.
     * @throws LiquibaseException in case of errors.
     */
    public LiquibaseServiceImpl(Logger logger, @Symbol(CoreConstants.PROP_LIQUIBASE_CHANGE_LOG_FILE) String changeLog,
        @Symbol(CoreConstants.PROP_LIQUIBASE_DATABASE_URL) String databaseUrl) throws LiquibaseException
    {
        this.logger = logger;
        this.changeLog = changeLog;

        if (!databaseUrl.startsWith(JNDI_COMP_ENV + "/"))
        {
            throw new LiquibaseException("Only JNDI URLs starting with '" + JNDI_COMP_ENV + "' are allowed!");
        }

        dbResourceName = databaseUrl.substring(JNDI_COMP_ENV.length() + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update()
    {
        try
        {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup(JNDI_COMP_ENV);
            DataSource dataSource = (DataSource) envContext.lookup(dbResourceName);
            try (Connection connection = dataSource.getConnection())
            {
                JdbcConnection jdbcConnection = new JdbcConnection(connection);
                Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
                ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

                Liquibase l = new Liquibase(changeLog, resourceAccessor, database);
                l.update("");
            }
        }
        catch (LiquibaseException | NamingException | SQLException e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}