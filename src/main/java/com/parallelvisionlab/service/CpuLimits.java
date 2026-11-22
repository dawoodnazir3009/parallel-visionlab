package com.parallelvisionlab.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Limits processing pools and, on Windows, the complete application process. */
public final class CpuLimits {
    public static final int MAX_WORKERS = 8;
    private static boolean applied;
    private static String affinityStatus = "Not applied to this process";
    private CpuLimits() { }

    public static int automaticWorkers() {
        return Math.max(1, Math.min(MAX_WORKERS, Runtime.getRuntime().availableProcessors()));
    }

    public static void validateWorkers(int workers) {
        if (workers < 1 || workers > MAX_WORKERS)
            throw new IllegalArgumentException("CPU workers must be between 1 and 8 logical processors");
    }

    public static synchronized String affinityStatus() { return affinityStatus; }

    public static synchronized void applyProcessLimit() {
        if (applied) return;
        if (!System.getProperty("os.name").startsWith("Windows")) {
            affinityStatus = "8-worker cap; process affinity supported on Windows only";
            applied = true;
            return;
        }
        // Restrict only this application, using up to eight CPUs already allowed
        // by Windows. Never broaden an existing affinity restriction.
        String command = "$ErrorActionPreference='Stop'; $process=[System.Diagnostics.Process]::GetProcessById("
                + ProcessHandle.current().pid() + "); $allowed=$process.ProcessorAffinity.ToInt64(); "
                + "$selected=[long]0; $count=0; for($i=0;$i -lt 64 -and $count -lt 8;$i++){"
                + "$bit=([long]1 -shl $i); if(($allowed -band $bit) -ne 0){$selected=$selected -bor $bit; $count++}}; "
                + "if($count -eq 0){throw 'No eligible logical processors'}; "
                + "$process.ProcessorAffinity=[IntPtr]::new($selected); $process.Refresh(); "
                + "if($process.ProcessorAffinity.ToInt64() -ne $selected){throw 'Affinity verification failed'}; "
                + "Write-Output ($count.ToString()+' logical processors; affinity mask '+$selected.ToString('X'))";
        Process helper = null;
        try {
            helper = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-Command", command).redirectErrorStream(true).start();
            if (!helper.waitFor(15, TimeUnit.SECONDS)) throw new IOException("CPU affinity setup timed out");
            String output = new String(helper.getInputStream().readNBytes(4096), StandardCharsets.UTF_8).trim();
            if (helper.exitValue() != 0) throw new IOException(output);
            affinityStatus = output;
            applied = true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while applying the 8-logical-processor limit", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot apply the 8-logical-processor limit: " + exception.getMessage(), exception);
        } finally {
            if (helper != null && helper.isAlive()) helper.destroyForcibly();
        }
    }
}
