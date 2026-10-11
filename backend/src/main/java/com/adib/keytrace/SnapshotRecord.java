package com.adib.keytrace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

// One recorded moment of a room. Spring creates a "snapshots" table from this class.
@Entity
@Table(name = "snapshots")
public class SnapshotRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database numbers the rows: 1, 2, 3...
    private Long id;

    @Column(nullable = false)                             // every snapshot belongs to a room
    private String roomId;

    @Column(columnDefinition = "TEXT")                    // code can be long
    private String code;

    private long timestamp;                               // milliseconds, from System.currentTimeMillis()

    // used by Hibernate when reading a row from the database
    protected SnapshotRecord() {
    }

    // used by our code when recording a new snapshot (no id: the database makes it)
    public SnapshotRecord(String roomId, String code, long timestamp) {
        this.roomId = roomId;
        this.code = code;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getCode() {
        return code;
    }

    public long getTimestamp() {
        return timestamp;
    }
}