package com.adib.keytrace;

import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    @PostMapping("/api/rooms")
    public String createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0,8);   // generate a random unique ID
        return roomId;
    }
}