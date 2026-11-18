package com.parallelvisionlab.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Optional NVIDIA driver telemetry; unavailable values are never synthesized. */
public final class NvidiaTelemetry {
    public record Snapshot(String name,double utilization,double usedMiB,double totalMiB,double watts,double temperature) { }
    private NvidiaTelemetry() { }
    public static Optional<Snapshot> sample() {
        Process process=null;
        try {
            process=new ProcessBuilder("nvidia-smi","--id=0","--query-gpu=name,utilization.gpu,memory.used,memory.total,power.draw,temperature.gpu","--format=csv,noheader,nounits")
                    .redirectErrorStream(true).start();
            if(!process.waitFor(2,TimeUnit.SECONDS)||process.exitValue()!=0)return Optional.empty();
            return Optional.of(parse(new String(process.getInputStream().readNBytes(4096),StandardCharsets.UTF_8).trim()));
        }catch(IOException|IllegalArgumentException exception){return Optional.empty();}
        catch(InterruptedException exception){Thread.currentThread().interrupt();return Optional.empty();}
        finally{if(process!=null&&process.isAlive())process.destroyForcibly();}
    }
    static Snapshot parse(String text) {
        String[] fields=text.split(",",-1);if(fields.length!=6||fields[0].isBlank())throw new IllegalArgumentException("Invalid GPU telemetry");
        double load=number(fields[1]),used=number(fields[2]),total=number(fields[3]),power=number(fields[4]),temperature=number(fields[5]);
        if((Double.isFinite(load)&&(load<0||load>100))||(Double.isFinite(used)&&used<0)||(Double.isFinite(total)&&total<=0))throw new IllegalArgumentException("Invalid GPU values");
        return new Snapshot(fields[0].trim(),load,used,total,power,temperature);
    }
    private static double number(String text) {
        String value=text.trim();if(value.equals("N/A")||value.contains("Not Supported"))return Double.NaN;
        double parsed=Double.parseDouble(value);if(!Double.isFinite(parsed))throw new IllegalArgumentException("Invalid telemetry number");return parsed;
    }
}
