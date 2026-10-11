package com.adib.keytrace;

import org.springframework.data.jpa.repository.JpaRepository;

// Saves and loads RoomRecord rows. The ID type is String, matching RoomRecord's @Id.
public interface RoomRepository extends JpaRepository<RoomRecord, String> {
}