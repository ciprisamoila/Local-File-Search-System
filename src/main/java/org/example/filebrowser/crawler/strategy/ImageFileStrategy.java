package org.example.filebrowser.crawler.strategy;

import org.example.filebrowser.model.ImageColor;
import org.example.filebrowser.model.index.FileAttributes;
import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.ImageFileModel;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageFileStrategy implements ContentInspectStrategy {

    private void increment(Map<ImageColor, Integer> map, ImageColor color) {
        if (map.containsKey(color)) {
            map.put(color, map.get(color) + 1);
        } else {
            map.put(color, 1);
        }
    }

    //TODO: sa testez pe folderul cu screenshot uri

    @Override
    public ImageFileModel getSpecificFileModel(FileModel fileModel) throws CrawlerException {
        if (!fileModel.isReadAccess()) {
            return new ImageFileModel(fileModel, null);
        }

        ImageColor imageColor = ImageColor.WHITE;
        Map<ImageColor, Integer> hist = new HashMap<>();

        try {
            BufferedImage image = ImageIO.read(new File(fileModel.getFileAttributes().path()));

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    Color c = new Color(image.getRGB(x, y));

                    ImageColor specificColor;

                    // 1 -> hue
                    // 2 -> saturation
                    // 3 -> brightness
                    float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);

                    if (hsb[1] < 0.1 && hsb[2] > 0.9) {
                        specificColor = ImageColor.WHITE;
                    } else if (hsb[2] < 0.1) {
                        specificColor = ImageColor.BLACK;
                    } else {
                        float degrees = hsb[0] * 360;
                        if      (degrees >=   0 && degrees <  30) specificColor = ImageColor.RED;
                        else if (degrees >=  30 && degrees <  90) specificColor = ImageColor.YELLOW;
                        else if (degrees >=  90 && degrees < 150) specificColor = ImageColor.GREEN;
                        else if (degrees >= 150 && degrees < 210) specificColor = ImageColor.CYAN;
                        else if (degrees >= 210 && degrees < 270) specificColor = ImageColor.BLUE;
                        else if (degrees >= 270 && degrees < 330) specificColor = ImageColor.MAGENTA;
                        else specificColor = ImageColor.RED;
                    }

                    increment(hist, specificColor);
                }
            }

            int maxFrequency = 0;
            for (Map.Entry<ImageColor, Integer> entry : hist.entrySet()) {
                if (entry.getValue() > maxFrequency) {
                    maxFrequency = entry.getValue();
                    imageColor = entry.getKey();
                }
            }

        } catch (IOException e) {
            throw new CrawlerException(e.getMessage());
        }

        return new ImageFileModel(fileModel, imageColor.toString());
    }
}
