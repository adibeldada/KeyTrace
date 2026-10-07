package com.adib.keytrace;

import static org.junit.jupiter.api.Assertions.*;   // assertTrue, assertEquals, ...
import org.junit.jupiter.api.Test;                  // the @Test label

class EditorWebSocketHandlerTest {

    // ---------- creating rooms ----------

    @Test
    void createRoomReturnsIdAndToken() {
        // Arrange
        EditorWebSocketHandler handler = new EditorWebSocketHandler();

        // Act
        CreatedRoom created = handler.createRoom(RoomMode.SOLO);

        // Assert: both values exist, and the room ID is 8 characters
        assertNotNull(created.roomId());
        assertNotNull(created.hostToken());
        assertEquals(8, created.roomId().length());
    }

    @Test
    void eachRoomGetsItsOwnToken() {
        EditorWebSocketHandler handler = new EditorWebSocketHandler();

        CreatedRoom first = handler.createRoom(RoomMode.GROUP);
        CreatedRoom second = handler.createRoom(RoomMode.GROUP);

        // two rooms must never share a host token (or one host could end the other's room)
        assertNotEquals(first.hostToken(), second.hostToken());
    }

    // ---------- ending sessions (only the host can do it) ----------

    @Test
    void endSessionWithCorrectTokenSucceeds() {
        EditorWebSocketHandler handler = new EditorWebSocketHandler();
        CreatedRoom created = handler.createRoom(RoomMode.INTERVIEW);

        boolean ended = handler.endSession(created.roomId(), created.hostToken());

        assertTrue(ended);
    }

    @Test
    void endSessionWithWrongTokenIsRefused() {
        EditorWebSocketHandler handler = new EditorWebSocketHandler();
        CreatedRoom created = handler.createRoom(RoomMode.INTERVIEW);

        boolean ended = handler.endSession(created.roomId(), "wrong-token");

        assertFalse(ended);
    }

    @Test
    void endSessionWithNoTokenIsRefused() {
        EditorWebSocketHandler handler = new EditorWebSocketHandler();
        CreatedRoom created = handler.createRoom(RoomMode.INTERVIEW);

        // a participant's browser has no token, so it sends null
        boolean ended = handler.endSession(created.roomId(), null);

        assertFalse(ended);
    }

    @Test
    void endSessionForUnknownRoomIsRefused() {
        EditorWebSocketHandler handler = new EditorWebSocketHandler();

        boolean ended = handler.endSession("no-such-room", "any-token");

        assertFalse(ended);
    }
}