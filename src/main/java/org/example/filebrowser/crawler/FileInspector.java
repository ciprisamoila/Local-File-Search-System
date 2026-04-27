package org.example.filebrowser.crawler;

import org.apache.tika.Tika;
import org.example.filebrowser.model.FileAttributes;
import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.max;

public class FileInspector {
    Logger logger = Logger.getLogger("crawler");

    private BasicFileAttributes getBasicAttributes(File file) throws CrawlerException {
        Path path = file.toPath();
        BasicFileAttributes attr;

        try {
            attr = Files.readAttributes(path, BasicFileAttributes.class);

            return attr;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());

            throw new CrawlerException(e.getMessage());
        }
    }


    public boolean isSymbolicLink(File file){

        return Files.isSymbolicLink(file.toPath());
    }

    public FileAttributes getFileAttributes(File file) throws CrawlerException {
        BasicFileAttributes attr = getBasicAttributes(file);

        // decompose file name into name without extension and extension
        String fileName = file.getName();
        String extension = "";
        int index = fileName.length() - 1;
        while (index >= 0 && fileName.charAt(index) != '.') {
            index--;
        }
        if (index >= 0) {
            extension = fileName.substring(index + 1);
            fileName = fileName.substring(0, index);
        }

        return new FileAttributes(
                fileName,
                extension,
                file.getAbsolutePath().replace('\\', '/'), // all paths UNIX-like
                attr.creationTime(),
                attr.lastAccessTime(),
                attr.lastModifiedTime(),
                attr.size()
        );
    }

    public boolean isTextFile(File file) throws CrawlerException {
        Path path = file.toPath();
        String mimeType;
        Tika tika = new Tika();
        String type;
        try {
            mimeType = Files.probeContentType(path);

            // first stage -> mime type based
            if (mimeType != null && mimeType.startsWith("text"))
                return true;

            // second stage -> content based
            type = tika.detect(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
            throw new CrawlerException(e.getMessage());
        }

        return type.startsWith("text");
    }

    public boolean verifiesConfig(FileAttributes attr, CrawlConfig config) {
        if (attr.size() > config.maxFileSize())
            return false;

        for (String ext : config.fileTypes())
            if (ext.equals(attr.extension()))
                return true;

        return false;

    }

    String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // gives to each file a score from 0 to 1
    // takes into consideration the following: path length, directory importance, last access time, size, read_access
    // for each category computes a score from 0 to 1, then sums them with specific weights
    public double scoreFile(FileAttributes attr, boolean readAccess) {
        // path length score (by the number of subdirectories)
        String path = attr.path();
        int length = path.length() - path.replace("/", "").length();
        double score_pl = 1.0 / length;
        // directory importance Windows only
        // Recycle Bin, Downloads -> 0 worst importance
        // Program Files, ProgramData, Windows -> 0.2 less importance
        // Desktop, Documents, Pictures, Music, Video -> 1 best importance
        // others -> 0.6 medium importance
        double score_di = 0.6;
        if (path.contains("Recycle Bin") || path.contains("Downloads")) {
            score_di = 0;
        } else if (path.contains("Program Files") || path.contains("Windows") || path.contains("ProgramData")) {
            score_di = 0.2;
        } else if (path.contains("Desktop") || path.contains("Documents") || path.contains("Pictures") || path.contains("Music") || path.contains("Video")) {
            score_di = 1;
        }
        // last access time
        // difference in days
        long delta = (System.currentTimeMillis() - attr.lastAccessedTime().toMillis()) / (1000 * 60 * 60 * 24);
        int days_limit = 3650;
        double score_lat = max(1 - (double) delta /  days_limit, 0);
        // size
        long size_limit = 1_000_000;
        double score_s = max(1 - (double) attr.size() /  size_limit, 0);
        // read access
        double score_ra = readAccess ? 1 : 0;
        return 0.2 * score_pl + 0.2 * score_di + 0.2 * score_lat + 0.2 * score_s + 0.2 * score_ra;
    }

    public FileModel getFileModel(File file, FileAttributes fileAttributes, long scanId) throws CrawlerException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append('\n');
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.toString().getBytes(StandardCharsets.UTF_8));

            String checksum = bytesToHex(hashBytes);


            return new FileModel(fileAttributes, checksum, content.toString(), scanId, true, scoreFile(fileAttributes, true));
        } catch (FileNotFoundException e) {
            // the file cannot be opened for reading
            return new FileModel(fileAttributes, null, null, scanId, false, scoreFile(fileAttributes, false));
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new CrawlerException(e.getMessage());
        }
    }
}
