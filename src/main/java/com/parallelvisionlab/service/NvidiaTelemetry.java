package com.parallelvisionlab.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class NvidiaTelemetry 
{
    public record Snapshot(String name,double utilization,double usedMiB,double totalMiB,double watts,double temperature) { }
    
}
