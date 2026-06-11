package org.gensokyo.data.kafka.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.kafka.multiple")
public class MultipleKafkaClusterProperties {

    private String primary;
    private Map<String, KafkaClusterProperties> clusters = new LinkedHashMap<>();

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Map<String, KafkaClusterProperties> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, KafkaClusterProperties> clusters) {
        this.clusters = clusters;
    }

    public static class KafkaClusterProperties {

        private List<String> bootstrapServers = new ArrayList<>();
        private String clientId;
        private ProducerProperties producer = new ProducerProperties();

        public List<String> getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(List<String> bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public ProducerProperties getProducer() {
            return producer;
        }

        public void setProducer(ProducerProperties producer) {
            this.producer = producer;
        }

        public Map<String, Object> buildProducerProperties() {
            Map<String, Object> config = new LinkedHashMap<>();
            if (!bootstrapServers.isEmpty()) {
                config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            }
            if (clientId != null && !clientId.isBlank()) {
                config.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
            }
            producer.applyTo(config);
            return config;
        }
    }

    public static class ProducerProperties {

        private String acks;
        private DataSize batchSize;
        private DataSize bufferMemory;
        private String compressionType;
        private Class<?> keySerializer = StringSerializer.class;
        private Class<?> valueSerializer = StringSerializer.class;
        private Integer retries;
        private String transactionIdPrefix;
        private Map<String, String> properties = new LinkedHashMap<>();

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public DataSize getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(DataSize batchSize) {
            this.batchSize = batchSize;
        }

        public DataSize getBufferMemory() {
            return bufferMemory;
        }

        public void setBufferMemory(DataSize bufferMemory) {
            this.bufferMemory = bufferMemory;
        }

        public String getCompressionType() {
            return compressionType;
        }

        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }

        public Class<?> getKeySerializer() {
            return keySerializer;
        }

        public void setKeySerializer(Class<?> keySerializer) {
            this.keySerializer = keySerializer;
        }

        public Class<?> getValueSerializer() {
            return valueSerializer;
        }

        public void setValueSerializer(Class<?> valueSerializer) {
            this.valueSerializer = valueSerializer;
        }

        public Integer getRetries() {
            return retries;
        }

        public void setRetries(Integer retries) {
            this.retries = retries;
        }

        public String getTransactionIdPrefix() {
            return transactionIdPrefix;
        }

        public void setTransactionIdPrefix(String transactionIdPrefix) {
            this.transactionIdPrefix = transactionIdPrefix;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        void applyTo(Map<String, Object> config) {
            if (acks != null && !acks.isBlank()) {
                config.put(ProducerConfig.ACKS_CONFIG, acks);
            }
            if (batchSize != null) {
                config.put(ProducerConfig.BATCH_SIZE_CONFIG, toIntBytes(batchSize));
            }
            if (bufferMemory != null) {
                config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory.toBytes());
            }
            if (compressionType != null && !compressionType.isBlank()) {
                config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
            }
            if (keySerializer != null) {
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
            }
            if (valueSerializer != null) {
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
            }
            if (retries != null) {
                config.put(ProducerConfig.RETRIES_CONFIG, retries);
            }
            if (transactionIdPrefix != null && !transactionIdPrefix.isBlank()) {
                config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix);
            }
            config.putAll(properties);
        }

        private static int toIntBytes(DataSize dataSize) {
            long bytes = dataSize.toBytes();
            return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
        }
    }
}
