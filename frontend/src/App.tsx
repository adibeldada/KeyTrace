import { useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'

type Snapshot = { message: string; currentTime: number }
type CreatedRoom = { roomId: string; hostToken: string }

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
  const [role, setRole] = useState<string | null>(null)                   // NEW: "HOST" or "PARTICIPANT", from the server

  // no default room; if there's no ?room= we show the home screen
  const room = new URLSearchParams(window.location.search).get("room")

  useEffect(() => {
    return () => {
      socketRef.current?.close()
      stopInterval()
    }
  }, [])

  // ---------- New Room ----------
  // takes a mode, "SOLO" | "INTERVIEW" | "GROUP" (must match the Java enum names)
  async function handleNewRoom(mode: string) {
    const response = await fetch("http://localhost:8080/api/rooms?mode=" + mode, { method: "POST" })
    const data: CreatedRoom = await response.json()
    localStorage.setItem("keytrace-host-" + data.roomId, data.hostToken)   // save our host "key card"
    window.location.href = "/?room=" + data.roomId
  }

  // ---------- End session ----------
  async function handleEndSession() {
    const sure = window.confirm("End the session for everyone in this room?")
    if (!sure) return

    const token = localStorage.getItem("keytrace-host-" + room)   // our host token, or null

    const response = await fetch(
      "http://localhost:8080/api/rooms/" + room + "/end?token=" + token,   // send the token as proof
      { method: "POST" }
    )

    if (!response.ok) {                                           // 403 = not the host
      alert("Only the host can end the session.")
    }
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

    const response = await fetch("http://localhost:8080/api/rooms/" + room + "/snapshots")
    const data: Snapshot[] = await response.json()

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

    // CHANGED: send our host token (if we have one) when joining
    const token = localStorage.getItem("keytrace-host-" + room)   // our saved host token, or null
    let url = "ws://localhost:8080/ws?room=" + room
    if (token) {
      url = url + "&token=" + token                               // only hosts have one
    }
    const socket = new WebSocket(url)
    socketRef.current = socket

    socket.onopen = () => console.log("connected to room " + room)

    socket.onmessage = (event) => {
      const msg = JSON.parse(event.data)

      // NEW: the server tells us our role right after we join
      if (msg.type === "role") {
        setRole(msg.text)
        return
      }

      if (msg.type === "count") {
        setPlayerCount(Number(msg.text))
        return
      }

      if (msg.type === "code") {
        if (replaying.current) return
        applyingRemote.current = true
        editor.setValue(msg.text)
        applyingRemote.current = false
      }
    }

    // handle all our custom close codes from the backend
    socket.onclose = (event) => {
      if (event.code === 4000) enterReplay()                                          // session ended
      if (event.code === 4003) setErrorMessage("This room is full.")                  // room full
      if (event.code === 4004) setErrorMessage("Room not found. Create a new room.")  // bad link / server restarted
    }
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

  const buttonStyle = {
    fontSize: "18px",
    padding: "10px 20px",
    backgroundColor: "#66251e",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
  }

  // the three "create a room" buttons, reused on both screens
  const modeButtons = (
    <>
      <button onClick={() => handleNewRoom("SOLO")} style={buttonStyle}>New Solo</button>
      <button onClick={() => handleNewRoom("INTERVIEW")} style={buttonStyle}>New Interview</button>
      <button onClick={() => handleNewRoom("GROUP")} style={buttonStyle}>New Group</button>
    </>
  )

  // home screen when there's no room in the URL
  if (!room) {
    return (
      <div style={{ padding: "40px" }}>
        <h1>KeyTrace</h1>
        <p style={{ fontSize: "18px" }}>Practice coding interviews, then replay how you solved them.</p>
        <p style={{ fontSize: "16px", opacity: 0.8 }}>
          Solo: just you · Interview: 2 people · Group: up to 10
        </p>
        <div style={{ display: "flex", gap: "16px", marginTop: "20px" }}>{modeButtons}</div>
      </div>
    )
  }

  // room screen
  return (
    <>
      <div style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
        <h1>KeyTrace, room: {room}</h1>
        {modeButtons}

        {/* CHANGED: only the host sees "End session" */}
        {!inReplay && !errorMessage && role === "HOST" && (
          <button onClick={handleEndSession} style={buttonStyle}>End session</button>
        )}

        {inReplay && <span style={{ fontSize: "18px" }}>Session ended: replay</span>}

        {/* NEW: show our role */}
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
          <button onClick={togglePlay} style={{ ...buttonStyle, width: "130px" }}>
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
          height="75vh"
          defaultLanguage="python"
          defaultValue="# start coding here"
          theme="vs-dark"
          onMount={handleMount}
          onChange={handleChange}
          options={{ readOnly: inReplay }}
        />
      )}
    </>
  )
}

export default App