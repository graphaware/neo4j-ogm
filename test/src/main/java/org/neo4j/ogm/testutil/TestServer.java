/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.testutil;

import org.apache.commons.io.IOUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.harness.internal.InProcessServerControls;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;
import org.neo4j.ogm.service.Components;
import org.neo4j.server.AbstractNeoServer;

import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Vince Bickers
 */
@SuppressWarnings("deprecation")
public class TestServer {

    private final Integer port;
    private final Integer transactionTimeoutSeconds;
    private final Boolean enableAuthentication;

    private AbstractNeoServer server;
    private GraphDatabaseService database;
    private ServerControls controls;

    private TestServer(Builder builder) {

        this.port = builder.port == null ? TestUtils.getAvailablePort() : builder.port;
        this.transactionTimeoutSeconds = builder.transactionTimeoutSeconds;
        this.enableAuthentication = builder.enableAuthentication;

        startServer();

    }

    private void startServer() {
        try {

            checkDriver();

            controls = TestServerBuilders.newInProcessBuilder()
                    .withConfig("dbms.security.auth_enabled", String.valueOf(enableAuthentication))
                    .withConfig("org.neo4j.server.webserver.port", String.valueOf(port))
                    .withConfig("org.neo4j.server.transaction.timeout", String.valueOf(transactionTimeoutSeconds))
                    .withConfig("dbms.security.auth_store.location", createAuthStore())
                    .newServer();

            initialise(controls);

            // ensure we shutdown this server when the JVM terminates, if its not been shutdown by user code
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdown();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException("Error starting in-process server",e);
        }

    }

    private void checkDriver() {
        DriverConfiguration driverConfiguration = Components.configuration().driverConfiguration();
        String driverClassName = driverConfiguration.getDriverClassName();
        if (driverClassName == null || driverClassName.equals(HttpDriver.class.getName()) == false) {
            Components.setDriver(new HttpDriver());
        }
    }

    private String createAuthStore() {
        // creates a temp auth store, with encrypted credentials "neo4j:password" if the server is authenticating connections
        try {
            Path authStore = Files.createTempFile("neo4j", "credentials");
            authStore.toFile().deleteOnExit();

            if (enableAuthentication) {
                try (Writer authStoreWriter = new FileWriter(authStore.toFile())) {
                    IOUtils.write("neo4j:SHA-256,03C9C54BF6EEF1FF3DFEB75403401AA0EBA97860CAC187D6452A1FCF4C63353A,819BDB957119F8DFFF65604C92980A91:", authStoreWriter);
                }
                driver().getConfiguration().setCredentials("neo4j", "password");
            }

            return authStore.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise(ServerControls controls) throws Exception {

        Field field = InProcessServerControls.class.getDeclaredField("server");
        field.setAccessible(true);
        server = (AbstractNeoServer) field.get(controls);
        database = server.getDatabase().getGraph();
        Components.driver().getConfiguration().setURI(url());
    }

    public Driver driver() {
        return Components.driver();
    }

    public synchronized void start() throws InterruptedException {
        server.start();
    }

    /**
     * Stops the underlying server bootstrapper and, in turn, the Neo4j server.
     */
    public synchronized void shutdown() {
        controls.close();
        database.shutdown();
    }

    /**
     * Waits for a period of time and checks the database availability afterwards
     * @param timeout milliseconds to wait
     * @return true if the database is available, false otherwise
     */
    public boolean isRunning(long timeout) {
        return database.isAvailable(timeout);
    }

    /**
     * Retrieves the base URL of the Neo4j database server used in the test.
     *
     * @return The URL of the Neo4j test server
     */
    public String url() {
        return server.baseUri().toString();
    }

    /**
     * Loads the specified CQL file from the classpath into the database.
     *
     * @param cqlFileName The name of the CQL file to load
     */
    public void loadClasspathCypherScriptFile(String cqlFileName) {
        new ExecutionEngine(this.database).execute(TestUtils.readCQLFile(cqlFileName).toString());
    }

    /**
     * Deletes all the nodes and relationships in the test database.
     */
    public void clearDatabase() {
        new ExecutionEngine(this.database).execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
    }

    /**
     * Retrieves the underlying {@link org.neo4j.graphdb.GraphDatabaseService} used in this test.
     *
     * @return The test {@link org.neo4j.graphdb.GraphDatabaseService}
     */
    public GraphDatabaseService getGraphDatabaseService() {
        return this.database;
    }

    public void close() {
        shutdown();
    }

    public static class Builder {

        private Integer port = null;
        private Integer transactionTimeoutSeconds = 60;
        private boolean enableAuthentication = false;

        public Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder transactionTimeoutSeconds(int transactionTimeoutSeconds) {
            this.transactionTimeoutSeconds = transactionTimeoutSeconds;
            return this;
        }

        public Builder enableAuthentication(boolean enableAuthentication) {
            this.enableAuthentication = enableAuthentication;
            return this;
        }

        public TestServer build() {
            return new TestServer(this);
        }

    }

}
