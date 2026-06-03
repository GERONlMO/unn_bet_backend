package ru.meowmure.lobby.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateRoomRequestTest {

    @Test
    void gettersAndSetters() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Room");
        request.setPrivate(true);
        request.setPassword("pwd");
        request.setMaxPlayers(4);
        request.setMinBet(50L);
        request.setTurnTimeLimit(15000L);

        assertEquals("Room", request.getName());
        assertTrue(request.isPrivate());
        assertEquals("pwd", request.getPassword());
        assertEquals(4, request.getMaxPlayers());
        assertEquals(50L, request.getMinBet());
        assertEquals(15000L, request.getTurnTimeLimit());
    }
}
