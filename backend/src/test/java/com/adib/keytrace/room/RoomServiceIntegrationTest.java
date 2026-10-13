package com.adib.keytrace.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.adib.keytrace.TestcontainersConfiguration;
import com.adib.keytrace.api.CreatedRoom;
import com.adib.keytrace.persistence.RoomRecord;
import com.adib.keytrace.persistence.RoomRepository;
import com.adib.keytrace.persistence.SnapshotRecord;
import com.adib.keytrace.persistence.SnapshotRepository;

// Tests RoomService against a REAL PostgreSQL database (started by Testcontainers).
// Each test creates its own room (random ID), so tests don't affect each other.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RoomServiceIntegrationTest {

    @Autowired RoomService roomService;                 // the real service, built by Spring
    @Autowired RoomRepository roomRepository;           // to look directly at the "rooms" table
    @Autowired SnapshotRepository snapshotRepository;   // to look directly at the "snapshots" table

    @Test
    void createRoomSavesRoomInDatabase() {
        // Act: create a room through the service
        CreatedRoom created = roomService.createRoom(RoomMode.INTERVIEW);

        // Assert: the row is really in the database, with the right data
        RoomRecord record = roomRepository.findById(created.roomId()).orElseThrow();
        assertEquals(RoomMode.INTERVIEW, record.getRoomMode());
        assertEquals(created.hostToken(), record.getHostToken());
        assertFalse(record.getEnded());
    }

    @Test
    void recordCodeSavesSnapshotsAndLatestCode() {
        // Arrange: a room, and its live Room object (recordCode needs it)
        CreatedRoom created = roomService.createRoom(RoomMode.INTERVIEW);
        Room room = roomService.findOrLoadRoom(created.roomId());

        // Act: someone types twice
        roomService.recordCode(room, "a");
        roomService.recordCode(room, "ab");

        // Assert 1: two snapshot rows were saved for this room
        List<SnapshotRecord> snapshots = snapshotRepository.findByRoomIdOrderByTimestampAsc(created.roomId());
        assertEquals(2, snapshots.size());

        // Assert 2: the room's saved latest code is the LAST thing typed
        RoomRecord record = roomRepository.findById(created.roomId()).orElseThrow();
        assertEquals("ab", record.getLatestCode());
    }

    @Test
    void endSessionSavesEndedInDatabase() {
        // Arrange
        CreatedRoom created = roomService.createRoom(RoomMode.INTERVIEW);

        // Act: the host ends the session (correct token)
        boolean ended = roomService.endSession(created.roomId(), created.hostToken());

        // Assert: it worked, and "ended" is saved in the database
        assertTrue(ended);
        RoomRecord record = roomRepository.findById(created.roomId()).orElseThrow();
        assertTrue(record.getEnded());
    }

    @Test
    void getSnapshotsReturnsCodeInOrder() throws InterruptedException {
        // Arrange
        CreatedRoom created = roomService.createRoom(RoomMode.SOLO);
        Room room = roomService.findOrLoadRoom(created.roomId());

        // Act: type three times. The short pauses make sure each snapshot gets a different
        // timestamp (computers are fast enough that two calls can land in the same millisecond).
        roomService.recordCode(room, "a");
        Thread.sleep(5);
        roomService.recordCode(room, "ab");
        Thread.sleep(5);
        roomService.recordCode(room, "abc");

        List<Snapshot> replay = roomService.getSnapshots(created.roomId());

        // Assert: the replay has all three, oldest first
        assertEquals(3, replay.size());
        assertEquals("a", replay.get(0).message());
        assertEquals("ab", replay.get(1).message());
        assertEquals("abc", replay.get(2).message());
    }

    @Test
    void roomLoadsBackAfterRestart() {
        // Arrange: create a room and type something, with the normal service
        CreatedRoom created = roomService.createRoom(RoomMode.GROUP);
        Room room = roomService.findOrLoadRoom(created.roomId());
        roomService.recordCode(room, "print('hello')");

        // Act: simulate a server restart. A brand-new RoomService starts with EMPTY memory,
        // but it uses the same database, just like the real app after a restart.
        RoomService afterRestart = new RoomService(roomRepository, snapshotRepository);
        Room loaded = afterRestart.findOrLoadRoom(created.roomId());

        // Assert: the room came back from the database with its saved data
        assertNotNull(loaded);
        assertEquals(RoomMode.GROUP, loaded.getMode());
        assertEquals(created.hostToken(), loaded.getHostToken());
        assertEquals("print('hello')", loaded.getLatestCode());
        assertFalse(loaded.isEnded());
    }
}