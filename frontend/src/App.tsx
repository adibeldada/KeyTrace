import { useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'

type Snapshot = { message: string; currentTime: number }

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

  const room = new URLSearchParams(window.location.search).get("room") ?? "test"

  useEffect(() => {
    return () => {
      socketRef.current?.close()
      stopInterval()
    }
  }, [])

  // ---------- New Room ----------
  async function handleNewRoom() {
    const response = await fetch("http://localhost:8080/api/rooms", { method: "POST" })
    const newRoomId = await response.text()
    window.location.href = "/?room=" + newRoomId
  }

  // ---------- End session ----------
  // asks the backend to end the session; the backend then closes EVERYONE's
  // connection with code 4000, and each browser switches to replay (see onclose below)
  async function handleEndSession() {
    const sure = window.confirm("End the session for everyone in this room?")
    if (!sure) return
    await fetch("http://localhost:8080/api/rooms/" + room + "/end", { method: "POST" })
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

  // load the recording and switch this page into replay mode
  async function enterReplay() {
    if (replaying.current) return          // already in replay
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

    const socket = new WebSocket("ws://localhost:8080/ws?room=" + room)
    socketRef.current = socket

    socket.onopen = () => console.log("connected to room " + room)

    socket.onmessage = (event) => {
      if (replaying.current) return
      applyingRemote.current = true
      editor.setValue(event.data)
      applyingRemote.current = false
    }

    // the server closed our connection with code 4000 = "session ended" → go to replay.
    // This also happens right away if you open a room that has already ended.
    socket.onclose = (event) => {
      if (event.code === 4000) {
        enterReplay()
      }
    }
  }

  function handleChange(value: string | undefined) {
    if (applyingRemote.current || replaying.current) return
    socketRef.current?.send(value ?? "")
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

  return (
    <>
      <div style={{ display: "flex", alignItems: "center", gap: "20px" }}>
        <h1>KeyTrace, room: {room}</h1>
        <button onClick={handleNewRoom} style={buttonStyle}>New Room</button>
        {!inReplay && <button onClick={handleEndSession} style={buttonStyle}>End session</button>}
        {inReplay && <span style={{ fontSize: "18px" }}>Session ended: replay</span>}
      </div>

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

      <Editor
        height="75vh"
        defaultLanguage="python"
        defaultValue="# start coding here"
        theme="vs-dark"
        onMount={handleMount}
        onChange={handleChange}
        options={{ readOnly: inReplay }}
      />
    </>
  )
}

export default App