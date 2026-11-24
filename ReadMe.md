# Status: IN PROGRESS

# KeyTrace

## Overview

KeyTrace is a web app for practicing coding interviews. Users solve problems in a shared, browser-based code editor, either alone, with a friend, or with a tutor acting as the interviewer. Every keystroke is recorded, so after a session the user can retrace their steps and watch exactly how they solved the problem to find their weaknesses, just like athletes reviewing game footage.

## Features

### Built
- Create a session room and share it with a link
- Live shared code editor
- Session modes (Solo, Interview, Group) with a host who controls the session
- Every keystroke recorded with timestamps
- Replay sessions with play, pause, speed control and scrubbing
- Rooms and recordings saved in PostgreSQL, so they survive restarts

### In progress
- Live collaborator cursors
- Run code in a secure sandbox with shared output
- Built-in practice problems

### Planned
- User accounts and saved session history
- Countdown timer
- Built-in KeyTrace AI that analyzes performance
- Session breakdown: time spent reading, coding, and debugging
- Paste detection for large blocks of code
- Room chat

## Tech Stack

- **Frontend:** React, TypeScript, Monaco Editor
- **Backend:** Java, Spring Boot, WebSockets
- **Database:** PostgreSQL (Spring Data JPA)
- **Code Execution:** Docker sandboxed containers (in progress)
- **Testing:** JUnit, Mockito, Testcontainers, Playwright (planned)
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

### ✅ v0.6: Persistence
- [x] PostgreSQL persistence (rooms and recordings survive restarts)
- [x] Refactor: split room logic into a `RoomService` and organize code into packages
- [x] Integration tests against a real PostgreSQL database with Testcontainers
- [x] Cut database queries per keystroke from 4 to 2 (direct update + one transaction)

### v0.7: Coding tools
- [x] Live collaborator cursors
- [ ] Run code in a sandboxed Docker container (no network, CPU/memory/time limits)
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

### Performance (measured in BENCHMARKS.md)
- [ ] Benchmarks: sync latency, load test, recording size and replay load time
- [ ] Index snapshots by room
- [ ] Batch snapshot writes instead of one insert per keystroke
- [ ] Store edits as changes instead of full snapshots (smaller recordings)
- [ ] Load long replays in pages, or fetch a single moment from the backend

### Future ideas
- [ ] Conflict-free concurrent editing (sync engine)
- [ ] Session insights: time spent, longest pauses, paste detection
- [ ] User-added practice problems
- [ ] Room chat