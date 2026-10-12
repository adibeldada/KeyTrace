package com.adib.keytrace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.adib.keytrace.room.RoomMode;

@Entity
@Table(name = "rooms")
public class RoomRecord {
    @Id
    private String roomId;
    @Enumerated(EnumType.STRING)
    private RoomMode mode;
    @Column(nullable = false)
    private String hostToken;
    @Column(columnDefinition = "TEXT")
    private String latestCode = "";
    private boolean ended = false;

    public RoomRecord(String roomId, RoomMode mode, String hostToken){
        this.roomId = roomId;
        this.mode = mode;
        this.hostToken = hostToken;
    }

    protected RoomRecord(){
    }

    public String getRoomId(){
        return roomId;
    }
    public RoomMode getRoomMode(){
        return mode;
    }
    public String getHostToken(){
        return hostToken;
    }
    public String getLatestCode(){
        return latestCode;
    }
    public boolean getEnded(){
        return ended;
    }
    public void setLatestCode(String setlatestCode){
        latestCode = setlatestCode;
    }
    public void setEnded(boolean setended){
        ended = setended;
    }

}