package com.adib.keytrace;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> savedMessage = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // runs when a browser connects
        System.out.println("connected: " + session.getId()); // get the session id
        String query = session.getUri().getQuery(); // get the query parameters from the URL
        if (query == null) {
            return;
        }
        String roomId = query.split("=")[1]; // extract the room ID from the query parameters
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
        rooms.get(roomId).add(session); // add the session to the set of sessions

        if (savedMessage.containsKey(roomId)) {
            try {
                session.sendMessage(new TextMessage(savedMessage.get(roomId)));
            } catch (Exception e) {
                System.err.println("Error sending saved code to session: " + session.getId());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // runs when a browser sends a message
        System.out.println("message received: " + message.getPayload()); // print the received message
        String query = session.getUri().getQuery(); // get the query parameters from the URL
        String roomId = query.split("=")[1]; // extract the room ID from the query parameters

        savedMessage.put(roomId, message.getPayload());
        for (WebSocketSession s : rooms.get(roomId)) {
            if (!s.getId().equals(session.getId()) && s.isOpen()) {
                try {
                    s.sendMessage(message); // send the message to all other sessions
                } catch (Exception e) {
                    System.err.println("Error occurred while sending message to session: " + s.getId());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // runs when a browser disconnects
        String query = session.getUri().getQuery();

        // this session never joined a room, so there's nothing to clean up
        if (query == null) {
            try {
                session.close();
            } catch (Exception e){
                System.err.println("Error closing session" + session.getId());
            }
            return;
        }

        String roomId = query.split("=")[1];
        rooms.get(roomId).remove(session);

        // if the room is now empty, delete it and its saved code
        if (rooms.get(roomId).isEmpty()) {
            rooms.remove(roomId);
            savedMessage.remove(roomId);
        }

        System.out.println("disconnected: " + session.getId());
    }
}