package com.adib.keytrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.WebSocketSession;

public class Room {

    private final String roomId;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final List<Snapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
    private String latestCode = "";
    private boolean ended = false;

    // constructor (fixed: no "static")
    public Room(String roomId) {
        this.roomId = roomId;
    }

    // ---------- people ----------

    // yours
    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    // fixed: no parameter, returns the field
    public Set<WebSocketSession> getSessions() {
        return sessions;
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    // ---------- code ----------

    // fixed: no parameter, returns the field
    public String getLatestCode() {
        return latestCode;
    }

    // yours
    public void updateCode(String code) {
        latestCode = code;
        Snapshot snapshot = new Snapshot(latestCode, System.currentTimeMillis());
        snapshots.add(snapshot);
    }

    // ---------- recording ----------

    public List<Snapshot> getSnapshots() {
        synchronized (snapshots) {
            return new ArrayList<>(snapshots);   // a copy, safe while people keep typing
        }
    }

    // ---------- ending ----------

    public boolean isEnded() {
        return ended;
    }

    public void end() {
        ended = true;
    }

    public String getRoomId() {
        return roomId;
    }
}