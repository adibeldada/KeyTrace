import { useEffect, useRef } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'

function App() {
  const socketRef = useRef<WebSocket | null>(null)   // the WebSocket connection
  const editorRef = useRef<any>(null)                // the editor
  const applyingRemote = useRef(false)               // true while we insert text that didn't come from typing
  const replaying = useRef(false)                    // true while a replay is playing

  const room = new URLSearchParams(window.location.search).get("room") ?? "test"

  // close the connection when the page closes
  useEffect(() => {
    return () => socketRef.current?.close()
  }, [])

  // New Room button: ask the backend for an ID, then go to that room
  async function handleNewRoom() {
    const response = await fetch("http://localhost:8080/api/rooms", { method: "POST" })
    const newRoomId = await response.text()
    window.location.href = "/?room=" + newRoomId
  }

  // Replay button: get all snapshots, play them back with the original timing
  async function handleReplay() {
    // 1. ask the backend for this room's snapshots
    const response = await fetch("http://localhost:8080/api/rooms/" + room + "/snapshots")

    // 2. turn the reply into an array of { message, currentTime }
    const snapshots: { message: string; currentTime: number }[] = await response.json()
    if (snapshots.length === 0) return

    const editor = editorRef.current
    const start = snapshots[0].currentTime
    replaying.current = true

    // 3. schedule each snapshot to appear at the moment it originally happened
    snapshots.forEach((snap) => {
      setTimeout(() => {
        applyingRemote.current = true
        editor.setValue(snap.message)
        applyingRemote.current = false
      }, snap.currentTime - start)
    })

    // 4. after the last snapshot, the replay is over
    const total = snapshots[snapshots.length - 1].currentTime - start
    setTimeout(() => {
      replaying.current = false
    }, total + 100)
  }

  // editor is ready: connect to the backend
  function handleMount(editor: any) {
    editorRef.current = editor

    const socket = new WebSocket("ws://localhost:8080/ws?room=" + room)
    socketRef.current = socket

    socket.onopen = () => console.log("connected to room " + room)

    // someone else typed: show their code (unless a replay is playing)
    socket.onmessage = (event) => {
      if (replaying.current) return
      applyingRemote.current = true
      editor.setValue(event.data)
      applyingRemote.current = false
    }
  }

  // you typed: send your code to the backend (unless it came from the server or a replay)
  function handleChange(value: string | undefined) {
    if (applyingRemote.current || replaying.current) return
    socketRef.current?.send(value ?? "")
  }

  const buttonStyle = {
    fontSize: "20px",
    padding: "12px 24px",
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
        <button onClick={handleReplay} style={buttonStyle}>Replay</button>
      </div>

      <Editor
        height="80vh"
        defaultLanguage="python"
        defaultValue="# start coding here"
        theme="vs-dark"
        onMount={handleMount}
        onChange={handleChange}
      />
    </>
  )
}

export default App