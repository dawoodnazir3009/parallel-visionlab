package com.parallelvisionlab.benchmark;

import com.parallelvisionlab.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Versioned, human-readable local experiment files; never uses Java object deserialization. */
public final class BenchmarkArchive {
    private final Path directory;
    public BenchmarkArchive(Path directory) { this.directory = directory; }
    public record Loaded(List<BenchmarkRun> runs, int skipped) { }

    public void save(BenchmarkRun run) throws IOException {
        String id = UUID.fromString(run.id()).toString();
        Properties p = new Properties();
        p.setProperty("version","1");
        p.setProperty("id",id); p.setProperty("timestamp",run.timestamp().toString());
        p.setProperty("source",run.source()); p.setProperty("hash",run.imageHash());
        p.setProperty("width",""+run.width()); p.setProperty("height",""+run.height());
        p.setProperty("operation",run.request().operation().name());
        p.setProperty("radius",""+run.request().gaussianRadius()); p.setProperty("sigma",""+run.request().gaussianSigma());
        p.setProperty("warmups",""+run.config().warmups()); p.setProperty("iterations",""+run.config().iterations());
        p.setProperty("workers",""+run.config().workers()); p.setProperty("leafRows",""+run.config().leafRows());
        p.setProperty("java",run.javaVersion()); p.setProperty("os",run.operatingSystem());
        p.setProperty("count",""+run.measurements().size());
        for(int i=0;i<run.measurements().size();i++) {
            var m=run.measurements().get(i);String key="m."+i+".";
            p.setProperty(key+"backend",m.backend().name());p.setProperty(key+"workers",""+m.workers());
            p.setProperty(key+"median",""+m.medianMs());p.setProperty(key+"min",""+m.minimumMs());p.setProperty(key+"max",""+m.maximumMs());
            p.setProperty(key+"speedup",""+m.speedup());p.setProperty(key+"efficiency",""+m.efficiency());p.setProperty(key+"throughput",""+m.megapixelsPerSecond());
            p.setProperty(key+"kernelMs",""+m.kernelMs());p.setProperty(key+"uploadMs",""+m.uploadMs());p.setProperty(key+"downloadMs",""+m.downloadMs());p.setProperty(key+"tolerance",""+m.rgbTolerance());
        }
        Files.createDirectories(directory);
        Path temporary=Files.createTempFile(directory,"pending-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(temporary)){p.store(out,"Parallel VisionLab measured experiment");}
            try {Files.move(temporary,directory.resolve(id+".properties"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException exception){Files.move(temporary,directory.resolve(id+".properties"),StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }

    public Loaded load() throws IOException {
        if(!Files.exists(directory))return new Loaded(List.of(),0);
        List<BenchmarkRun> runs=new ArrayList<>();int skipped=0;
        try(var paths=Files.list(directory)) {
            for(Path path:paths.filter(p->p.getFileName().toString().endsWith(".properties")).limit(1000).toList()) {
                try {runs.add(read(path));}catch(IOException|RuntimeException exception){skipped++;}
            }
        }
        runs.sort(Comparator.comparing(BenchmarkRun::timestamp).reversed());
        return new Loaded(List.copyOf(runs),skipped);
    }

    private BenchmarkRun read(Path file) throws IOException {
        if(Files.size(file)>262144)throw new IOException("Experiment file too large");
        Properties p=new Properties();try(InputStream input=Files.newInputStream(file)){p.load(input);}
        if(!"1".equals(p.getProperty("version")))throw new IOException("Unsupported experiment version");
        String id=UUID.fromString(p.getProperty("id")).toString();
        int width=integer(p,"width"),height=integer(p,"height"),count=integer(p,"count");
        if(width<1||height<1||(long)width*height>Integer.MAX_VALUE||count<1||count>5)throw new IOException("Invalid experiment shape/count");
        List<BenchmarkRun.Measurement> rows=new ArrayList<>();
        for(int i=0;i<count;i++) {
            String key="m."+i+".";int workers=integer(p,key+"workers");
            double median=positive(p,key+"median"),min=positive(p,key+"min"),max=positive(p,key+"max");
            var backend=ExecutionBackend.valueOf(p.getProperty(key+"backend"));
            if((backend.gpu()?workers!=0:workers<1||workers>256)||min>median||median>max)throw new IOException("Invalid measurement");
            int tolerance=Integer.parseInt(p.getProperty(key+"tolerance","0"));
            if(tolerance<0||tolerance>1)throw new IOException("Invalid tolerance");
            rows.add(new BenchmarkRun.Measurement(backend,workers,median,min,max,
                    positive(p,key+"speedup"),backend.gpu()?Double.NaN:positive(p,key+"efficiency"),positive(p,key+"throughput"),
                    event(p,key+"kernelMs"),event(p,key+"uploadMs"),event(p,key+"downloadMs"),tolerance));
        }
        return new BenchmarkRun(id,Instant.parse(p.getProperty("timestamp")),Objects.requireNonNull(p.getProperty("source")),
                Objects.requireNonNull(p.getProperty("hash")),width,height,
                new ProcessingRequest(FilterOperation.valueOf(p.getProperty("operation")),integer(p,"radius"),Double.parseDouble(p.getProperty("sigma"))),
                new BenchmarkConfig(integer(p,"warmups"),integer(p,"iterations"),integer(p,"workers"),integer(p,"leafRows")),rows,
                Objects.requireNonNull(p.getProperty("java")),Objects.requireNonNull(p.getProperty("os")));
    }
    private static int integer(Properties p,String key){return Integer.parseInt(p.getProperty(key));}
    private static double event(Properties p,String key)throws IOException{double value=Double.parseDouble(p.getProperty(key,"0"));if(!Double.isFinite(value)||value<0)throw new IOException("Invalid event timing");return value;}
    private static double positive(Properties p,String key)throws IOException {
        double value=Double.parseDouble(p.getProperty(key));
        if(!Double.isFinite(value)||value<=0)throw new IOException("Invalid metric");return value;
    }
}
