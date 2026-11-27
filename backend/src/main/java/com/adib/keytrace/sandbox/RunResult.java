package com.adib.keytrace.sandbox;

public class RunResult {
    // the attributes every RunResult has
    private final String output;
    private final int exitCode;
    private final boolean timedOut;

    // constructor: runs when you do "new RunResult(...)", and fills in the attributes
    public RunResult(String output, int exitCode, boolean timedOut) {
        this.output = output;
        this.exitCode = exitCode;
        this.timedOut = timedOut;
    }

    // getters: how other code reads the attributes
    public String getOutput() { 
        return output; 
        }
    public int getExitCode() { 
        return exitCode; 
        }
    public boolean isTimedOut() { 
        return timedOut; 
        }
}