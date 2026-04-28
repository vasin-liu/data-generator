package org.springframework.boot.autoconfigure.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boot 4 removed the old Boot 3 kafka properties package that the internal
 * gensokyo starter still compiles against. This compatibility copy provides the
 * subset of the Boot 3 API surface that the starter needs at runtime.
 */
public class KafkaProperties {

    private List<String> bootstrapServers = new ArrayList<>(Collections.singletonList("localhost:9092"));
    private String clientId;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final Consumer consumer = new Consumer();
    private final Producer producer = new Producer();
    private final Listener listener = new Listener();
    private final Admin admin = new Admin();
    private final Streams streams = new Streams();
    private final Ssl ssl = new Ssl();
    private final Jaas jaas = new Jaas();
    private final Template template = new Template();
    private final Security security = new Security();
    private final Retry retry = new Retry();

    public List<String> getBootstrapServers() {
        return this.bootstrapServers;
    }

    public void setBootstrapServers(List<String> bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public Consumer getConsumer() {
        return this.consumer;
    }

    public Producer getProducer() {
        return this.producer;
    }

    public Listener getListener() {
        return this.listener;
    }

    public Admin getAdmin() {
        return this.admin;
    }

    public Streams getStreams() {
        return this.streams;
    }

    public Ssl getSsl() {
        return this.ssl;
    }

    public Jaas getJaas() {
        return this.jaas;
    }

    public Template getTemplate() {
        return this.template;
    }

    public Security getSecurity() {
        return this.security;
    }

    public Retry getRetry() {
        return this.retry;
    }

    public Map<String, Object> buildConsumerProperties() {
        return buildConsumerProperties(null);
    }

    public Map<String, Object> buildConsumerProperties(SslBundles sslBundles) {
        Map<String, Object> properties = buildCommonProperties();
        properties.putAll(this.consumer.buildProperties(sslBundles));
        return properties;
    }

    public Map<String, Object> buildProducerProperties() {
        return buildProducerProperties(null);
    }

    public Map<String, Object> buildProducerProperties(SslBundles sslBundles) {
        Map<String, Object> properties = buildCommonProperties();
        properties.putAll(this.producer.buildProperties(sslBundles));
        return properties;
    }

    public Map<String, Object> buildAdminProperties(SslBundles sslBundles) {
        Map<String, Object> properties = buildCommonProperties();
        properties.putAll(this.admin.buildProperties(sslBundles));
        return properties;
    }

    public Map<String, Object> buildStreamsProperties(SslBundles sslBundles) {
        Map<String, Object> properties = buildCommonProperties();
        properties.putAll(this.streams.buildProperties(sslBundles));
        return properties;
    }

    private Map<String, Object> buildCommonProperties() {
        Map<String, Object> properties = new HashMap<>();
        if (this.bootstrapServers != null && !this.bootstrapServers.isEmpty()) {
            properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        }
        if (this.clientId != null && !this.clientId.isBlank()) {
            properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
        }
        properties.putAll(this.properties);
        return properties;
    }

    public static class Consumer {

        private final Ssl ssl = new Ssl();
        private final Security security = new Security();
        private Duration autoCommitInterval;
        private String autoOffsetReset;
        private List<String> bootstrapServers;
        private String clientId;
        private Boolean enableAutoCommit;
        private Duration fetchMaxWait;
        private DataSize fetchMinSize;
        private String groupId;
        private Duration heartbeatInterval;
        private IsolationLevel isolationLevel;
        private Class<?> keyDeserializer = StringDeserializer.class;
        private Class<?> valueDeserializer = StringDeserializer.class;
        private Integer maxPollRecords;
        private Duration maxPollInterval;
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Ssl getSsl() {
            return this.ssl;
        }

        public Security getSecurity() {
            return this.security;
        }

        public Duration getAutoCommitInterval() {
            return this.autoCommitInterval;
        }

        public void setAutoCommitInterval(Duration autoCommitInterval) {
            this.autoCommitInterval = autoCommitInterval;
        }

        public String getAutoOffsetReset() {
            return this.autoOffsetReset;
        }

        public void setAutoOffsetReset(String autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
        }

        public List<String> getBootstrapServers() {
            return this.bootstrapServers;
        }

        public void setBootstrapServers(List<String> bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getClientId() {
            return this.clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Boolean getEnableAutoCommit() {
            return this.enableAutoCommit;
        }

        public void setEnableAutoCommit(Boolean enableAutoCommit) {
            this.enableAutoCommit = enableAutoCommit;
        }

        public Duration getFetchMaxWait() {
            return this.fetchMaxWait;
        }

        public void setFetchMaxWait(Duration fetchMaxWait) {
            this.fetchMaxWait = fetchMaxWait;
        }

        public DataSize getFetchMinSize() {
            return this.fetchMinSize;
        }

        public void setFetchMinSize(DataSize fetchMinSize) {
            this.fetchMinSize = fetchMinSize;
        }

        public String getGroupId() {
            return this.groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public Duration getHeartbeatInterval() {
            return this.heartbeatInterval;
        }

        public void setHeartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        public IsolationLevel getIsolationLevel() {
            return this.isolationLevel;
        }

        public void setIsolationLevel(IsolationLevel isolationLevel) {
            this.isolationLevel = isolationLevel;
        }

        public Class<?> getKeyDeserializer() {
            return this.keyDeserializer;
        }

        public void setKeyDeserializer(Class<?> keyDeserializer) {
            this.keyDeserializer = keyDeserializer;
        }

        public Class<?> getValueDeserializer() {
            return this.valueDeserializer;
        }

        public void setValueDeserializer(Class<?> valueDeserializer) {
            this.valueDeserializer = valueDeserializer;
        }

        public Integer getMaxPollRecords() {
            return this.maxPollRecords;
        }

        public void setMaxPollRecords(Integer maxPollRecords) {
            this.maxPollRecords = maxPollRecords;
        }

        public Duration getMaxPollInterval() {
            return this.maxPollInterval;
        }

        public void setMaxPollInterval(Duration maxPollInterval) {
            this.maxPollInterval = maxPollInterval;
        }

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Map<String, Object> buildProperties(SslBundles sslBundles) {
            Map<String, Object> properties = new HashMap<>();
            if (this.bootstrapServers != null && !this.bootstrapServers.isEmpty()) {
                properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
            }
            if (this.clientId != null && !this.clientId.isBlank()) {
                properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
            }
            if (this.enableAutoCommit != null) {
                properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, this.enableAutoCommit);
            }
            if (this.autoCommitInterval != null) {
                properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, this.autoCommitInterval.toMillis());
            }
            if (this.autoOffsetReset != null && !this.autoOffsetReset.isBlank()) {
                properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.autoOffsetReset);
            }
            if (this.fetchMaxWait != null) {
                properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, this.fetchMaxWait.toMillis());
            }
            if (this.fetchMinSize != null) {
                properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, toIntBytes(this.fetchMinSize));
            }
            if (this.groupId != null && !this.groupId.isBlank()) {
                properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
            }
            if (this.heartbeatInterval != null) {
                properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, this.heartbeatInterval.toMillis());
            }
            if (this.isolationLevel != null) {
                properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, this.isolationLevel.name().toLowerCase());
            }
            if (this.keyDeserializer != null) {
                properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, this.keyDeserializer);
            }
            if (this.valueDeserializer != null) {
                properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, this.valueDeserializer);
            }
            if (this.maxPollRecords != null) {
                properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.maxPollRecords);
            }
            if (this.maxPollInterval != null) {
                properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, this.maxPollInterval.toMillis());
            }
            properties.putAll(this.properties);
            properties.putAll(this.security.getProperties());
            properties.putAll(this.ssl.getProperties());
            return properties;
        }
    }

    public static class Producer {

        private final Ssl ssl = new Ssl();
        private final Security security = new Security();
        private String acks;
        private DataSize batchSize;
        private List<String> bootstrapServers;
        private DataSize bufferMemory;
        private String clientId;
        private String compressionType;
        private Class<?> keySerializer = StringSerializer.class;
        private Class<?> valueSerializer = StringSerializer.class;
        private Integer retries;
        private String transactionIdPrefix;
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Ssl getSsl() {
            return this.ssl;
        }

        public Security getSecurity() {
            return this.security;
        }

        public String getAcks() {
            return this.acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public DataSize getBatchSize() {
            return this.batchSize;
        }

        public void setBatchSize(DataSize batchSize) {
            this.batchSize = batchSize;
        }

        public List<String> getBootstrapServers() {
            return this.bootstrapServers;
        }

        public void setBootstrapServers(List<String> bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public DataSize getBufferMemory() {
            return this.bufferMemory;
        }

        public void setBufferMemory(DataSize bufferMemory) {
            this.bufferMemory = bufferMemory;
        }

        public String getClientId() {
            return this.clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getCompressionType() {
            return this.compressionType;
        }

        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }

        public Class<?> getKeySerializer() {
            return this.keySerializer;
        }

        public void setKeySerializer(Class<?> keySerializer) {
            this.keySerializer = keySerializer;
        }

        public Class<?> getValueSerializer() {
            return this.valueSerializer;
        }

        public void setValueSerializer(Class<?> valueSerializer) {
            this.valueSerializer = valueSerializer;
        }

        public Integer getRetries() {
            return this.retries;
        }

        public void setRetries(Integer retries) {
            this.retries = retries;
        }

        public String getTransactionIdPrefix() {
            return this.transactionIdPrefix;
        }

        public void setTransactionIdPrefix(String transactionIdPrefix) {
            this.transactionIdPrefix = transactionIdPrefix;
        }

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Map<String, Object> buildProperties(SslBundles sslBundles) {
            Map<String, Object> properties = new HashMap<>();
            if (this.bootstrapServers != null && !this.bootstrapServers.isEmpty()) {
                properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
            }
            if (this.clientId != null && !this.clientId.isBlank()) {
                properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
            }
            if (this.acks != null && !this.acks.isBlank()) {
                properties.put(ProducerConfig.ACKS_CONFIG, this.acks);
            }
            if (this.batchSize != null) {
                properties.put(ProducerConfig.BATCH_SIZE_CONFIG, toIntBytes(this.batchSize));
            }
            if (this.bufferMemory != null) {
                properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, this.bufferMemory.toBytes());
            }
            if (this.compressionType != null && !this.compressionType.isBlank()) {
                properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, this.compressionType);
            }
            if (this.keySerializer != null) {
                properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, this.keySerializer);
            }
            if (this.valueSerializer != null) {
                properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, this.valueSerializer);
            }
            if (this.retries != null) {
                properties.put(ProducerConfig.RETRIES_CONFIG, this.retries);
            }
            if (this.transactionIdPrefix != null && !this.transactionIdPrefix.isBlank()) {
                properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, this.transactionIdPrefix);
            }
            properties.putAll(this.properties);
            properties.putAll(this.security.getProperties());
            properties.putAll(this.ssl.getProperties());
            return properties;
        }
    }

    public static class Listener {
    }

    public static class Admin {
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Map<String, Object> buildProperties(SslBundles sslBundles) {
            return new HashMap<>(this.properties);
        }
    }

    public static class Streams {
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Map<String, Object> buildProperties(SslBundles sslBundles) {
            return new HashMap<>(this.properties);
        }
    }

    public static class Ssl {
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Map<String, String> getProperties() {
            return this.properties;
        }
    }

    public static class Jaas {
    }

    public static class Template {
        private String defaultTopic;
        private String transactionIdPrefix;
        private boolean observationEnabled;

        public String getDefaultTopic() {
            return this.defaultTopic;
        }

        public void setDefaultTopic(String defaultTopic) {
            this.defaultTopic = defaultTopic;
        }

        public String getTransactionIdPrefix() {
            return this.transactionIdPrefix;
        }

        public void setTransactionIdPrefix(String transactionIdPrefix) {
            this.transactionIdPrefix = transactionIdPrefix;
        }

        public boolean isObservationEnabled() {
            return this.observationEnabled;
        }

        public void setObservationEnabled(boolean observationEnabled) {
            this.observationEnabled = observationEnabled;
        }
    }

    public static class Security {
        private final Map<String, String> properties = new LinkedHashMap<>();

        public Map<String, String> getProperties() {
            return this.properties;
        }
    }

    public static class Retry {
    }

    public enum IsolationLevel {
        READ_UNCOMMITTED,
        READ_COMMITTED
    }

    private static int toIntBytes(DataSize dataSize) {
        long bytes = dataSize.toBytes();
        return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
    }
}
