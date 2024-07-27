package com.tibame.utils.basic;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ImageUtil {
    public static BufferedImage getBufferedImage(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    public static byte[] resizeImage(BufferedImage originalImage, Integer targetWidth, Integer targetHeight, float quality) throws IOException {
        if (targetWidth == null && targetHeight == null) {
            throw new IllegalArgumentException("長寬皆無設定, 無法進行圖片縮放");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 如果只提供一個目標尺寸，維持圖片比例縮放
        if (targetWidth == null) {
            targetWidth = originalWidth * targetHeight / originalHeight;
        } else if (targetHeight == null) {
            targetHeight = originalHeight * targetWidth / originalWidth;
        }

        // 縮放圖片
        // 計算縮放的比例
        double scale = Math.max((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);

        // 計算縮放圖片的寬高
        int scaledWidth = (int) Math.ceil(scale * originalWidth);
        int scaledHeight = (int) Math.ceil(scale * originalHeight);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        // 裁切圖片
        int x = (scaledWidth - targetWidth) / 2;
        int y = (scaledHeight - targetHeight) / 2;
        BufferedImage croppedImage = scaledImage.getSubimage(x, y, targetWidth, targetHeight);

        // 將裁切後的圖片轉換成 JPEG 格式並設定壓縮率
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No writers found for format 'jpg'");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new javax.imageio.IIOImage(croppedImage, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
