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