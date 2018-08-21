/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.util.Enumeration;

import javax.sql.DataSource;

import org.killbill.billing.platform.test.PlatformDBTestingHelper;
import org.killbill.billing.util.glue.IDBISetup;
import org.killbill.billing.util.io.IOUtils;
import org.killbill.commons.embeddeddb.EmbeddedDB;
import org.killbill.commons.jdbi.guice.DBIProvider;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class DBTestingHelper extends PlatformDBTestingHelper {

    private static DBTestingHelper dbTestingHelper = null;

    private DBI dbi;

    public static synchronized DBTestingHelper get() {
        if (dbTestingHelper == null) {
            dbTestingHelper = new DBTestingHelper();
        }
        return dbTestingHelper;
    }

    private DBTestingHelper() {
        super();
    }

    @Override
    public synchronized IDBI getDBI() {
        if (dbi == null) {
            final RetryableDataSource retryableDataSource = new RetryableDataSource(getDataSource());
            dbi = (DBI) new DBIProvider(null, retryableDataSource, null).get();

            // Register KB specific mappers
            for (final ResultSetMapperFactory resultSetMapperFactory : IDBISetup.mapperFactoriesToRegister()) {
                dbi.registerMapper(resultSetMapperFactory);
            }
            for (final ResultSetMapper resultSetMapper : IDBISetup.mappersToRegister()) {
                dbi.registerMapper(resultSetMapper);
            }
        }
        return dbi;
    }

    protected synchronized void executePostStartupScripts() throws IOException {

        final EmbeddedDB instance = getInstance();
        final String databaseSpecificDDL = "org/killbill/billing/util/" + "ddl-" + instance.getDBEngine().name().toLowerCase() + ".sql";
        installDDLSilently(databaseSpecificDDL);

        // We always want the accounts and tenants table
        instance.executeScript("drop table if exists accounts;" +
                               "CREATE TABLE accounts (\n" +
                               "    record_id serial unique,\n" +
                               "    id varchar(36) NOT NULL,\n" +
                               "    external_key varchar(255) NOT NULL,\n" +
                               "    email varchar(128) DEFAULT NULL,\n" +
                               "    name varchar(100) DEFAULT NULL,\n" +
                               "    first_name_length int DEFAULT NULL,\n" +
                               "    currency varchar(3) DEFAULT NULL,\n" +
                               "    billing_cycle_day_local int DEFAULT NULL,\n" +
                               "    parent_account_id varchar(36) DEFAULT NULL,\n" +
                               "    is_payment_delegated_to_parent boolean DEFAULT FALSE,\n" +
                               "    payment_method_id varchar(36) DEFAULT NULL,\n" +
                               "    reference_time datetime NOT NULL,\n" +
                               "    time_zone varchar(50) NOT NULL,\n" +
                               "    locale varchar(5) DEFAULT NULL,\n" +
                               "    address1 varchar(100) DEFAULT NULL,\n" +
                               "    address2 varchar(100) DEFAULT NULL,\n" +
                               "    company_name varchar(50) DEFAULT NULL,\n" +
                               "    city varchar(50) DEFAULT NULL,\n" +
                               "    state_or_province varchar(50) DEFAULT NULL,\n" +
                               "    country varchar(50) DEFAULT NULL,\n" +
                               "    postal_code varchar(16) DEFAULT NULL,\n" +
                               "    phone varchar(25) DEFAULT NULL,\n" +
                               "    notes varchar(4096) DEFAULT NULL,\n" +
                               "    migrated boolean default false,\n" +
                               "    created_date datetime NOT NULL,\n" +
                               "    created_by varchar(50) NOT NULL,\n" +
                               "    updated_date datetime DEFAULT NULL,\n" +
                               "    updated_by varchar(50) DEFAULT NULL,\n" +
                               "    tenant_record_id bigint /*! unsigned */ not null default 0,\n" +
                               "    PRIMARY KEY(record_id)\n" +
                               ");");
        instance.executeScript("DROP TABLE IF EXISTS tenants;\n" +
                               "CREATE TABLE tenants (\n" +
                               "    record_id serial unique,\n" +
                               "    id varchar(36) NOT NULL,\n" +
                               "    external_key varchar(255) NULL,\n" +
                               "    api_key varchar(128) NULL,\n" +
                               "    api_secret varchar(128) NULL,\n" +
                               "    api_salt varchar(128) NULL,\n" +
                               "    created_date datetime NOT NULL,\n" +
                               "    created_by varchar(50) NOT NULL,\n" +
                               "    updated_date datetime DEFAULT NULL,\n" +
                               "    updated_by varchar(50) DEFAULT NULL,\n" +
                               "    PRIMARY KEY(record_id)\n" +
                               ");");

        // We always want the basic tables when we do account_record_id lookups (e.g. for custom fields, tags or junction)
        instance.executeScript("DROP TABLE IF EXISTS bundles;\n" +
                               "CREATE TABLE bundles (\n" +
                               "    record_id serial unique,\n" +
                               "    id varchar(36) NOT NULL,\n" +
                               "    external_key varchar(255) NOT NULL,\n" +
                               "    account_id varchar(36) NOT NULL,\n" +
                               "    last_sys_update_date datetime,\n" +
                               "    original_created_date datetime NOT NULL,\n" +
                               "    created_by varchar(50) NOT NULL,\n" +
                               "    created_date datetime NOT NULL,\n" +
                               "    updated_by varchar(50) NOT NULL,\n" +
                               "    updated_date datetime NOT NULL,\n" +
                               "    account_record_id bigint /*! unsigned */ not null,\n" +
                               "    tenant_record_id bigint /*! unsigned */ not null default 0,\n" +
                               "    PRIMARY KEY(record_id)\n" +
                               ");");
        instance.executeScript("DROP TABLE IF EXISTS subscriptions;\n" +
                               "CREATE TABLE subscriptions (\n" +
                               "    record_id serial unique,\n" +
                               "    id varchar(36) NOT NULL,\n" +
                               "    bundle_id varchar(36) NOT NULL,\n" +
                               "    category varchar(32) NOT NULL,\n" +
                               "    start_date datetime NOT NULL,\n" +
                               "    bundle_start_date datetime NOT NULL,\n" +
                               "    charged_through_date datetime DEFAULT NULL,\n" +
                               "    migrated bool NOT NULL default FALSE,\n" +
                               "    created_by varchar(50) NOT NULL,\n" +
                               "    created_date datetime NOT NULL,\n" +
                               "    updated_by varchar(50) NOT NULL,\n" +
                               "    updated_date datetime NOT NULL,\n" +
                               "    account_record_id bigint /*! unsigned */ not null,\n" +
                               "    tenant_record_id bigint /*! unsigned */ not null default 0,\n" +
                               "    PRIMARY KEY(record_id)\n" +
                               ");");

        // HACK (PIERRE): required by invoice tests which perform payments lookups to find the account record id for the internal callcontext
        instance.executeScript("DROP TABLE IF EXISTS payments;\n" +
                               "CREATE TABLE payments (\n" +
                               "    record_id serial unique,\n" +
                               "    id varchar(36) NOT NULL,\n" +
                               "    account_id varchar(36) NOT NULL,\n" +
                               "    payment_method_id varchar(36) NOT NULL,\n" +
                               "    external_key varchar(255) NOT NULL,\n" +
                               "    state_name varchar(64) DEFAULT NULL,\n" +
                               "    last_success_state_name varchar(64) DEFAULT NULL,\n" +
                               "    created_by varchar(50) NOT NULL,\n" +
                               "    created_date datetime NOT NULL,\n" +
                               "    updated_by varchar(50) NOT NULL,\n" +
                               "    updated_date datetime NOT NULL,\n" +
                               "    account_record_id bigint /*! unsigned */ not null,\n" +
                               "    tenant_record_id bigint /*! unsigned */ not null default 0,\n" +
                               "    PRIMARY KEY (record_id)\n" +
                               ");");

        for (final String pack : new String[]{"catalog", "account", "analytics", "beatrix", "subscription", "util", "payment", "invoice", "entitlement", "usage", "meter", "tenant"}) {
            for (final String ddlFile : new String[]{"ddl.sql", "ddl_test.sql"}) {
                final String resourceName = "org/killbill/billing/" + pack + "/" + ddlFile;
                installDDLSilently(resourceName);
            }
        }
    }

    private void installDDLSilently(final String resourceName) throws IOException {
        final ClassLoader classLoader = MoreObjects.firstNonNull(Thread.currentThread().getContextClassLoader(), DBTestingHelper.class.getClassLoader());
        final Enumeration<URL> resources = classLoader.getResources(resourceName);
        while (resources.hasMoreElements()) {
            final URL inputStream = resources.nextElement();

            final String ddl;
            try {
                ddl = IOUtils.toString(inputStream.openStream());
                getInstance().executeScript(ddl);
            } catch (final Exception ignored) {
                // The test doesn't have this module ddl in the classpath - that's fine
            }
        }
    }

    // DataSource which will retry recreating a connection once in case of a connection exception.
    // This is useful for transient network errors in tests when using a separate database (e.g. Docker container),
    // as we don't use a connection pool.
    private static final class RetryableDataSource implements DataSource {

        private static final Logger logger = LoggerFactory.getLogger(RetryableDataSource.class);

        private final DataSource delegate;

        private RetryableDataSource(final DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            try {
                return delegate.getConnection();
            } catch (final SQLNonTransientConnectionException e) {
                logger.warn("Unable to retrieve connection, attempting to retry", e);
                return delegate.getConnection();
            }
        }

        @Override
        public Connection getConnection(final String username, final String password) throws SQLException {
            try {
                return delegate.getConnection(username, password);
            } catch (final SQLNonTransientConnectionException e) {
                logger.warn("Unable to retrieve connection, attempting to retry", e);
                return delegate.getConnection(username, password);
            }
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(final PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(final int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        //@Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("javax.sql.DataSource.getParentLogger() is not currently supported by " + this.getClass().getName());
        }
    }
}
