package ch.autotyper.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates Java files in the correct package directory structure within the project
 * and opens them in the editor.
 */
public class FileCreationService {

    private static final Logger LOG = Logger.getInstance(FileCreationService.class);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(?:public\\s+)?(?:class|interface|enum|record)\\s+(\\w+)");

    private final Project project;

    public FileCreationService(Project project) {
        this.project = project;
    }

    /**
     * Extracts the package name from source code (ignoring @step markers).
     */
    public static String extractPackageName(String sourceCode) {
        // Remove step markers to find the real package declaration
        String cleaned = sourceCode.replaceAll("//\\s*@step\\s+\\d+\\s*\\n?", "");
        Matcher matcher = PACKAGE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null; // default package
    }

    /**
     * Extracts the class name from source code.
     */
    public static String extractClassName(String sourceCode) {
        String cleaned = sourceCode.replaceAll("//\\s*@step\\s+\\d+\\s*\\n?", "");
        Matcher matcher = CLASS_NAME_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts class name from filename (fallback).
     */
    public static String classNameFromFileName(String fileName) {
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    /**
     * Creates the Java file in the project's source directory with proper package structure,
     * and opens it in the editor.
     *
     * @param snippetFileName The original snippet filename (e.g. "Calculator.java")
     * @param sourceCode The full source code (with @step markers)
     * @return true if file was created and opened successfully
     */
    public boolean createAndOpenFile(String snippetFileName, String sourceCode) {
        String packageName = extractPackageName(sourceCode);
        String className = extractClassName(sourceCode);
        if (className == null) {
            className = classNameFromFileName(snippetFileName);
        }

        // Determine source root
        Path sourceRoot = findSourceRoot();
        if (sourceRoot == null) {
            LOG.warn("Could not determine source root, using project base path");
            sourceRoot = Paths.get(project.getBasePath());
        }

        // Build target directory path
        Path targetDir = sourceRoot;
        if (packageName != null && !packageName.isEmpty()) {
            String packagePath = packageName.replace('.', '/');
            targetDir = sourceRoot.resolve(packagePath);
        }

        // Create directories
        Path finalTargetDir = targetDir;
        String finalClassName = className;

        try {
            Files.createDirectories(finalTargetDir);
        } catch (IOException e) {
            LOG.error("Failed to create package directories: " + finalTargetDir, e);
            return false;
        }

        // Create the empty Java file
        Path filePath = finalTargetDir.resolve(finalClassName + ".java");
        try {
            if (Files.exists(filePath)) {
                // Overwrite with empty content
                Files.writeString(filePath, "");
            } else {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            LOG.error("Failed to create file: " + filePath, e);
            return false;
        }

        // Refresh VFS and open file in editor
        Path finalFilePath = filePath;
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(finalFilePath);
            if (vFile != null) {
                vFile.refresh(false, false);
                FileEditorManager.getInstance(project).openFile(
                        vFile, true);
            } else {
                LOG.error("Could not find virtual file after creation: " + finalFilePath);
            }
        });

        LOG.info("Created and opened: " + filePath);
        return true;
    }

    /**
     * Finds the source root directory of the project.
     * Tries common conventions: src/main/java, src, or project root.
     */
    private Path findSourceRoot() {
        String basePath = project.getBasePath();
        if (basePath == null) return null;

        Path base = Paths.get(basePath);

        // Try common source roots in order of preference
        Path[] candidates = {
                base.resolve("src/main/java"),
                base.resolve("src"),
                base
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        return base;
    }
}
