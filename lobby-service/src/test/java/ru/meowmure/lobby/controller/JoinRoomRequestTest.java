package ru.meowmure.lobby.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JoinRoomRequestTest {

    @Test
    void gettersAndSetters() {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setRoomName("Poker");
        request.setPassword("secret");

        assertEquals("Poker", request.getRoomName());
        assertEquals("secret", request.getPassword());
    }
}
