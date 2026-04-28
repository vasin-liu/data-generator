package org.gensokyo.data.elasticsearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.elasticsearch.multiple")
public class MultipleElasticsearchClusterProperties {

    private String primary;
    private Map<String, ElasticsearchClusterProperties> clusters = new LinkedHashMap<>();

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public Map<String, ElasticsearchClusterProperties> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, ElasticsearchClusterProperties> clusters) {
        this.clusters = clusters;
    }

    public static class ElasticsearchClusterProperties {

        private List<String> uris = new ArrayList<>();
        private String username;
        private String password;
        private String apiKey;
        private Duration connectionTimeout;
        private Duration socketTimeout;
        private boolean socketKeepAlive;
        private String pathPrefix;
        private Restclient restclient = new Restclient();

        public List<String> getUris() {
            return uris;
        }

        public void setUris(List<String> uris) {
            this.uris = uris;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Duration getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public boolean isSocketKeepAlive() {
            return socketKeepAlive;
        }

        public void setSocketKeepAlive(boolean socketKeepAlive) {
            this.socketKeepAlive = socketKeepAlive;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public Restclient getRestclient() {
            return restclient;
        }

        public void setRestclient(Restclient restclient) {
            this.restclient = restclient;
        }
    }

    public static class Restclient {
        private Sniffer sniffer = new Sniffer();

        public Sniffer getSniffer() {
            return sniffer;
        }

        public void setSniffer(Sniffer sniffer) {
            this.sniffer = sniffer;
        }
    }

    public static class Sniffer {
        private Duration interval;
        private Duration delayAfterFailure;

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getDelayAfterFailure() {
            return delayAfterFailure;
        }

        public void setDelayAfterFailure(Duration delayAfterFailure) {
            this.delayAfterFailure = delayAfterFailure;
        }
    }
}
