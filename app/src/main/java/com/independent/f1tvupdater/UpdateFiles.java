package com.independent.f1tvupdater;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Only updater-owned staging files; walking never follows symbolic links. */
final class UpdateFiles {
    static void cleanup(File files, File cache, boolean removeCandidate) throws IOException {
        Files.deleteIfExists(new File(files, "downloading.apkm").toPath());
        if (removeCandidate) Files.deleteIfExists(new File(files, "candidate.apkm").toPath());
        File[] directories = cache.listFiles();
        if (directories != null) for (File directory : directories) {
            if (directory.getName().matches("apkm_extract_-?[0-9]+")) removeTree(directory);
        }
    }

    static void removeTree(File file) throws IOException {
        if (!Files.exists(file.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
