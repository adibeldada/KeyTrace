package com.adib.keytrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.socket.WebSocketSession;

public class Room {

    private final String roomId;
    private final Map<WebSocketSession, Role> sessions = new ConcurrentHashMap<>();
    private final List<Snapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
    private final RoomMode mode;
    private final String hostToken;
    private String latestCode = "";
    private boolean ended = false;


    // constructor (fixed: no "static")
    public Room(String roomId, RoomMode mode, String hostToken) {
        this.roomId = roomId;
        this.mode = mode;
        this.hostToken = hostToken;
    }

    // ---------- people ----------

    // everyone joins as a participant; the handler upgrades the host with setRole
    public void addSession(WebSocketSession session) {
        sessions.put(session, Role.PARTICIPANT);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    // returns just the keys (the sessions), so the handler doesn't need to change
    public Set<WebSocketSession> getSessions() {
        return sessions.keySet();
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

    // -------- room mode ---------

    public RoomMode getMode() {
        return mode;
    }

    // -------- roles ---------

    public void setRole(WebSocketSession session, Role roleid){
        sessions.put(session, roleid);

    }

    public boolean isHost(WebSocketSession session){
        return sessions.get(session) == Role.HOST;
    }

    public String getHostToken(){
        return hostToken;
    }
}