package com.parallelvisionlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.image.Imaege;
import javafx.imageio.ImageIO;

public Image load(Path path) throws IOException 
{
        if(path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".dng"))
            throw new IOException("DNG is a camera RAW format. Export it as JPEG or PNG, then open the exported image.");
        if(Files.size(path)>256L*1024*1024)
            throw new IOException("Image file exceeds the 256 MB input limit.");
        try(var input=ImageIO.createImageInputStream(path.toFile())) 
        {
            if(input==null)
                throw new IOException("Image is not readable.");
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())
                throw new IOException("Unsupported or corrupt image. Use PNG, JPEG, BMP or GIF.");
            var reader=readers.next();


