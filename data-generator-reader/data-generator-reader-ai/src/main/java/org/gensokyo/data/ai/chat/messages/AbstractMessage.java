/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.ai.chat.messages;

import org.gensokyo.kit.Assert;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 抽象消息类
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/4/3 , Version 1.0.0
 */
public abstract class AbstractMessage implements Message {

    protected final MessageType messageType;

    protected final String textContent;

    protected final List<Media> mediaData;

    protected final Map<String, Object> properties;

    protected AbstractMessage(MessageType messageType, String content) {
        this(messageType, content, Map.of());
    }

    protected AbstractMessage(MessageType messageType, String content, Map<String, Object> messageProperties) {
        Assert.notNull(messageType, "消息类型不能为空");
        this.messageType = messageType;
        this.textContent = content;
        this.mediaData = new ArrayList<>();
        this.properties = messageProperties;
    }

    protected AbstractMessage(MessageType messageType, String textContent, List<Media> mediaData) {
        this(messageType, textContent, mediaData, Map.of());
    }

    protected AbstractMessage(MessageType messageType, String textContent, List<Media> mediaData,
                              Map<String, Object> messageProperties) {

        Assert.notNull(messageType, "消息类型不能为空");
        Assert.notNull(textContent, "文本内容不能为空");
        Assert.notNull(mediaData, "媒体类型不能为空");

        this.messageType = messageType;
        this.textContent = textContent;
        this.mediaData = new ArrayList<>(mediaData);
        this.properties = messageProperties;
    }

    protected AbstractMessage(MessageType messageType, Resource resource) {
        this(messageType, resource, Collections.emptyMap());
    }

    @SuppressWarnings("null")
    protected AbstractMessage(MessageType messageType, Resource resource, Map<String, Object> messageProperties) {
        Assert.notNull(messageType, "消息类型不能为空");
        Assert.notNull(resource, "资源文件不能为空");

        this.messageType = messageType;
        this.properties = messageProperties;
        this.mediaData = new ArrayList<>();

        try (InputStream inputStream = resource.getInputStream()) {
            this.textContent = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
        } catch (IOException ex) {
            throw new RuntimeException("读取资源失败", ex);
        }
    }

    @Override
    public String getContent() {
        return this.textContent;
    }

    @Override
    public List<Media> getMedia() {
        return this.mediaData;
    }

    @Override
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public MessageType getMessageType() {
        return this.messageType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mediaData == null) ? 0 : mediaData.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((messageType == null) ? 0 : messageType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractMessage other = (AbstractMessage) obj;
        if (mediaData == null) {
            if (other.mediaData != null) {
                return false;
            }
        } else if (!mediaData.equals(other.mediaData)) {
            return false;
        }
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        return messageType == other.messageType;
    }

}
