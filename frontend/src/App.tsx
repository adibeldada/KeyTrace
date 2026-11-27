import { useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'
import { endSession, getSnapshots, roomSocketUrl, type Snapshot } from './api/api'
import HomeScreen from './components/HomeScreen'
import ModeButtons from './components/ModeButtons'
import OutputPanel, { type RunResult } from './components/OutputPanel'   // NEW: run

function App() {
  const socketRef = useRef<WebSocket | null>(null)
  const editorRef = useRef<any>(null)
  const applyingRemote = useRef(false)
  const replaying = useRef(false)

  // replay data kept in refs so the playback timer always sees the latest values
  const snapshotsRef = useRef<Snapshot[]>([])
  const currentMsRef = useRef(0)
  const shownIndexRef = useRef(-1)
  const intervalRef = useRef<number | null>(null)
  const lastTickRef = useRef(0)

  const [inReplay, setInReplay] = useState(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentMs, setCurrentMs] = useState(0)
  const [durationMs, setDurationMs] = useState(0)
  const [playerCount, setPlayerCount] = useState<number | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)   // "room full" etc.
  const [role, setRole] = useState<string | null>(null)                   // "HOST" or "PARTICIPANT", from the server
  const [running, setRunning] = useState(false)                           // NEW: run, is code running in the sandbox right now?
  const [runResult, setRunResult] = useState<RunResult | null>(null)      // NEW: run, the last run's output
  const [mode, setMode] = useState<string | null>(null)                   // "SOLO" | "INTERVIEW" | "GROUP"

  const remoteCursors = useRef<Record<string, any>>({})   // other people's cursors: session id → their drawn cursor
  // no default room; if there's no ?room= we show the home screen
  const room = new URLSearchParams(window.location.search).get("room")

  useEffect(() => {
    return () => {
      socketRef.current?.close()
      stopInterval()
    }
  }, [])

  // ---------- End session ----------
  async function handleEndSession() {
    const sure = window.confirm("End the session for everyone in this room?")
    if (!sure) return
    const ok = await endSession(room!)
    if (!ok) alert("Only the host can end the session.")   // 403 = not the host
  }

  // ---------- Replay helpers ----------
  function indexAt(ms: number) {
    const list = snapshotsRef.current
    const target = list[0].currentTime + ms
    let low = 0
    let high = list.length - 1
    let answer = 0
    while (low <= high) {
      const mid = Math.floor((low + high) / 2)
      if (list[mid].currentTime <= target) {
        answer = mid
        low = mid + 1
      } else {
        high = mid - 1
      }
    }
    return answer
  }

  function showAt(ms: number) {
    currentMsRef.current = ms
    setCurrentMs(ms)
    if (snapshotsRef.current.length === 0) return
    const i = indexAt(ms)
    if (i !== shownIndexRef.current) {
      shownIndexRef.current = i
      applyingRemote.current = true
      editorRef.current.setValue(snapshotsRef.current[i].message)
      applyingRemote.current = false
    }
  }

  function stopInterval() {
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
  }

  async function enterReplay() {
    if (replaying.current) return
    replaying.current = true

    Object.values(remoteCursors.current).forEach((c: any) => c.clear())   // no live cursors in replay
    remoteCursors.current = {}

    const data = await getSnapshots(room!)

    snapshotsRef.current = data
    shownIndexRef.current = -1
    setDurationMs(data.length > 0 ? data[data.length - 1].currentTime - data[0].currentTime : 0)
    setInReplay(true)
    showAt(0)
  }

  function play() {
    if (snapshotsRef.current.length === 0) return
    if (currentMsRef.current >= durationMs) showAt(0)

    setIsPlaying(true)
    lastTickRef.current = performance.now()
    intervalRef.current = window.setInterval(() => {
      const now = performance.now()
      const next = currentMsRef.current + (now - lastTickRef.current)
      lastTickRef.current = now

      if (next >= durationMs) {
        showAt(durationMs)
        pause()
      } else {
        showAt(next)
      }
    }, 50)
  }

  function pause() {
    stopInterval()
    setIsPlaying(false)
  }

  function togglePlay() {
    if (isPlaying) pause()
    else play()
  }

  function handleSlider(ms: number) {
    lastTickRef.current = performance.now()
    showAt(ms)
  }

  // ---------- Live editing ----------
  function handleMount(editor: any) {
    editorRef.current = editor

    const socket = new WebSocket(roomSocketUrl(room!))   // adds our host token if we have one
    socketRef.current = socket

    socket.onopen = () => console.log("connected to room " + room)

    socket.onmessage = (event) => {
      const msg = JSON.parse(event.data)

      // the server tells us our role right after we join
      if (msg.type === "role") {
        setRole(msg.text)
        return
      }

      // the server tells us the room's mode right after we join
      if (msg.type === "mode") {
        setMode(msg.text)
        return
      }

      // NEW: run, someone clicked Run: show "Running…" for everyone
      if (msg.type === "running") {
        setRunning(true)
        return
      }

      // NEW: run, the run finished: msg.text is the RunResult as JSON
      if (msg.type === "output") {
        setRunning(false)
        setRunResult(JSON.parse(msg.text))
        return
      }

      if (msg.type === "count") {
        setPlayerCount(Number(msg.text))
        return
      }

      // someone else moved their cursor: "abc:4:10" → draw it at line 4, column 10
      if (msg.type === "cursor") {
        if (replaying.current) return
        const [id, line, column] = msg.text.split(":")
        const l = Number(line)
        const c = Number(column)

        // first time we see this person → make a drawing slot for them
        if (!remoteCursors.current[id]) {
          remoteCursors.current[id] = editor.createDecorationsCollection()
        }

        // draw (or move) their cursor at that spot
        remoteCursors.current[id].set([{
          range: { startLineNumber: l, startColumn: c, endLineNumber: l, endColumn: c },
          options: { beforeContentClassName: "remote-cursor" },
        }])
        return
      }

      // someone left: remove their cursor
      if (msg.type === "cursor-leave") {
        remoteCursors.current[msg.text]?.clear()
        delete remoteCursors.current[msg.text]
        return
      }

      if (msg.type === "code") {
        if (replaying.current) return
        applyingRemote.current = true
        const position = editor.getPosition()        // remember where my cursor is
        editor.setValue(msg.text)                    // replace the text (this resets the cursor)
        if (position) editor.setPosition(position)   // put my cursor back
        applyingRemote.current = false
      }
    }

    // handle all our custom close codes from the backend
    socket.onclose = (event) => {
      if (event.code === 4000) enterReplay()                                          // session ended
      if (event.code === 4003) setErrorMessage("This room is full.")                  // room full
      if (event.code === 4004) setErrorMessage("Room not found. Create a new room.")  // bad link / server restarted
    }

    // every time MY cursor moves, tell the server where it is ("line:column")
    editor.onDidChangeCursorPosition((e: any) => {
      if (applyingRemote.current || replaying.current) return   // not a real move by me
      if (socket.readyState !== WebSocket.OPEN) return          // not connected yet
      const line = e.position.lineNumber
      const column = e.position.column
      socket.send(JSON.stringify({ type: "cursor", text: line + ":" + column }))
    })
  }

  // NEW: run, send the editor's code to the backend to run in the sandbox
  function handleRun() {
    const code = editorRef.current?.getValue() ?? ""
    socketRef.current?.send(JSON.stringify({ type: "run", text: code }))
  }

  function handleChange(value: string | undefined) {
    if (applyingRemote.current || replaying.current) return
    socketRef.current?.send(JSON.stringify({ type: "code", text: value ?? "" }))
  }

  function formatTime(ms: number) {
    const totalSeconds = Math.floor(ms / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return minutes + ":" + String(seconds).padStart(2, "0")
  }

  // home screen when there's no room in the URL
  if (!room) {
    return <HomeScreen />
  }

  // room screen
  return (
    <>
      <div style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
        <h1>KeyTrace, room: {room}</h1>

        {/* show which kind of room this is */}
        {mode && (
          <span style={{ fontSize: "18px" }}>
            {mode === "SOLO" ? "🧑 Solo" : mode === "INTERVIEW" ? "🎤 Interview" : "👥 Group"}
          </span>
        )}

        <ModeButtons />

        {/* NEW: run, anyone in a live room can run the code */}
        {!inReplay && !errorMessage && (
          <button onClick={handleRun} className="button" disabled={running}>
            {running ? "⏳ Running…" : "▶ Run"}
          </button>
        )}

        {/* only the host sees "End session" */}
        {!inReplay && !errorMessage && role === "HOST" && (
          <button onClick={handleEndSession} className="button">End session</button>
        )}

        {inReplay && <span style={{ fontSize: "18px" }}>Session ended: replay</span>}

        {/* show our role */}
        {!inReplay && role && (
          <span style={{ fontSize: "18px" }}>{role === "HOST" ? "👑 Host" : "👤 Participant"}</span>
        )}

        {!inReplay && playerCount !== null && (
          <span style={{ fontSize: "18px" }}>👥 {playerCount} {playerCount === 1 ? "player" : "players"}</span>
        )}
      </div>

      {/* error message, e.g. room full or not found */}
      {errorMessage && <p style={{ color: "#ff6b6b", fontSize: "18px" }}>{errorMessage}</p>}

      {inReplay && (
        <div style={{ display: "flex", alignItems: "center", gap: "12px", margin: "10px 0" }}>
          <button onClick={togglePlay} className="button" style={{ width: "130px" }}>
            {isPlaying ? "⏸ Pause" : "▶ Play"}
          </button>
          <span>{formatTime(currentMs)}</span>
          <input
            type="range"
            min={0}
            max={durationMs}
            value={currentMs}
            onChange={(e) => handleSlider(Number(e.target.value))}
            style={{ flex: 1 }}
          />
          <span>{formatTime(durationMs)}</span>
        </div>
      )}

      {!errorMessage && (
        <Editor
          height="60vh"
          defaultLanguage="python"
          defaultValue="# start coding here"
          theme="vs-dark"
          onMount={handleMount}
          onChange={handleChange}
          options={{ readOnly: inReplay }}
        />
      )}

      {/* NEW: run, what the code printed (live rooms only) */}
      {!inReplay && !errorMessage && <OutputPanel running={running} result={runResult} />}
    </>
  )
}

export default App