package com.parallelvisionlab.benchmark;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class BenchmarkPreferences {
    private final Path file;
    public BenchmarkPreferences(Path file){this.file=file;}
    public BenchmarkConfig load(BenchmarkConfig fallback)throws IOException {
        if(!Files.exists(file))return fallback;
        Properties p=new Properties();try(InputStream in=Files.newInputStream(file)){p.load(in);}
        try {
            var loaded = new BenchmarkConfig(Integer.parseInt(p.getProperty("warmups")),Integer.parseInt(p.getProperty("iterations")),
                    Integer.parseInt(p.getProperty("workers")),Integer.parseInt(p.getProperty("leafRows")));
            return new BenchmarkConfig(loaded.warmups(),loaded.iterations(),
                    Math.min(com.parallelvisionlab.service.CpuLimits.MAX_WORKERS,loaded.workers()),loaded.leafRows());
        }
        catch(RuntimeException exception){throw new IOException("Invalid saved benchmark preferences",exception);}
    }
    public void save(BenchmarkConfig config)throws IOException {
        Properties p=new Properties();p.setProperty("warmups",""+config.warmups());p.setProperty("iterations",""+config.iterations());
        p.setProperty("workers",""+config.workers());p.setProperty("leafRows",""+config.leafRows());
        Path absolute=file.toAbsolutePath();Files.createDirectories(absolute.getParent());
        Path temporary=Files.createTempFile(absolute.getParent(),"preferences-",".tmp");
        try {try(OutputStream out=Files.newOutputStream(temporary)){p.store(out,"Parallel VisionLab preferences");}
            Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temporary);}
    }
}
