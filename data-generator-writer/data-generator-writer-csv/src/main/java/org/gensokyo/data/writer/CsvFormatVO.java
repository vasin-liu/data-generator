/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.writer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.apache.commons.csv.QuoteMode;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.ArrayKit;

import java.util.Objects;

/**
 * CSV格式配置
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/8/26 , Version 1.0.0
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CsvFormatVO {

    private boolean allowMissingColumnNames;

    private boolean autoFlush;

    private Character commentMarker;

    private String delimiter;

    private DuplicateHeaderMode duplicateHeaderMode;

    private Character escapeCharacter;

    private String[] headerComments;

    private String[] headers;

    private boolean ignoreEmptyLines;

    private boolean ignoreHeaderCase;

    private boolean ignoreSurroundingSpaces;

    private String nullString;

    private Character quoteCharacter;

    private String quotedNullString;

    private QuoteMode quoteMode;

    private String recordSeparator;

    private boolean skipHeaderRecord;

    private boolean lenientEof;

    private boolean trailingData;

    private boolean trailingDelimiter;

    private boolean trim;

    CSVFormat copyTo(CSVFormat.Builder builder) {
        builder.setAllowMissingColumnNames(this.isAllowMissingColumnNames());
        builder.setAutoFlush(this.isAutoFlush());
        if (Objects.nonNull(this.getCommentMarker())) {
            builder.setCommentMarker(this.getCommentMarker());
        }
        if (StrKit.isNotBlank(this.getDelimiter())) {
            builder.setDelimiter(this.getDelimiter());
        }
        if (Objects.nonNull(this.getDuplicateHeaderMode())) {
            builder.setDuplicateHeaderMode(this.getDuplicateHeaderMode());
        }
        if (Objects.nonNull(this.getEscapeCharacter())) {
            builder.setEscape(this.getEscapeCharacter());
        }
        if (ArrayKit.isNotEmpty(this.getHeaderComments())) {
            builder.setHeaderComments(this.getHeaderComments());
        }
        if (ArrayKit.isNotEmpty(this.getHeaders())) {
            builder.setHeader(this.getHeaders());
        } else {
            if (this.isSkipHeaderRecord()) {
                builder.setHeader();
            }
        }
        builder.setIgnoreEmptyLines(this.isIgnoreEmptyLines());
        builder.setIgnoreHeaderCase(this.isIgnoreHeaderCase());
        builder.setIgnoreSurroundingSpaces(this.isIgnoreSurroundingSpaces());
        if (Objects.nonNull(this.getNullString())) {
            builder.setNullString(this.getNullString());
        }
        if (Objects.nonNull(this.getQuoteCharacter())) {
            builder.setQuote(this.getQuoteCharacter());
        }
        if (Objects.nonNull(this.getQuoteMode())) {
            builder.setQuoteMode(this.getQuoteMode());
        }
        if (Objects.nonNull(this.getRecordSeparator())) {
            builder.setRecordSeparator(this.getRecordSeparator());
        }
        builder.setSkipHeaderRecord(this.isSkipHeaderRecord());
        builder.setLenientEof(this.isLenientEof());
        builder.setTrailingData(this.isTrailingData());
        builder.setTrailingDelimiter(this.isTrailingDelimiter());
        builder.setTrim(this.isTrim());
        return builder.build();
    }
}
