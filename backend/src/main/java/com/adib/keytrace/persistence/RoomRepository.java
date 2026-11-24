package com.adib.keytrace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

// Saves and loads RoomRecord rows. The ID type is String, matching RoomRecord's @Id.
public interface RoomRepository extends JpaRepository<RoomRecord, String> {

    // Changes only latest_code for one room: one UPDATE, no SELECTs first.
    @Transactional
    @Modifying
    @Query("update RoomRecord r set r.latestCode = :code where r.roomId = :roomId")
    void updateLatestCode(@Param("roomId") String roomId, @Param("code") String code);
}