package com.adib.keytrace;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    // All live rooms on the server (in memory).
    // Key (String): the room ID from the URL, e.g. "abc" in ?room=abc
    // Value (Room): that room's live data: who's connected, their roles, latest code, ended status
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private final RoomRepository roomRepository;            // v0.6: saves/loads rows in the "rooms" table
    private final SnapshotRepository snapshotRepository;    // v0.6: saves/loads rows in the "snapshots" table

    // Spring passes in the two repositories it created at startup
    public EditorWebSocketHandler(RoomRepository roomRepository, SnapshotRepository snapshotRepository) {
        this.roomRepository = roomRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("connected: " + session.getId());
        String roomId = getParam(session, "room");
        if (roomId == null) {
            try {
                session.close();
            } catch (Exception e) {
                System.err.println("Error closing session: " + session.getId());
            }
            return;
        }

        // 1. room must exist (v0.6: in memory, or loaded back from the database after a restart)
        Room room = findOrLoadRoom(roomId);
        if (room == null) {
            try {
                session.close(new CloseStatus(4004, "Room not found"));
            } catch (Exception e) {
                System.err.println("Error closing session: " + session.getId());
            }
            return;
        }

        // 2. ended room → send them to the replay
        if (room.isEnded()) {
            try {
                session.close(new CloseStatus(4000, "Session ended"));
            } catch (Exception e) {
                System.err.println("Error closing session: " + session.getId());
            }
            return;
        }

        // 3. full room → reject
        int roomCount = room.getSessions().size();
        int maxCount = room.getMode().getMaxPlayers();
        if (roomCount >= maxCount) {
            try {
                session.close(new CloseStatus(4003, "Room full"));
            } catch (Exception e) {
                System.err.println("Error closing session: " + session.getId());
            }
            return;
        }

        // 4. all checks passed → join the room
        room.addSession(session);

        // 4b. is this the host? (their token matches the room's), if not the host make them a participant
        if (getParam(session, "token") != null && getParam(session, "token").equals(room.getHostToken())){
            room.setRole(session, Role.HOST);
        } else {
            room.setRole(session, Role.PARTICIPANT);
        }

        // 4c. tell this person their role, so the frontend can show it
        String role;
        if (room.isHost(session)) {
            role = "HOST";
        } else {
            role = "PARTICIPANT";
        }

        try {
            EditorMessage out = new EditorMessage("role", role);
            String json = mapper.writeValueAsString(out);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            System.err.println("Error sending role to session: " + session.getId());
        }

        // 4d. tell this person the room's mode
        String mode = room.getMode().name();
        try {
            EditorMessage out = new EditorMessage("mode", mode);
            String json = mapper.writeValueAsString(out);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            System.err.println("Error sending mode to session: " + session.getId());
        }

        // 5. late joiner gets the current code
        if (!room.getLatestCode().isEmpty()) {
            try {
                EditorMessage out = new EditorMessage("code", room.getLatestCode());
                String json = mapper.writeValueAsString(out);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.err.println("Error sending saved code to session: " + session.getId());
            }
        }

        // 6. tell everyone the new player count
        int count = room.getSessions().size();
        EditorMessage msg = new EditorMessage("count", String.valueOf(count));
        broadcast(room, msg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("message received: " + message.getPayload());
        String roomId = getParam(session, "room");

        if (roomId == null) {
            return;
        }

        Room room = rooms.get(roomId);        // find this person's room (they joined, so it's in memory)
        if (room == null) {
            return;
        }

        if (room.isEnded()) {
            return;   // session is over, so ignore any edits
        }

        EditorMessage msg = mapper.readValue(message.getPayload(), EditorMessage.class);

        if ("code".equals(msg.type())) {
            room.updateCode(msg.text());                                                                   // in memory
            snapshotRepository.save(new SnapshotRecord(roomId, msg.text(), System.currentTimeMillis()));   // v0.6: record this moment
            roomRepository.findById(roomId).ifPresent(record -> {                                          // v0.6: update the saved latest code
                record.setLatestCode(msg.text());
                roomRepository.save(record);
            });

            for (WebSocketSession s : room.getSessions()) {   // send the edit to everyone else in the room
                if (!s.getId().equals(session.getId()) && s.isOpen()) {
                    try {
                        s.sendMessage(message);
                    } catch (Exception e) {
                        System.err.println("Error sending to session: " + s.getId());
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = getParam(session, "room");

        // this session never joined a room, so there's nothing to clean up
        if (roomId == null) {
            return;
        }

        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.removeSession(session);           // remove this person from the room
        int count = room.getSessions().size();
        EditorMessage msg = new EditorMessage("count", String.valueOf(count));
        broadcast(room, msg);

        System.out.println("disconnected: " + session.getId());
    }

    // v0.6: gets a room's recording from the database, in time order
    public List<Snapshot> getSnapshot(String roomId) {
        List<SnapshotRecord> records = snapshotRepository.findByRoomIdOrderByTimestampAsc(roomId);

        List<Snapshot> result = new ArrayList<>();
        for (SnapshotRecord record : records) {
            result.add(new Snapshot(record.getCode(), record.getTimestamp()));   // code → message, timestamp → currentTime
        }
        return result;
    }

    // Ends the session for everyone in the room: marks it ended and disconnects them all
    public boolean endSession(String roomId, String token) {
        Room room = findOrLoadRoom(roomId);   // v0.6: works after a restart too
        if (room == null) {
            return false;                     // no such room, nothing to end
        }

        if (token == null || !token.equals(room.getHostToken())) {
            return false;                     // only the host can end the session
        }

        room.end();                           // mark as ended first, so nothing new gets in

        roomRepository.findById(roomId).ifPresent(record -> {   // v0.6: save "ended" in the database too
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

    private void broadcast(Room room, EditorMessage msg) {
        String json;
        try {
            json = mapper.writeValueAsString(msg);
        } catch (Exception e) {
            System.err.println("Error converting message to JSON");
            return;
        }

        for (WebSocketSession s : room.getSessions()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    System.err.println("Error sending to session: " + s.getId());
                }
            }
        }
    }

    // Creates a new room with the chosen mode and returns its ID and host token
    public CreatedRoom createRoom(RoomMode mode) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);   // a random 8-character ID
        String hostToken = UUID.randomUUID().toString();
        rooms.put(roomId, new Room(roomId, mode, hostToken));           // live room, in memory
        roomRepository.save(new RoomRecord(roomId, mode, hostToken));   // v0.6: saved copy, in the database
        return new CreatedRoom(roomId, hostToken);
    }

    // v0.6: finds a room in memory, or loads it from the database (e.g. after a restart).
    // Returns null if the room doesn't exist anywhere.
    private Room findOrLoadRoom(String roomId) {
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

    // Reads one value from the WebSocket URL by name, e.g. "room" or "token".
    // Returns null if that name isn't in the URL.
    private String getParam(WebSocketSession session, String name) {
        return UriComponentsBuilder
                .fromUri(session.getUri())   // take the full URL
                .build()                     // let Spring split it into parts
                .getQueryParams()            // just the name=value pairs after "?"
                .getFirst(name);             // the value for this name, or null
    }
}