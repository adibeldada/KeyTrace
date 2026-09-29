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
  const currentMsRef = useRef(0)          // playback position, in ms since the first snapshot
  const shownIndexRef = useRef(-1)        // which snapshot is currently in the editor
  const intervalRef = useRef<number | null>(null)
  const lastTickRef = useRef(0)

  // state = things shown on screen
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

  // ---------- Replay helpers ----------

  // binary search: index of the last snapshot at or before `ms`
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

  // move playback to `ms` and update the editor if a different snapshot should show
  function showAt(ms: number) {
    currentMsRef.current = ms
    setCurrentMs(ms)
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

  // ---------- Replay controls ----------

  // Replay button: load the recording and start at 0:00, paused
  async function handleReplay() {
    const response = await fetch("http://localhost:8080/api/rooms/" + room + "/snapshots")
    const data: Snapshot[] = await response.json()
    if (data.length === 0) return

    snapshotsRef.current = data
    shownIndexRef.current = -1
    setDurationMs(data[data.length - 1].currentTime - data[0].currentTime)
    replaying.current = true
    setInReplay(true)
    showAt(0)
  }

  function play() {
    // at the end? start over
    if (currentMsRef.current >= durationMs) showAt(0)

    setIsPlaying(true)
    lastTickRef.current = performance.now()
    intervalRef.current = window.setInterval(() => {
      const now = performance.now()
      const next = currentMsRef.current + (now - lastTickRef.current)
      lastTickRef.current = now

      if (next >= durationMs) {
        showAt(durationMs)
        pause()             // reached the end
      } else {
        showAt(next)
      }
    }, 50)                  // tick every 50 ms = 20 times a second
  }

  function pause() {
    stopInterval()
    setIsPlaying(false)
  }

  function togglePlay() {
    if (isPlaying) pause()
    else play()
  }

  // slider dragged: jump there (keeps playing if it was playing)
  function handleSlider(ms: number) {
    lastTickRef.current = performance.now()
    showAt(ms)
  }

  function handleBackToLive() {
    pause()
    showAt(durationMs)
    replaying.current = false
    setInReplay(false)
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
  }

  function handleChange(value: string | undefined) {
    if (applyingRemote.current || replaying.current) return
    socketRef.current?.send(value ?? "")
  }

  // 65000 ms → "1:05"
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
        {!inReplay && <button onClick={handleReplay} style={buttonStyle}>Replay</button>}
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
          <button onClick={handleBackToLive} style={buttonStyle}>Back to live</button>
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