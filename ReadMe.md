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
- [x] Empty rooms cleaned up automatically

### ✅ v0.2: Recording & replay
- [x] Keystroke recording with timestamps
- [x] Session replay with timeline slider, play/pause and scrubbing

### 🚧 v0.3: Sessions
- [ ] End session (ends for everyone in the room, switches to replay)
- [ ] Structured JSON messages with a `type` field
- [ ] Live player count
- [ ] Refactor room state into a `Room` class

### v0.4: Modes & roles
- [ ] Session modes: Solo, Interview/Training, Group
- [ ] Host role (only the host can end the session)
- [ ] Interviewer and candidate roles, private interviewer notes
- [ ] Session timer

### v0.5: Coding tools
- [ ] Run code in a sandboxed Docker container
- [ ] Problem panel with built-in practice problems

### v0.6: Production-ready
- [ ] PostgreSQL persistence (sessions survive restarts)
- [ ] Tests (JUnit, Testcontainers) + GitHub Actions CI
- [ ] Deploy to AWS

### Future ideas
- [ ] Store edits as changes instead of full snapshots (smaller recordings)
- [ ] Backend endpoint to fetch a single moment via binary search (for long sessions)
- [ ] Conflict-free concurrent editing (sync engine) and live cursors
- [ ] User accounts and saved session history
- [ ] Session insights: time spent, longest pauses, paste detection