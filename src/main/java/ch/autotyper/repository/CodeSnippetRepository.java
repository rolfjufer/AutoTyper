package ch.autotyper.repository;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages loading and caching of code snippet files from a configurable directory.
 */
@Service(Service.Level.APP)
public final class CodeSnippetRepository {

    private static final Logger LOG = Logger.getInstance(CodeSnippetRepository.class);
    private static final String DEFAULT_DIR_NAME = "autotyper-snippets";

    private Path snippetsDirectory;
    private final Map<String, String> snippetCache = new LinkedHashMap<>();

    public CodeSnippetRepository() {
        this.snippetsDirectory = Paths.get(System.getProperty("user.home"), DEFAULT_DIR_NAME);
        ensureDirectoryExists();
        reload();
    }

    public static CodeSnippetRepository getInstance() {
        return ApplicationManager.getApplication().getService(CodeSnippetRepository.class);
    }

    /**
     * Returns all loaded snippet names (filenames).
     */
    public List<String> getSnippetNames() {
        return new ArrayList<>(snippetCache.keySet());
    }

    /**
     * Returns the content of a snippet by name.
     */
    public String getSnippetContent(String name) {
        return snippetCache.getOrDefault(name, "");
    }

    /**
     * Returns the current snippets directory path.
     */
    public Path getSnippetsDirectory() {
        return snippetsDirectory;
    }

    /**
     * Changes the snippets directory and reloads.
     */
    public void setSnippetsDirectory(Path directory) {
        this.snippetsDirectory = directory;
        reload();
    }

    /**
     * Reloads all snippets from the configured directory.
     */
    public void reload() {
        snippetCache.clear();

        if (!Files.isDirectory(snippetsDirectory)) {
            LOG.warn("Snippets directory does not exist: " + snippetsDirectory);
            return;
        }

        try (Stream<Path> paths = Files.list(snippetsDirectory)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .sorted()
                 .forEach(this::loadFile);
        } catch (IOException e) {
            LOG.error("Failed to list snippets directory", e);
        }

        LOG.info("Loaded " + snippetCache.size() + " snippet(s) from " + snippetsDirectory);
    }

    private void loadFile(Path path) {
        try {
            String content = Files.readString(path);
            String name = path.getFileName().toString();
            snippetCache.put(name, content);
        } catch (IOException e) {
            LOG.error("Failed to read snippet file: " + path, e);
        }
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(snippetsDirectory)) {
                Files.createDirectories(snippetsDirectory);
                LOG.info("Created snippets directory: " + snippetsDirectory);
            }
        } catch (IOException e) {
            LOG.error("Failed to create snippets directory", e);
        }
    }
}
