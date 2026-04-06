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
                file.getAbsolutePath(),
                attr.creationTime(),
                attr.lastAccessTime(),
                attr.lastModifiedTime(),
                attr.size()
        );
    }

    public boolean isTextFile(File file) throws CrawlerException {
        Tika tika = new Tika();
        String type;
        try {
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

    public boolean canRead(File file) {
        try (FileReader _ = new FileReader(file)){
            return true;
        } catch (IOException e) {
            return false;
        }
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


            return new FileModel(fileAttributes, checksum, content.toString(), scanId, true);
        } catch (FileNotFoundException e) {
            // the file cannot be opened for reading
            return new FileModel(fileAttributes, null, null, scanId, false);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new CrawlerException(e.getMessage());
        }
    }
}
