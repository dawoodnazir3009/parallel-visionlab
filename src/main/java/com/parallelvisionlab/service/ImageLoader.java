package com.parallelvisionlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/** Decodes images off the JavaFX application thread; owns and closes the input stream. */
public final class ImageLoader {
    public Image load(Path path) throws IOException {
        if(path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".dng"))
            throw new IOException("DNG is a camera RAW format. Export it as JPEG or PNG, then open the exported image.");
        if(Files.size(path)>256L*1024*1024)throw new IOException("Image file exceeds the 256 MB input limit.");
        try(var input=ImageIO.createImageInputStream(path.toFile())) {
            if(input==null)throw new IOException("Image is not readable.");
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw new IOException("Unsupported or corrupt image. Use PNG, JPEG, BMP or GIF.");
            var reader=readers.next();
            try {
                reader.setInput(input);validateDimensions(reader.getWidth(0),reader.getHeight(0),Runtime.getRuntime().maxMemory());
                if(reader.getFormatName().equalsIgnoreCase("gif")){
                    var first=reader.read(0);int width=first.getWidth(),height=first.getHeight();
                    return PixelImages.toFx(new com.parallelvisionlab.model.PixelImage(width,height,first.getRGB(0,0,width,height,null,0,width)));
                }
            }
            finally{reader.dispose();}
        }
        try (InputStream input = Files.newInputStream(path)) {
            Image image = new Image(input);
            if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new IOException("This file could not be decoded as an image.", image.getException());
            }
            return image;
        }
    }

    static void validateDimensions(int width,int height,long heapLimit)throws IOException {
        long pixels=(long)width*height;
        long maximum=Math.min(64_000_000L,heapLimit/96);
        if(width<1||height<1||pixels>maximum)throw new IOException("Image dimensions "+width+" × "+height+
                " exceed this JVM's safe processing limit of "+maximum+" pixels. Export a smaller JPEG/PNG or run with a larger Java heap.");
    }
}
