package com.adib.keytrace;

import java.util.Map;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    // All active rooms on the server.
    // Key (String): the room ID from the URL, e.g. "abc" in ?room=abc
    // Value (Room): that room's data: its sessions, latest code, snapshots, and ended status
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // runs when a browser connects
        System.out.println("connected: " + session.getId()); // get the session id
        String query = session.getUri().getQuery(); // get the query parameters from the URL
        if (query == null) {
            try {
                session.close();
            } catch (Exception e){
                System.err.println("Error closing session" + session.getId());
            }
            return;
        }
        String roomId = query.split("=")[1]; // extract the room ID from the query parameters
        Room room = rooms.computeIfAbsent(roomId, k -> new Room(roomId));
        room.addSession(session);
        if (room.isEnded()) {
            try {
                session.close(new CloseStatus(4000, "Session ended"));   // late visitor: send them to replay
            } catch (Exception e) {
                System.err.println("Error closing session: " + session.getId());
            }
            return;
        }

        if (!room.getLatestCode().isEmpty()) {
            try {
                EditorMessage out = new EditorMessage("code", room.getLatestCode());   // 1. build the object
                String json = mapper.writeValueAsString(out);                         // 2. object → JSON text
                session.sendMessage(new TextMessage(json));                           // 3. send the JSON
            } catch (Exception e) {
                System.err.println("Error sending saved code to session: " + session.getId());
            }
        }

        int count = room.getSessions().size(); 
        EditorMessage msg = new EditorMessage("count",String.valueOf(count));
        broadcast(room, msg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("message received: " + message.getPayload());
        String query = session.getUri().getQuery();
        String roomId = query.split("=")[1];

        Room room = rooms.get(roomId);        // CHANGED: find this person's room
        if (room == null) {
            return;
        }

        if (room.isEnded()) {
            return;   // session is over, so ignore any edits
        }

        EditorMessage msg = mapper.readValue(message.getPayload(),EditorMessage.class);
        
        if ("code".equals(msg.type())){
            room.updateCode(msg.text()); // CHANGED: saves latest code AND records a snapshot
            for (WebSocketSession s : room.getSessions()) {   // CHANGED: loop over the room's people
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
        String query = session.getUri().getQuery();

        // this session never joined a room, so there's nothing to clean up
        if (query == null) {
            return;
        }

        String roomId = query.split("=")[1];
        Room room = rooms.get(roomId);         // CHANGED: find the room
        if (room == null) {
            return;
        }

        room.removeSession(session);           // CHANGED: remove this person from the room
        int count = room.getSessions().size();
        EditorMessage msg = new EditorMessage("count", String.valueOf(count));
        broadcast(room, msg);


        System.out.println("disconnected: " + session.getId());
    }

    public List<Snapshot> getSnapshot(String roomId) {
        Room room = rooms.get(roomId);         // CHANGED: find the room
        if (room == null) {
            return List.of();                  // no such room: empty recording
        }
        return room.getSnapshots();            // CHANGED: the room holds its own recording
    }

    // Ends the session for everyone in the room: marks it ended and disconnects them all
    public void endSession(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;                       // no such room, nothing to end
        }

        room.end();                       // mark as ended first, so nothing new gets in

        for (WebSocketSession s : room.getSessions()) {
            try {
                s.close(new CloseStatus(4000, "Session ended"));   // 4000 = our "session ended" code
            } catch (Exception e) {
                System.err.println("Error closing session: " + s.getId());
            }
        }   

    }

    private void broadcast(Room room, EditorMessage msg){
        String json;
        try {
            json = mapper.writeValueAsString(msg);
        } catch (Exception e){
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

}