/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console.dto;

/**
 * Persists pasted text as a server-side file for file-based sources.
 *
 * @param filename suggested file name (e.g. {@code data.json})
 * @param content  raw file body
 * @author Gensokyo
 * @since 2026-06-03
 */
public record InlineUploadRequest(String filename, String content) {
}
