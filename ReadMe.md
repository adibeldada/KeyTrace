# Status: IN PROGRESS

# KeyTrace
 
## Overview

KeyTrace is a web app for practicing coding interviews. Users solve problems in a shared, browser-based code editor, problems can be solved either alone, with a friend or with a tutor that act as an interviewer. Every Keystroke is recorded, so after a session is done, the user can retrace their steps and watch exactly how they solved their problem to find their weaknesses, just like how athletes review game footage. 

# Key Features

## Features

### Core (in progress)

- Create a session room and share it with a link
- Live shared code editor with real-time cursors
- Built-in practice problems, or add your own
- Run code in a secure sandbox with shared output
- Every keystroke recorded with timestamps
- Replay sessions with play, pause, speed control and scrubbing

### Planned

- User accounts and saved session history
- Countdown timer
- Built-in KeyTrace AI that analyzes performance
- Session breakdown: time spent reading, coding, and debugging
- paste detection for large blocks of code
- room chat

### Tech Stack

- **Frontend:** React, TypeScript, Monaco Editor
- **Backend:** Java, Spring Boot, WebSockets
- **Database:** PostgreSQL
- **Code Execution:** Docker (sandboxed containers)
- **Testing:** JUnit, Testcontainers, Playwright
- **CI/CD:** GitHub Actions
- **Deployment:** AWS (planned)

## Roadmap
 
### ✅ v0.1: Live collaboration
- [x] Real-time shared editor (Spring Boot WebSockets + React/Monaco)
- [x] Rooms with shareable links
- [x] "New Room" button with random room IDs
- [x] Late joiners receive current code

### ✅ v0.2: Recording & replay
- [x] Keystroke recording with timestamps
- [x] Session replay with timeline slider, play/pause and scrubbing

### ✅ v0.3: Sessions
- [x] End session (ends for everyone in the room, switches to replay)
- [x] Structured JSON messages with a `type` field
- [x] Live player count
- [x] Refactor room state into a `Room` class
- [x] Rooms kept after everyone leaves, so sessions can be replayed

### ✅ v0.4: Modes & host
- [x] Session modes: Solo, Interview, Group
- [x] Player limits per mode (room full / room not found)
- [x] Home screen to choose a mode
- [x] Host role via host token (only the host can end the session)

### ✅ v0.5: Quality
- [x] Unit tests (JUnit + Mockito) for room and handler logic
- [x] GitHub Actions CI

### v0.6: Persistence
- [ ] PostgreSQL persistence (rooms and recordings survive restarts)
- [ ] Integration tests against a real PostgreSQL database with Testcontainers

### v0.7: Coding tools
- [ ] Run code in a sandboxed Docker container
- [ ] Problem panel with built-in practice problems

### v0.8: Accounts
- [ ] User accounts (login)
- [ ] Saved session history
- [ ] Host linked to the logged-in creator (guests can still join by link)

### v0.9: Interview features
- [ ] Interviewer and candidate roles
- [ ] Private interviewer notes
- [ ] Session timer

### v1.0: End-to-end tests & deployment
- [ ] Playwright tests for the main flows (create room, host vs participant, live sync, ending a session, running code)
- [ ] Run Playwright tests in GitHub Actions
- [ ] Deploy to AWS

### Future ideas
- [ ] Store edits as changes instead of full snapshots (smaller recordings)
- [ ] Backend endpoint to fetch a single moment via binary search (for long sessions)
- [ ] Conflict-free concurrent editing (sync engine) and live cursors
- [ ] Session insights: time spent, longest pauses, paste detection
- [ ] Room chat
 