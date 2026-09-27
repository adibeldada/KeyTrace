package com.adib.keytrace;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // runs when a browser connects
        System.out.println("connected: " + session.getId()); // get the session id
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // runs when a browser sends a message
        System.out.println("message received: " + message.getPayload()); // print the received message
        session.sendMessage(new TextMessage("Echo: " + message.getPayload())); // send a message back to the browser
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // runs when a browser disconnects
        System.out.println("disconnected: " + session.getId()); // get the session id
    }
}