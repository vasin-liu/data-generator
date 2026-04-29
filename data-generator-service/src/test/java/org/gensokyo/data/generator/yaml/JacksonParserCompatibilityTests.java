package org.gensokyo.data.generator.yaml;

import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.yaml.JacksonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class JacksonParserCompatibilityTests {

    @Test
    void parsesBuiltInDemoTemplate() throws Exception {
        TemplateVO template = parseTemplate("template/demo", "00_");

        assertTemplateParsed(template);
        Assertions.assertEquals("demo_00", template.getName());
    }

    @Test
    void parsesDateTimeIteratorTemplate() throws Exception {
        assertTemplateParsed(parseTemplate("template/demo", "17_"));
    }

    @Test
    void parsesDatabaseIteratorTemplate() throws Exception {
        assertTemplateParsed(parseTemplate("template/demo", "18_"));
    }

    @Test
    void parsesPauseStageTemplate() throws Exception {
        assertTemplateParsed(parseTemplate("template/demo", "27_"));
    }

    @Test
    void parsesConstantIteratorRepeatTemplate() throws Exception {
        assertTemplateParsed(parseTemplate("template/demo", "28_"));
    }

    @Test
    void parsesTrafficCommandTemplate() throws Exception {
        assertTemplateParsed(parseTemplate("template/idps/traffic-command", "01_Q_USPP_WIT_DEVICE_LOCATION"));
    }

    private TemplateVO parseTemplate(String directory, String filePrefix) throws Exception {
        var parser = new JacksonParser();
        var resource = new ClassPathResource(directory);
        var basePath = resource.getFile().toPath();
        return parser.parse(resolveTemplate(basePath, filePrefix).toFile(), TemplateVO.class);
    }

    private Path resolveTemplate(Path directory, String filePrefix) throws Exception {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Template not found for prefix " + filePrefix + " under " + directory));
        }
    }

    private void assertTemplateParsed(TemplateVO template) {
        Assertions.assertNotNull(template);
        Assertions.assertNotNull(template.getIterator());
        Assertions.assertNotNull(template.getGenerator());
        Assertions.assertFalse(template.getFields().isEmpty());
        Assertions.assertNotNull(template.getOutput());
    }
}
