import { createRoom } from '../api/api'

// The three "create a room" buttons, used on the home screen and inside a room
export default function ModeButtons() {
  // mode is "SOLO" | "INTERVIEW" | "GROUP" (must match the Java enum names)
  async function handleNewRoom(mode: string) {
    const roomId = await createRoom(mode)       // asks the backend for a new room (and saves our host token)
    window.location.href = "/?room=" + roomId   // go to it
  }

  return (
    <>
      <button onClick={() => handleNewRoom("SOLO")} className="button">New Solo</button>
      <button onClick={() => handleNewRoom("INTERVIEW")} className="button">New Interview</button>
      <button onClick={() => handleNewRoom("GROUP")} className="button">New Group</button>
    </>
  )
}