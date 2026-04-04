package org.example.filebrowser.crawler;

import java.nio.file.attribute.FileTime;

public class FileChecker {

    public boolean modifiedTimeHasBeenModified(FileTime lastRecordedModifiedTime, FileTime realModifiedTime) {
        return lastRecordedModifiedTime.toMillis() < realModifiedTime.toMillis();
    }

    public boolean checksumHasBeenModified(String lastRecordedChecksum, String realChecksum) {
        return !lastRecordedChecksum.equals(realChecksum);
    }

    public boolean readingRightsHaveBeenModified(boolean hadRightsToRead, boolean haveRightsToRead) {
        return hadRightsToRead != haveRightsToRead;
    }
}
