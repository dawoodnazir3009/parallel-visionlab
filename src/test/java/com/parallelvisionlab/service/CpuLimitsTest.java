package com.parallelvisionlab.service;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

class CpuLimitsTest {
    @Test @EnabledOnOs(OS.WINDOWS)
    void restrictsAndVerifiesOnlyAnIsolatedChildProcess() throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"),"bin","java.exe").toString();
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        Process child=new ProcessBuilder(javaExecutable,"-cp",classpath,Probe.class.getName()).redirectErrorStream(true).start();
        try {
            assertTrue(child.waitFor(25,TimeUnit.SECONDS));
            String output=new String(child.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();
            assertEquals(0,child.exitValue(),output);
            assertTrue(output.matches("(?:[1-9]|1[0-6]) logical processors; affinity mask [0-9A-F]+"),output);
        } finally { if(child.isAlive())child.destroyForcibly(); }
    }

    public static class Probe {
        public static void main(String[] args) {
            CpuLimits.applyProcessLimit();
            CpuLimits.applyProcessLimit();
            System.out.println(CpuLimits.affinityStatus());
        }
    }
}
