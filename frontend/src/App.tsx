import { useEffect, useRef } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'

function App() {
  const socketRef = useRef<WebSocket | null>(null)
  const editorRef = useRef<any>(null)
  const applyingRemote = useRef(false)

  const room = new URLSearchParams(window.location.search).get("room") ?? "test"

  // CHANGED: this useEffect now only closes the socket when the page goes away
  useEffect(() => {
    return () => socketRef.current?.close()
  }, [])

     async function handleNewRoom() {
     const response = await fetch("http://localhost:8080/api/rooms", { method: "POST" })
     const newRoomId = await response.text()
     window.location.href = "/?room=" + newRoomId
   }

  // CHANGED: open the WebSocket here, once the editor is ready
  function handleMount(editor: any) {
    editorRef.current = editor

    const socket = new WebSocket("ws://localhost:8080/ws?room=" + room)
    socketRef.current = socket

    socket.onopen = () => console.log("connected to room " + room)

    socket.onmessage = (event) => {
      applyingRemote.current = true
      editor.setValue(event.data)   // the editor definitely exists now
      applyingRemote.current = false
    }
  }

  function handleChange(value: string | undefined) {
    if (applyingRemote.current) return
    socketRef.current?.send(value ?? "")
  }

    return (
  <>
    <div style={{ display: "flex", alignItems: "center", gap: "20px" }}>
      <h1>KeyTrace, room: {room}</h1>
      <button
        onClick={handleNewRoom}
        style={{
          fontSize: "20px",
          padding: "12px 24px",
          backgroundColor: "#66251e",
          color: "white",
          border: "none",
          borderRadius: "8px",
          cursor: "pointer",
        }}
      >
        New Room
      </button>
    </div>

    <div>
      <Editor
        height="80vh"
        defaultLanguage="python"
        defaultValue="# start coding here"
        theme="vs-dark"
        onMount={handleMount}
        onChange={handleChange}
      />
    </div>
  </>
)
    
  
}

export default App