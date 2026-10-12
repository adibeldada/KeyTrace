package com.adib.keytrace.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import com.adib.keytrace.api.CreatedRoom;
import com.adib.keytrace.persistence.RoomRecord;
import com.adib.keytrace.persistence.RoomRepository;
import com.adib.keytrace.persistence.SnapshotRecord;
import com.adib.keytrace.persistence.SnapshotRepository;

// All the room logic in one place: the live rooms in memory, and saving/loading them in the database.
// Both the WebSocket handler and the controller use this class.
@Service
public class RoomService {

    // All live rooms (in memory). Key: room ID, Value: the live Room
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private final RoomRepository roomRepository;            // the "rooms" table
    private final SnapshotRepository snapshotRepository;    // the "snapshots" table

    // Spring passes in the two repositories it created at startup
    public RoomService(RoomRepository roomRepository, SnapshotRepository snapshotRepository) {
        this.roomRepository = roomRepository;
        this.snapshotRepository = snapshotRepository;
    }

    // Creates a new room with the chosen mode and returns its ID and host token
    public CreatedRoom createRoom(RoomMode mode) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);   // a random 8-character ID
        String hostToken = UUID.randomUUID().toString();
        rooms.put(roomId, new Room(roomId, mode, hostToken));           // live room, in memory
        roomRepository.save(new RoomRecord(roomId, mode, hostToken));   // saved copy, in the database
        return new CreatedRoom(roomId, hostToken);
    }

    // Finds a live room in memory only (for people who already joined). Returns null if not there.
    public Room findRoom(String roomId) {
        return rooms.get(roomId);
    }

    // Finds a room in memory, or loads it from the database (e.g. after a restart).
    // Returns null if the room doesn't exist anywhere.
    public Room findOrLoadRoom(String roomId) {
        // 1. look in memory
        Room room = rooms.get(roomId);
        if (room != null) {
            return room;
        }

        // 2. not in memory, so look in the database
        RoomRecord record = roomRepository.findById(roomId).orElse(null);
        if (record == null) {
            return null;                      // not in the database either: the room doesn't exist
        }

        // 3. rebuild the live room from its saved data
        Room loaded = new Room(record.getRoomId(), record.getRoomMode(), record.getHostToken());
        loaded.restoreLatestCode(record.getLatestCode());
        if (record.getEnded()) {
            loaded.end();
        }

        // 4. keep it in memory, so next time step 1 finds it
        rooms.put(roomId, loaded);
        return loaded;
    }

    // Someone typed: update the live room, record the moment, and save the latest code
    public void recordCode(Room room, String code) {
        room.updateCode(code);                                                                        // in memory
        snapshotRepository.save(new SnapshotRecord(room.getRoomId(), code, System.currentTimeMillis())); // record this moment
        roomRepository.findById(room.getRoomId()).ifPresent(record -> {                               // update the saved latest code
            record.setLatestCode(code);
            roomRepository.save(record);
        });
    }

    // Gets a room's recording from the database, in time order
    public List<Snapshot> getSnapshots(String roomId) {
        List<SnapshotRecord> records = snapshotRepository.findByRoomIdOrderByTimestampAsc(roomId);

        List<Snapshot> result = new ArrayList<>();
        for (SnapshotRecord record : records) {
            result.add(new Snapshot(record.getCode(), record.getTimestamp()));   // code → message, timestamp → currentTime
        }
        return result;
    }

    // Ends the session for everyone in the room: marks it ended and disconnects them all.
    // Only the host (the right token) can do it.
    public boolean endSession(String roomId, String token) {
        Room room = findOrLoadRoom(roomId);   // works after a restart too
        if (room == null) {
            return false;                     // no such room, nothing to end
        }

        if (token == null || !token.equals(room.getHostToken())) {
            return false;                     // only the host can end the session
        }

        room.end();                           // mark as ended first, so nothing new gets in

        roomRepository.findById(roomId).ifPresent(record -> {   // save "ended" in the database too
            record.setEnded(true);
            roomRepository.save(record);
        });

        for (WebSocketSession s : room.getSessions()) {
            try {
                s.close(new CloseStatus(4000, "Session ended"));   // 4000 = our "session ended" code
            } catch (Exception e) {
                System.err.println("Error closing session: " + s.getId());
            }
        }

        return true;
    }
}