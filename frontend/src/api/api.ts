// Everything that talks to the backend lives here.
const API_URL = "http://localhost:8080"
const WS_URL = "ws://localhost:8080"

export type Snapshot = { message: string; currentTime: number }
type CreatedRoom = { roomId: string; hostToken: string }

// ---------- host token (our "key card" for a room, saved in the browser) ----------
export function getHostToken(roomId: string): string | null {
  return localStorage.getItem("keytrace-host-" + roomId)
}

function saveHostToken(roomId: string, token: string) {
  localStorage.setItem("keytrace-host-" + roomId, token)
}

// ---------- REST calls ----------
// Creates a room, saves our host token, and returns the new room's ID
export async function createRoom(mode: string): Promise<string> {
  const response = await fetch(API_URL + "/api/rooms?mode=" + mode, { method: "POST" })
  const data: CreatedRoom = await response.json()
  saveHostToken(data.roomId, data.hostToken)
  return data.roomId
}

// Ends the session. Returns false if we're not the host (403).
export async function endSession(roomId: string): Promise<boolean> {
  const token = getHostToken(roomId)
  const response = await fetch(API_URL + "/api/rooms/" + roomId + "/end?token=" + token, { method: "POST" })
  return response.ok
}

// Loads a room's whole recording for replay
export async function getSnapshots(roomId: string): Promise<Snapshot[]> {
  const response = await fetch(API_URL + "/api/rooms/" + roomId + "/snapshots")
  return response.json()
}

// ---------- WebSocket ----------
// The URL to join a room live (adds our host token if we have one)
export function roomSocketUrl(roomId: string): string {
  const token = getHostToken(roomId)
  let url = WS_URL + "/ws?room=" + roomId
  if (token) url = url + "&token=" + token
  return url
}