package com.adib.keytrace;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final EditorWebSocketHandler handler;

    public RoomController(EditorWebSocketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/api/rooms")
    public CreatedRoom createRoom(@RequestParam String mode) {
        RoomMode roomMode = RoomMode.valueOf(mode);   // "SOLO" → RoomMode.SOLO
        return handler.createRoom(roomMode);          // create the room, return its ID
    }

    @GetMapping("/api/rooms/{roomId}/snapshots")
    public List<Snapshot> replay(@PathVariable String roomId){
        return handler.getSnapshot(roomId);

    }

    @PostMapping("/api/rooms/{roomId}/end")
    public ResponseEntity<Void> endSession(@PathVariable String roomId,
                                        @RequestParam(required = false) String token) {
        boolean ended = handler.endSession(roomId, token);
        if (ended) {
            return ResponseEntity.ok().build();                          // 200 OK
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();      // 403 Forbidden
    }
}