package com.parallelvisionlab.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ImageLoaderTest {
    @Test void accepts48MegapixelPhotosWhenHeapBudgetAllows() throws Exception {
        ImageLoader.validateDimensions(8064,6048,6L*1024*1024*1024);
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(8064,6048,1024L*1024*1024));
    }

    @Test void explainsRawDngConversion() throws Exception {
        Path raw=Files.write(directory.resolve("camera.DNG"),new byte[]{73,73,42,0});
        assertTrue(assertThrows(IOException.class,()->loader.load(raw)).getMessage().contains("camera RAW"));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="visionlab.testImage", matches=".+")
    void opensUserSuppliedPhotoAtOriginalResolution() throws Exception {
        var image=loader.load(Path.of(System.getProperty("visionlab.testImage")));
        assertEquals(8064L*6048,(long)image.getWidth()*(long)image.getHeight());
        assertNotNull(image.getPixelReader());
    }
    @Test void animatedGifUsesOnlyTheFirstFrame()throws Exception {
        Path file=directory.resolve("animated.gif");
        var writer=javax.imageio.ImageIO.getImageWritersByFormatName("gif").next();
        try(var output=javax.imageio.ImageIO.createImageOutputStream(file.toFile())){
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int color:new int[]{0xffff0000,0xff0000ff}){
                var frame=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);frame.setRGB(0,0,color);
                writer.writeToSequence(new javax.imageio.IIOImage(frame,null,null),null);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
        assertEquals(0xffff0000,loader.load(file).getPixelReader().getArgb(0,0));
    }
    @Test void rejectsOversizedHeadersBeforePixelAllocation() {
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(100000,100000,4L*1024*1024*1024));
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(1024,1024,1024));
        assertThrows(IOException.class,()->ImageLoader.validateDimensions(0,2,Long.MAX_VALUE));
    }
    @TempDir Path directory;
    private final ImageLoader loader = new ImageLoader();

    @Test void decodesKnownPixelAndClosesFile() throws IOException {
        Path file = directory.resolve("pixel.png");
        Files.write(file, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII="));
        var image = loader.load(file);
        assertEquals(1, image.getWidth());
        assertEquals(1, image.getHeight());
        assertEquals(0xffffffff, image.getPixelReader().getArgb(0, 0));
        Files.delete(file);
        assertFalse(Files.exists(file));
    }

    @Test void rejectsCorruptImage() throws IOException {
        Path file = Files.writeString(directory.resolve("broken.png"), "not an image");
        assertThrows(IOException.class, () -> loader.load(file));
    }

    @Test void rejectsMissingFile() {
        assertThrows(IOException.class, () -> loader.load(directory.resolve("missing.png")));
    }
}
