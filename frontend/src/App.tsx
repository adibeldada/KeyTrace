import Editor from '@monaco-editor/react'
import './App.css'

function App() {
  return (
    <div>
      <h1>KeyTrace</h1>
      <Editor
        height="80vh"
        defaultLanguage="python"
        defaultValue="# start coding here"
        theme="vs-dark"
      />
    </div>
  )
}

export default App