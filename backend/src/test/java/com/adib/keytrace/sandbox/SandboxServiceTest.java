package com.adib.keytrace.sandbox;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// Runs real code in real Docker containers (Docker Desktop must be running).
class SandboxServiceTest {

    SandboxService sandbox = new SandboxService();   // no Spring needed: it has no dependencies

    @Test
    void runsNormalCode() throws Exception {
        //IMPORTANT, its creating a RunResult object (since sandboxservice.runpython returns that object and that result has 3 attributes)
        RunResult result = sandbox.runPython("print(2 + 2)");

        assertEquals("4", result.getOutput().trim());   // trim() removes the newline after "4"
        assertEquals(0, result.getExitCode());
        assertFalse(result.isTimedOut());
    }

    @Test
    void showsPythonErrors() throws Exception {
        RunResult result = sandbox.runPython("1 / 0");

        assertTrue(result.getOutput().contains("ZeroDivisionError"));   // the error text reaches us
        assertEquals(1, result.getExitCode());                          // Python exits with 1 on an error
        assertFalse(result.isTimedOut());
    }

    @Test
    void killsInfiniteLoops() throws Exception {
        RunResult result = sandbox.runPython("print('start')\nwhile True:\n    pass");

        assertTrue(result.isTimedOut());                   // stopped by our 5-second limit
        assertTrue(result.getOutput().contains("start"));  // we still see what it printed before (thanks to -u)
    }

    @Test
    void hasNoInternet() throws Exception {
        // connect straight to an IP (no DNS lookup), so it fails instantly instead of retrying
        RunResult result = sandbox.runPython(
            "import socket\nsocket.create_connection(('1.1.1.1', 80), timeout=2)\nprint('connected!')");

        assertFalse(result.getOutput().contains("connected!"));   // it never got online
        assertNotEquals(0, result.getExitCode());                 // it failed with an error
        assertFalse(result.isTimedOut());                         // and failed fast
    }
}