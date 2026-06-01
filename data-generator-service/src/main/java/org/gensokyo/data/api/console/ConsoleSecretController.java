/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.api.console;

import lombok.RequiredArgsConstructor;
import org.gensokyo.data.model.vo.R;
import org.gensokyo.data.secret.SecretService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Secret registry for {@code passwordSecretRef} resolution (Phase B).
 *
 * @author Gensokyo
 * @since 2026-05-29
 */
@RestController
@RequestMapping("/api/secrets")
@RequiredArgsConstructor
public class ConsoleSecretController {

    private final SecretService secretService;

    /**
     * @return secret names without values
     */
    @GetMapping
    public R<List<SecretService.SecretSummary>> list() {
        return R.ok(secretService.listSummaries());
    }

    /**
     * @param name  logical secret name
     * @param body  value and optional description
     * @return acknowledgement
     */
    @PutMapping("/{name}")
    public R<String> upsert(@PathVariable String name, @RequestBody SecretUpsertRequest body) {
        secretService.upsert(name, body.value(), body.description());
        return R.ok("Secret saved");
    }

    /**
     * @param name logical secret name
     * @return acknowledgement
     */
    @DeleteMapping("/{name}")
    public R<String> delete(@PathVariable String name) {
        secretService.delete(name);
        return R.ok("Secret deleted");
    }

    /**
     * Request body for secret upsert (value is never returned by list/get APIs).
     *
     * @param value       secret value
     * @param description optional note
     */
    public record SecretUpsertRequest(String value, String description) {
    }
}
