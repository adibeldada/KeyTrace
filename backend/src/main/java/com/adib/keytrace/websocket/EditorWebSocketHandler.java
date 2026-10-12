package com.adib.keytrace.websocket;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.adib.keytrace.room.Role;
import com.adib.keytrace.room.Room;
import com.adib.keytrace.room.RoomService;

// Handles the live WebSocket side of a room: people joining, typing, and leaving.
// Anything about rooms or the database is done by RoomService.
@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    private final RoomService roomService;   // finds, loads, and saves rooms

    // Spring passes in the RoomService it created at startup
    public EditorWebSocketHandler(RoomService roomService) {
        this.roomService = roomService;
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

        // 1. room must exist (in memory, or loaded back from the database after a restart)
        Room room = roomService.findOrLoadRoom(roomId);
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
        if (getParam(session, "token") != null && getParam(session, "token").equals(room.getHostToken())) {
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

        Room room = roomService.findRoom(roomId);   // they joined, so the room is in memory
        if (room == null) {
            return;
        }

        if (room.isEnded()) {
            return;   // session is over, so ignore any edits
        }

        EditorMessage msg = mapper.readValue(message.getPayload(), EditorMessage.class);

        if ("code".equals(msg.type())) {
            roomService.recordCode(room, msg.text());   // update the room, record the moment, save to the database

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

        Room room = roomService.findRoom(roomId);
        if (room == null) {
            return;
        }

        room.removeSession(session);           // remove this person from the room
        int count = room.getSessions().size();
        EditorMessage msg = new EditorMessage("count", String.valueOf(count));
        broadcast(room, msg);

        System.out.println("disconnected: " + session.getId());
    }

    // Sends one message to everyone in the room
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