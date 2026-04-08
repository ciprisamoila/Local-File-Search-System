package org.example.filebrowser.crawler;

public class FileChecker {

    public boolean checksumHasBeenModified(String lastRecordedChecksum, String realChecksum) {
        // the file still has not read access
        if (lastRecordedChecksum == null && realChecksum == null)
            return false;

        // the file has read access and had before; we check for change in checksum
        if (lastRecordedChecksum != null && realChecksum != null)
            return !lastRecordedChecksum.equals(realChecksum);

        // file changed its access rights
        return true;
    }
}
