package com.adib.keytrace;

public enum RoomMode {
    SOLO(1),
    INTERVIEW(2),
    GROUP(10);

    private final int maxPlayers;

    RoomMode(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMaxPlayers(){
        return maxPlayers;
    }
}