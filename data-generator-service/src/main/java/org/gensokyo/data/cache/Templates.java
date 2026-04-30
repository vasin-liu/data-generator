/*
 * Copyright 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 */
package org.gensokyo.data.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.json.TemplateJsonCodec;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.TemplateDefinitionDetector;
import org.gensokyo.data.template.TemplateDefinitionKind;
import org.gensokyo.data.util.RandomKit;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;
import org.gensokyo.kit.io.FileKit;
import org.gensokyo.kit.security.Md5Kit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class Templates implements InitializingBean {
    private final DataGeneratorProperties props;
    private final YamlParser yamlParser;
    private final TemplateRepository repository;

    public List<TemplatePO> reloadAll() {
        repository.deleteAll();
        return parse(loadTemplateResources());
    }

    public FileAlterationObserver createResourceObserver() {
        String baseDir;
        if (isRunningFromJar()) {
            baseDir = Paths.get(System.getProperty("user.dir"), "..", "conf", "template").toString();
        } else {
            var url = Templates.class.getClassLoader().getResource("template");
            baseDir = Objects.requireNonNull(url).getPath();
        }
        FileAlterationObserver observer = new FileAlterationObserver(baseDir);
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                if (!isYamlFile(file)) {
                    return;
                }
                var template = parse(file);
                if (Objects.isNull(template)) {
                    return;
                }
                repository.saveAndFlush(template);
                log.info("Template cache entry created for {}", file.getAbsolutePath());
            }

            @Override
            public void onFileChange(File file) {
                if (!isYamlFile(file)) {
                    return;
                }
                var template = parse(file);
                if (Objects.isNull(template)) {
                    return;
                }
                var templateInDb = repository.findByPathMd5(template.getPathMd5());
                if (Objects.nonNull(templateInDb)) {
                    repository.delete(templateInDb);
                }
                repository.saveAndFlush(template);
                log.info("Template cache entry updated for {}", file.getAbsolutePath());
            }

            @Override
            public void onFileDelete(File file) {
                var templateInDb = repository.findByPathMd5(Md5Kit.encrypt(file.getAbsolutePath()));
                if (Objects.nonNull(templateInDb)) {
                    repository.delete(templateInDb);
                    log.info("Template cache entry deleted for {}", file.getAbsolutePath());
                }
            }

            @Override
            public void onDirectoryCreate(File directory) {
                var resources = fromFileSystem(directory.toPath());
                if (CollectKit.isEmpty(resources)) {
                    return;
                }
                var pos = parse(resources);
                if (CollectKit.isEmpty(pos)) {
                    return;
                }
                repository.deleteAllInBatch(pos);
                repository.saveAllAndFlush(pos);
                log.info("Template cache entries created for directory {}", directory.getAbsolutePath());
            }

            @Override
            public void onDirectoryDelete(File directory) {
                log.info("Template directory deleted: {}", directory.getAbsolutePath());
            }
        });
        return observer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        repository.saveAll(reloadAll());
        log.info("Template cache initialized");
        var monitor = new FileAlterationMonitor(1000, createResourceObserver());
        monitor.start();
        log.info("Template file monitor started");
    }

    public List<TemplatePO> parse(List<Resource> resources) {
        if (CollectKit.isEmpty(resources)) {
            return Collections.emptyList();
        }
        var parsed = new ArrayList<TemplatePO>();
        var skipped = new ArrayList<String>();
        var ignored = new ArrayList<String>();
        resources.stream()
                .filter(r -> {
                    boolean matched = Arrays.stream(props.getIgnorePrefix())
                            .anyMatch(prefix -> Objects.requireNonNull(r.getFilename()).startsWith(prefix));
                    if (matched) {
                        ignored.add(Objects.requireNonNullElse(r.getFilename(), describe(r)));
                        return false;
                    }
                    return true;
                })
                .filter(r -> {
                    try {
                        return isYamlFile(r.getFile());
                    } catch (IOException ex) {
                    }
                    return false;
                })
                .forEach(resource -> {
                    var template = parse(resource, false);
                    if (Objects.nonNull(template)) {
                        parsed.add(template);
                    } else {
                        skipped.add(describe(resource));
                    }
                });
        if (!ignored.isEmpty()) {
            log.info("Ignored {} template files by prefix: {}", ignored.size(), ignored);
        }
        if (!skipped.isEmpty()) {
            log.warn("Skipped {} template files that are incompatible with the current parser: {}", skipped.size(), abbreviate(skipped));
        }
        return parsed;
    }

    private TemplatePO parse(Resource resource) {
        return parse(resource, true);
    }

    private TemplatePO parse(Resource resource, boolean verbose) {
        try {
            return parse(resource.getFile(), verbose);
        } catch (IOException e) {
            if (verbose) {
                log.warn("Skip template resource {} because it cannot be opened: {}", describe(resource), sanitize(e.getMessage()));
            }
        }
        return null;
    }

    private TemplatePO parse(File file) {
        return parse(file, true);
    }

    private TemplatePO parse(File file, boolean verbose) {
        try {
            var id = RandomKit.snowFlake().nextId();
            var fileName = file.getName();
            var yamlContent = Files.readString(file.toPath());
            var template = tryParse(yamlContent, TemplateVO.class);
            var templateV2 = tryParse(yamlContent, TemplateV2DraftVO.class);
            var kind = TemplateDefinitionDetector.detect(template, templateV2);
            var entity = new TemplatePO();
            entity.setId(id);
            if (kind == TemplateDefinitionKind.V2 && Objects.nonNull(templateV2)) {
                entity.setName(templateV2.getName());
                entity.setContentJson(TemplateJsonCodec.write(templateV2));
            } else if (Objects.nonNull(template)) {
                template.setId(id);
                entity.setName(template.getName());
                entity.setContentJson(TemplateJsonCodec.write(template));
            } else {
                throw new IllegalArgumentException("Template content is neither valid V1 nor valid V2");
            }
            entity.setFileName(fileName);
            entity.setFileExt(FileKit.getExtension(fileName));
            entity.setPathMd5(Md5Kit.encrypt(file.getPath()));
            entity.setContentMd5(Md5Kit.encrypt(yamlContent));
            entity.setContentYaml(yamlContent);
            return entity;
        } catch (Exception e) {
            if (verbose) {
                log.warn("Skip template file {} because parsing failed: {}", file.getAbsolutePath(), summarize(e));
            }
        }
        return null;
    }

    private <T> T tryParse(String yamlContent, Class<T> clazz) {
        try {
            return yamlParser.parse(yamlContent, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String describe(Resource resource) {
        if (Objects.isNull(resource)) {
            return "<unknown>";
        }
        try {
            return resource.getFile().getAbsolutePath();
        } catch (IOException ignored) {
            return Objects.toString(resource.getDescription(), resource.getFilename());
        }
    }

    private String summarize(Exception exception) {
        var root = exception;
        while (root.getCause() instanceof Exception cause) {
            root = cause;
        }
        var message = root.getMessage();
        if (StrKit.isBlank(message)) {
            return root.getClass().getSimpleName();
        }
        return sanitize(message);
    }

    private String sanitize(String message) {
        if (StrKit.isBlank(message)) {
            return message;
        }
        return message.replaceAll("\\s+", " ").trim();
    }

    private String abbreviate(List<String> values) {
        if (CollectKit.isEmpty(values)) {
            return "[]";
        }
        int limit = Math.min(values.size(), 5);
        var sample = values.subList(0, limit);
        if (values.size() == limit) {
            return sample.toString();
        }
        return sample + " ...";
    }

    private List<Resource> fromFileSystem(String baseDir) {
        if (StrKit.isBlank(baseDir)) {
            return Collections.emptyList();
        }
        var basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        return fromFileSystem(basePath);
    }

    private List<Resource> fromFileSystem(Path basePath) {
        var resourceList = new ArrayList<Resource>();
        try {
            if (Objects.isNull(basePath) || !Files.exists(basePath) || !Files.isDirectory(basePath)) {
                return resourceList;
            }
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    resourceList.add(new FileSystemResource(file.toFile()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.error("Failed to read template resources from {}", basePath, e);
        }
        return resourceList;
    }

    private List<Resource> fromClasspath(String baseDir) {
        if (StrKit.isBlank(baseDir)) {
            return Collections.emptyList();
        }
        var resourceList = new ArrayList<Resource>();
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("classpath:" + baseDir);
            for (var resource : resources) {
                if (resource.isReadable()) {
                    resourceList.add(resource);
                }
            }
        } catch (Exception e) {
            log.error("Failed to read template resources from classpath: {}", baseDir, e);
        }
        return resourceList;
    }

    private List<Resource> loadTemplateResources() {
        if (isRunningFromJar()) {
            String baseDir = Paths.get(System.getProperty("user.dir"), "..", "conf", "template").toString();
            return fromFileSystem(baseDir);
        }
        return fromClasspath("/template/**/*.yaml");
    }

    private boolean isRunningFromJar() {
        var location = Templates.class.getProtectionDomain().getCodeSource().getLocation();
        return Objects.nonNull(location) && location.getPath().endsWith(".jar");
    }

    private boolean isYamlFile(File file) {
        if (Objects.isNull(file) || !file.exists() || !file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
