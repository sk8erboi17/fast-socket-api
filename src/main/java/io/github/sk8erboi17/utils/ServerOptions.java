package io.github.sk8erboi17.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ServerOptions {

    private static final Logger log = LoggerFactory.getLogger(ServerOptions.class);
    private static final ServerOptions instance = new ServerOptions();

    private static final String KEEP_ALIVE_PROP = "keepAlive";
    private static final String TIMEOUT_PROP = "keepAliveTimeoutSeconds";
    private static final String BUFFER_POOLS_PROP = "bufferPools";
    private static final String THREADS_NUMBER_PROP = "threadsNumber";
    private static final String SERVER_NAME_PROP = "server_name";
    private static final String PROPERTIES_FILENAME = "server_options.properties";

    private boolean keepAlive;
    private int timeout;
    private int bufferPools;
    private int threadsNumber;
    private String serverName;

    private ServerOptions() {
        initializeOrUpdateProperties();
    }

    private void initializeOrUpdateProperties() {
        Properties fileProps = new Properties();
        File propertiesFile = new File(System.getProperty("user.dir"), PROPERTIES_FILENAME);

        if (propertiesFile.exists()) {
            try (InputStream input = new FileInputStream(propertiesFile)) {
                fileProps.load(input);
            } catch (IOException e) {
                log.error("Could not read '{}'. Default values will be used.", PROPERTIES_FILENAME, e);
            }
        }

        Map<String, String> defaultProps = new LinkedHashMap<>();
        defaultProps.put(KEEP_ALIVE_PROP, "true");
        defaultProps.put(TIMEOUT_PROP, "30");
        defaultProps.put(BUFFER_POOLS_PROP, "128");
        defaultProps.put(THREADS_NUMBER_PROP, "8");
        defaultProps.put(SERVER_NAME_PROP, "fast-socket-api");

        boolean needsUpdate = !propertiesFile.exists();
        for (Map.Entry<String, String> entry : defaultProps.entrySet()) {
            if (fileProps.getProperty(entry.getKey()) == null) {
                fileProps.setProperty(entry.getKey(), entry.getValue());
                log.warn("Missing property '{}'. Adding default value '{}' to {}.",
                        entry.getKey(), entry.getValue(), PROPERTIES_FILENAME);
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            try (OutputStream output = new FileOutputStream(propertiesFile)) {
                fileProps.store(output, "Default Server Options");
                log.info("File '{}' has been created or updated.", PROPERTIES_FILENAME);
            } catch (IOException e) {
                log.error("FATAL: Could not create or update the properties file '{}'.", PROPERTIES_FILENAME, e);
            }
        }

        loadValuesFromProperties(fileProps);
    }


    private void loadValuesFromProperties(Properties properties) {
        try {
            this.keepAlive = Boolean.parseBoolean(properties.getProperty(KEEP_ALIVE_PROP));
            this.timeout = Integer.parseInt(properties.getProperty(TIMEOUT_PROP));
            this.bufferPools = Integer.parseInt(properties.getProperty(BUFFER_POOLS_PROP));
            this.threadsNumber = Integer.parseInt(properties.getProperty(THREADS_NUMBER_PROP));
            this.serverName = properties.getProperty(SERVER_NAME_PROP);
        } catch (NumberFormatException e) {
            log.error("Invalid number format in '{}'. Fallback values will be used.", PROPERTIES_FILENAME, e);
            setFallbackDefaults();
        }
    }

    private void setFallbackDefaults() {
        this.keepAlive = true;
        this.timeout = 30;
        this.bufferPools = 128;
        this.threadsNumber = 8;
        this.serverName = "default-server";
    }

    public static ServerOptions getInstance() { return instance; }
    public boolean isKeepAlive() { return keepAlive; }
    public int getTimeout() { return timeout; }
    public int getBufferPools() { return bufferPools; }
    public int getThreadsNumber() { return threadsNumber; }
    public String getServerName() { return this.serverName; }
}