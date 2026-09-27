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
    <div>
      <h1>KeyTrace, room: {room}</h1>
      <Editor
        height="80vh"
        defaultLanguage="python"
        defaultValue="# start coding here"
        theme="vs-dark"
        onMount={handleMount}
        onChange={handleChange}
      />
    </div>
  )
}

export default App