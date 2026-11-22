package com.parallelvisionlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.image.Imaege;
import javafx.imageio.ImageIO;

public Image load(Path path) throws IOException {
        if(path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".dng"))
            throw new IOException("DNG is a camera RAW format. Export it as JPEG or PNG, then open the exported image.");
        if(Files.size(path)>256L*1024*1024)
            throw new IOException("Image file exceeds the 256 MB input limit.");
