package com.adib.keytrace;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    @GetMapping("/api/rooms")
    public String createRoom() {
        String roomId = UUID.randomUUID().toString();   // generate a random unique ID
        return roomId;
    }
}