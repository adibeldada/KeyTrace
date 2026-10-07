package com.adib.keytrace;

import static org.junit.jupiter.api.Assertions.*;   // assertFalse, assertEquals, ...
import static org.mockito.Mockito.mock;             // mock(...)
import org.junit.jupiter.api.Test;                  // the @Test label
import org.springframework.web.socket.WebSocketSession;

class RoomTest {

    @Test
    void newSessionStartsAsParticipant() {
        // Arrange: set up the situation
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");
        WebSocketSession session = mock(WebSocketSession.class);

        // Act: do the ONE thing you're testing
        room.addSession(session);

        // Assert: check the result
        assertFalse(room.isHost(session));
    }

    @Test
    void hostCanBeSetWithSetRole() {
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");
        WebSocketSession session = mock(WebSocketSession.class);

        room.addSession(session);
        room.setRole(session, Role.HOST);

        assertTrue(room.isHost(session));
    }

    @Test
    void removeSessionLeavesRoomEmpty() {
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");
        WebSocketSession session = mock(WebSocketSession.class);

        room.addSession(session);
        room.removeSession(session);

        assertTrue(room.isEmpty());
    }

    @Test
    void updateCodeSavesLatestCode() {
        // Arrange: just a room, no people needed for code updates
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");

        // Act: two updates, so we can check it keeps the LATEST one
        room.updateCode("print(1)");
        room.updateCode("print(2)");

        // Assert: assertEquals(expected, actual)
        assertEquals("print(2)", room.getLatestCode());
    }

    @Test
    void updateCodeRecordsSnapshot() {
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");

        // Act: every update should add one snapshot to the recording
        room.updateCode("print(1)");
        room.updateCode("print(2)");

        // Assert: 2 updates → 2 snapshots
        assertEquals(2, room.getSnapshots().size());
    }

    @Test
    void endMarksRoomAsEnded() {
        Room room = new Room("abc", RoomMode.GROUP, "secret-token");

        // a brand-new room should NOT be ended yet
        assertFalse(room.isEnded());

        // Act
        room.end();

        // Assert: now it should be ended
        assertTrue(room.isEnded());
    }
}