package org.alien4cloud.tosca.variable;

import alien4cloud.utils.FileUtil;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import org.alien4cloud.tosca.utils.YamlParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Component
public class QuickFileStorageService {

    private Path variablesStoreRootPath;
    private Path inputsStoreRootPath;

    @Required
    @Value("${directories.alien}")
    public void setStorageRootPath(String variableRootDir) throws IOException {
        this.variablesStoreRootPath = FileUtil.createDirectoryIfNotExists(variableRootDir + "/variables/");
        this.inputsStoreRootPath = FileUtil.createDirectoryIfNotExists(variableRootDir + "/inputs/");
    }

    public Map<String, Object> loadInputsMappingFile(String environmentId, String topologyId) {
        Path ymlPath = this.inputsStoreRootPath.resolve(sanitizeFilename("inputs_" + environmentId + "_" + topologyId + ".yml"));
        return loadYamlToMapIfExists(ymlPath);
    }

    public Properties loadApplicationVariables(String applicationId) {
        Path ymlPath = this.variablesStoreRootPath.resolve(sanitizeFilename("app_" + applicationId + ".yml"));
        return loadYamlToPropertiesIfExists(ymlPath);
    }

    @SneakyThrows
    private Properties loadYamlToPropertiesIfExists(Path ymlPath) {
        Properties props;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            props = YamlParser.ToProperties.from(appVar);
        } else {
            Files.createFile(ymlPath);
            props = new Properties();
        }

        return props;
    }

    @SneakyThrows
    private Map<String, Object> loadYamlToMapIfExists(Path ymlPath) {
        Map<String, Object> map;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            map = YamlParser.ToMap.from(appVar);
        } else {
            Files.createFile(ymlPath);
            map = Maps.newHashMap();
        }

        return map;
    }

    public Properties loadEnvironmentVariables(String environmentId) {
        Path ymlPath = this.variablesStoreRootPath.resolve(sanitizeFilename("env_" + environmentId + ".yml"));
        return loadYamlToPropertiesIfExists(ymlPath);
    }

    public void saveInputMappingFile(String environmentId, String topologyId, InputStream inputsMappingFile) {
        Path ymlPath = this.inputsStoreRootPath.resolve(sanitizeFilename("inputs_" + environmentId + "_" + topologyId + ".yml"));
        save(ymlPath, inputsMappingFile);
    }

    public void saveApplicationVariables(String applicationId, InputStream content) {
        Path ymlPath = this.variablesStoreRootPath.resolve(sanitizeFilename("app_" + applicationId + ".yml"));
        save(ymlPath, content);
    }

    public void saveEnvironmentVariables(String environmentId, InputStream content) {
        Path ymlPath = this.variablesStoreRootPath.resolve(sanitizeFilename("env_" + environmentId + ".yml"));
        save(ymlPath, content);
    }

    @SneakyThrows
    private void save(Path path, String content) {
        if (Files.exists(path)) {
            FileUtils.writeStringToFile(path.toFile(), content, "UTF-8");
        }
    }

    @SneakyThrows
    private void save(Path path, InputStream content) {
        save(path, IOUtils.toString(content, "UTF-8"));
    }

    private String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
