package com.adib.keytrace.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import com.adib.keytrace.room.RoomMode;
import com.adib.keytrace.room.RoomService;
import com.adib.keytrace.room.Snapshot;

// HTTP endpoints the frontend calls: create a room, get its replay, end it.
// The actual room logic is in RoomService.
@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomService roomService;

    // Spring passes in the RoomService it created at startup
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/api/rooms")
    public CreatedRoom createRoom(@RequestParam String mode) {
        RoomMode roomMode = RoomMode.valueOf(mode);   // "SOLO" → RoomMode.SOLO
        return roomService.createRoom(roomMode);      // create the room, return its ID and host token
    }

    @GetMapping("/api/rooms/{roomId}/snapshots")
    public List<Snapshot> replay(@PathVariable String roomId) {
        return roomService.getSnapshots(roomId);
    }

    @PostMapping("/api/rooms/{roomId}/end")
    public ResponseEntity<Void> endSession(@PathVariable String roomId,
                                           @RequestParam(required = false) String token) {
        boolean ended = roomService.endSession(roomId, token);
        if (ended) {
            return ResponseEntity.ok().build();                          // 200 OK
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();      // 403 Forbidden
    }
}