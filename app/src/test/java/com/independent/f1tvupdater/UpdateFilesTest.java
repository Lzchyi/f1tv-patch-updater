package com.independent.f1tvupdater;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class UpdateFilesTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void interruptedFilesAreRemovedWhilePendingBundleIsKept() throws Exception {
        File files = temp.newFolder("files"), cache = temp.newFolder("cache");
        File pending = new File(files, "candidate.apkm");
        Files.write(pending.toPath(), new byte[] {1, 2, 3});
        File partial = new File(files, "downloading.apkm");
        Files.write(partial.toPath(), new byte[] {4});
        File extraction = new File(cache, "apkm_extract_123");
        assertTrue(extraction.mkdir());
        Files.write(new File(extraction, "base.apk").toPath(), new byte[] {5});
        UpdateFiles.cleanup(files, cache, false);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(pending.toPath()));
        assertFalse(partial.exists());
        assertFalse(extraction.exists());
    }
    @Test public void obsoleteBundleAndExtractionAreRemoved() throws Exception {
        File files = temp.newFolder("files"), cache = temp.newFolder("cache");
        File pending = new File(files, "candidate.apkm");
        Files.write(pending.toPath(), new byte[] {1});
        File extraction = new File(cache, "apkm_extract_-123");
        assertTrue(extraction.mkdir());
        UpdateFiles.cleanup(files, cache, true);
        assertFalse(pending.exists());
        assertFalse(extraction.exists());
        UpdateFiles.cleanup(files, cache, true); // Already-clean storage is harmless.
    }
    @Test public void unrelatedAppFilesAreNeverSwept() throws Exception {
        File files = temp.newFolder("files"), cache = temp.newFolder("cache");
        File unrelated = new File(files, "important.apkm");
        Files.write(unrelated.toPath(), new byte[] {7});
        File notes = new File(cache, "apkm_extract_notes");
        assertTrue(notes.mkdir());
        UpdateFiles.cleanup(files, cache, true);
        assertTrue(unrelated.exists());
        assertTrue(notes.exists());
    }
    @Test public void extractionSymlinkDoesNotDeleteItsTarget() throws Exception {
        File files = temp.newFolder("files"), cache = temp.newFolder("cache");
        File outside = temp.newFolder("outside");
        File valuable = new File(outside, "keep.txt");
        Files.write(valuable.toPath(), new byte[] {9});
        File link = new File(cache, "apkm_extract_456");
        Files.createSymbolicLink(link.toPath(), outside.toPath());
        UpdateFiles.cleanup(files, cache, true);
        assertFalse(Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(valuable.toPath()));
    }
}
