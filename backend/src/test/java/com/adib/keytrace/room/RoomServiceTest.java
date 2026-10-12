package com.adib.keytrace.room;

import static org.junit.jupiter.api.Assertions.*;   // assertTrue, assertEquals, ...
import static org.mockito.Mockito.mock;             // fake repositories
import org.junit.jupiter.api.Test;                  // the @Test label
import com.adib.keytrace.api.CreatedRoom;
import com.adib.keytrace.persistence.RoomRepository;
import com.adib.keytrace.persistence.SnapshotRepository;

class RoomServiceTest {

    // a service with fake (mock) repositories, so no real database is needed
    private RoomService newService() {
        return new RoomService(mock(RoomRepository.class), mock(SnapshotRepository.class));
    }

    // ---------- creating rooms ----------

    @Test
    void createRoomReturnsIdAndToken() {
        // Arrange
        RoomService service = newService();

        // Act
        CreatedRoom created = service.createRoom(RoomMode.SOLO);

        // Assert: both values exist, and the room ID is 8 characters
        assertNotNull(created.roomId());
        assertNotNull(created.hostToken());
        assertEquals(8, created.roomId().length());
    }

    @Test
    void eachRoomGetsItsOwnToken() {
        RoomService service = newService();

        CreatedRoom first = service.createRoom(RoomMode.GROUP);
        CreatedRoom second = service.createRoom(RoomMode.GROUP);

        // two rooms must never share a host token (or one host could end the other's room)
        assertNotEquals(first.hostToken(), second.hostToken());
    }

    // ---------- ending sessions (only the host can do it) ----------

    @Test
    void endSessionWithCorrectTokenSucceeds() {
        RoomService service = newService();
        CreatedRoom created = service.createRoom(RoomMode.INTERVIEW);

        boolean ended = service.endSession(created.roomId(), created.hostToken());

        assertTrue(ended);
    }

    @Test
    void endSessionWithWrongTokenIsRefused() {
        RoomService service = newService();
        CreatedRoom created = service.createRoom(RoomMode.INTERVIEW);

        boolean ended = service.endSession(created.roomId(), "wrong-token");

        assertFalse(ended);
    }

    @Test
    void endSessionWithNoTokenIsRefused() {
        RoomService service = newService();
        CreatedRoom created = service.createRoom(RoomMode.INTERVIEW);

        // a participant's browser has no token, so it sends null
        boolean ended = service.endSession(created.roomId(), null);

        assertFalse(ended);
    }

    @Test
    void endSessionForUnknownRoomIsRefused() {
        RoomService service = newService();

        boolean ended = service.endSession("no-such-room", "any-token");

        assertFalse(ended);
    }
}
