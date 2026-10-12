package com.adib.keytrace.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Saves and loads SnapshotRecord rows. The ID type is Long, matching SnapshotRecord's @Id.
public interface SnapshotRepository extends JpaRepository<SnapshotRecord, Long> {

    // Spring builds the query from the method name:
    // SELECT * FROM snapshots WHERE room_id = ? ORDER BY timestamp ASC
    List<SnapshotRecord> findByRoomIdOrderByTimestampAsc(String roomId);
}