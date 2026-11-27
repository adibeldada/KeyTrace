package com.adib.keytrace.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

// Runs someone's Python code safely inside a throwaway Docker container.
@Service
public class SandboxService {

    public RunResult runPython(String code) throws Exception {
        // A. write the code to a temp folder as main.py
        Path folder = Files.createTempDirectory("keytrace-run-");   // a new empty folder, e.g. ...\Temp\keytrace-run-8273645
        Path file = folder.resolve("main.py");                      // the path to main.py inside it
        Files.writeString(file, code);                              // create main.py with the person's code

        // Linux: let the container's user (1000) read the folder and file. Windows doesn't need this.
        if (folder.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        }

        // B. pick a unique container name, so we can kill exactly this one later
        String name = "keytrace-run-" + UUID.randomUUID();

        // C. build the docker run command (every piece is its own string)
        List<String> dockerCommand = List.of(
            "docker", "run",
            "--rm",                                       // delete the container when it's done
            "--name", name,                               // our own name, so we can kill it later
            "--network", "none",                          // no internet
            "--memory", "128m",                           // max 128 MB of RAM
            "--cpus", "0.5",                              // half a CPU
            "--pids-limit", "64",                         // max 64 processes (no fork bombs)
            "--read-only",                                // can't write anywhere
            "--user", "1000",                             // not an admin
            "-v", folder.toAbsolutePath() + ":/code:ro",  // our temp folder → /code inside, read-only
            "python:3.12-alpine",                         // the image: tiny Linux + Python
            "python", "-u", "/code/main.py"               // run the file, unbuffered
        );

        // D. start it (errors and prints come out of the same stream)
        Process process = new ProcessBuilder(dockerCommand)
                .redirectErrorStream(true)
                .start();

        // E. read the output in the background, bit by bit, keeping what we get even if the run is killed
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            try (InputStream in = process.getInputStream()) {
                int n;
                while ((n = in.read(chunk)) != -1) {                       // keep reading until the program ends
                    int room = 10_000 - collected.size();                  // cap at 10,000 bytes
                    if (room > 0) collected.write(chunk, 0, Math.min(n, room));
                    // past the cap we keep reading but throw it away, so the program never blocks
                }
            } catch (IOException e) {
                // the stream was closed because we killed the process: keep what we already have
            }
            return collected.toString(StandardCharsets.UTF_8);
        });

        // F. wait max 5 seconds, kill it if it's still running
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        boolean timedOut = !finished;

        if (timedOut) {
            new ProcessBuilder("docker", "kill", name).start().waitFor(5, TimeUnit.SECONDS);   // stop the container
            if (!process.waitFor(5, TimeUnit.SECONDS)) {   // docker run normally exits by itself once the container is killed
                process.destroyForcibly();                 // only force it if it didn't
                process.waitFor();
            }
        }

        String output = outputFuture.get(5, TimeUnit.SECONDS);   // the text it printed (collected in step E)
        int exitCode = process.exitValue();                      // 0 = ok, 1 = Python error, 137 = killed

        // G. delete main.py and the temp folder
        Files.deleteIfExists(file);     // 1. delete main.py first
        Files.deleteIfExists(folder);   // 2. then the folder, which is empty now

        // H. return the result
        return new RunResult(output, exitCode, timedOut);
    }
}