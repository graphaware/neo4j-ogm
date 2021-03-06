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

package org.neo4j.ogm.config;

import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for ensuring that the various pluggable components
 * required by the OGM can be loaded.
 * The Components class can be explicitly configured via an {@link Configuration} instance.
 * If no explicit configuration is supplied, the class will attempt to auto-configure.
 * Auto-configuration is accomplished using a properties file. By default, this file
 * is called "ogm.properties" and it must be available on the class path.
 * You can supply a different configuration properties file, by specifying a system property
 * "ogm.properties" that refers to the configuration file you want to use. Your alternative
 * configuration file must be on the class path.
 * The properties file should contain the desired configuration values for each of the
 * various components - Driver, Compiler, etc. Please refer to the relevant configuration
 * for each of these.
 *
 * @author vince
 */
public class Components {

    private Components() {
    }

    private static final Logger logger = LoggerFactory.getLogger(Components.class);

    private static Configuration configuration = new Configuration();
    private static Driver driver;

    /**
     * Configure the OGM from a pre-built Configuration class
     *
     * @param configuration The configuration to use
     */
    public static void configure(Configuration configuration) {
        // new configuration object, or update of current one?
        if (Components.configuration != configuration) {
            destroy();
            Components.configuration = configuration;
        } else {
            // same config - but have we switched drivers?
            if (driver != null && !driver.getClass().getCanonicalName().equals(configuration.getDriverClassName())) {
                driver.close();
                driver = null;
            }
        }
    }

    /**
     * Configure the OGM from the specified config file
     *
     * @param configurationFileName The config file to use
     */
    public static void configure(String configurationFileName) {
        destroy();
        configuration = new Configuration(new ClasspathConfigurationSource(configurationFileName));
    }

    /**
     * Returns the current OGM {@link Driver}
     * Normally only one instance of the driver exists for the lifetime of the application
     * You cannot use this method to find out if a driver is initialised because it will attempt to
     * initialise the driver if it is not.
     *
     * @return an instance of the {@link Driver} to be used by the OGM
     */
    public synchronized static Driver driver() {
        if (driver == null) {
            loadDriver();
        }
        return driver;
    }

    /**
     * The OGM Components can be auto-configured from a properties file, "ogm.properties", or
     * a similar configuration file, specified by a system property or environment variable called "ogm.properties".
     * If an auto-configure properties file is not available by any of these means, the Components class should be configured
     * by passing in a Configuration object to the configure method, or an explicit configuration file name
     */
    private synchronized static void autoConfigure() {
        String configFileName = System.getenv("ogm.properties");

        if (configFileName == null) {
            configFileName = System.getProperty("ogm.properties");
            if (configFileName == null) {
                configFileName = "ogm.properties";
            }
        }
        configure(configFileName);
    }

    /**
     * Loads the configured Neo4j {@link Driver} and stores it on this class
     */
    private static void loadDriver() {
        if (configuration.getDriverClassName() == null) {
            autoConfigure();
        }
        setDriver(loadDriver(configuration));
    }

    /**
     * Loads and initialises a Driver using the specified DriverConfiguration
     *
     * @param configuration an instance of {@link Configuration} with which to configure the driver
     * @return the named {@link Driver} if found, otherwise throws a ServiceNotFoundException
     */
    static Driver loadDriver(Configuration configuration) {
        String driverClassName = configuration.getDriverClassName();
        logger.info("Loading driver: [{}]", driverClassName);

        try {
            final Class<?> driverClass = Class.forName(driverClassName);
            Driver driver = (Driver) driverClass.newInstance();
            driver.configure(configuration);
            return driver;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Error loading driver. Is the driver defined on the classpath?: {}", e);
        }

        throw new ServiceNotFoundException("Could not load driver: " + driverClassName + ".");
    }

    /**
     * Sets a new {@link Driver} to be used by the OGM.
     * If a different driver is in use, it will be closed first. In addition, the {@link Configuration} is updated
     * to reflect the correct classname for the new driver.
     *
     * @param driver an instance of {@link Driver} to be used by the OGM.
     */
    public static void setDriver(Driver driver) {

        logger.debug("Setting driver to: {}", driver.getClass().getName());

        if (Components.driver != null && Components.driver != driver) {
            Components.driver.close();
            Components.getConfiguration().setDriverClassName(driver.getClass().getCanonicalName());
        }

        Components.driver = driver;
    }

    /**
     * Releases any current driver resources and clears the current configuration
     */
    public synchronized static void destroy() {

        if (driver != null) {
            driver.close();
            driver = null;
        }
        configuration.clear();
    }

    /**
     * There is a single configuration object, which should never be null, associated with the Components class
     * You can update this configuration in-situ, or you can replace the configuration with another.
     *
     * @return the current Configuration object
     */
    public static Configuration getConfiguration() {
        return configuration;
    }
}
