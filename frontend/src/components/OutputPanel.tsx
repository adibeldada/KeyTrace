// What the backend sends back after a run (same fields as the Java RunResult)
export type RunResult = { output: string; exitCode: number; timedOut: boolean }

type Props = {
  running: boolean           // true while the code is running in the sandbox
  result: RunResult | null   // the last run's result, or null if nothing has run yet
}

// The box under the editor that shows what the code printed
export default function OutputPanel({ running, result }: Props) {
  // one short line saying how the run ended
  let status = ""
  if (running) status = "⏳ Running…"
  else if (result === null) status = "Click ▶ Run to run your code"
  else if (result.timedOut) status = "⏱ Time limit exceeded (5s)"
  else if (result.exitCode === 137) status = "💥 Memory limit exceeded (128 MB)"
  else if (result.exitCode === 0) status = "✅ Finished"
  else status = "❌ Exited with code " + result.exitCode

  return (
    <div style={{ background: "#1e1e1e", color: "#d4d4d4", padding: "10px 14px", fontFamily: "monospace" }}>
      <div style={{ opacity: 0.8, marginBottom: "6px" }}>{status}</div>
      <pre style={{ margin: 0, whiteSpace: "pre-wrap", maxHeight: "20vh", overflowY: "auto" }}>
        {!running && result ? result.output : ""}
      </pre>
    </div>
  )
}