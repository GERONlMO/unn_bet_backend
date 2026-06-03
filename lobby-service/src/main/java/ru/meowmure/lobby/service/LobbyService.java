package ru.meowmure.lobby.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.service.LobbyRoomsCacheService;
import ru.meowmure.lobby.client.GameServiceClient;

import java.util.*;

@Service
public class LobbyService {

    private static final Logger log = LoggerFactory.getLogger(LobbyService.class);

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GameServiceClient gameServiceClient;

    @Autowired
    private LobbyRoomsCacheService lobbyRoomsCache;

    public GameRoom createRoom(String name, boolean isPrivate, String password, int maxPlayers, Long minBet, Long turnTimeLimit) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name cannot be empty");
        }
        name = name.trim();
        if (name.length() > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name cannot exceed 24 characters");
        }
        if (!name.matches("^[\\p{L}\\p{N}\\s_\\-!.]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name contains invalid characters");
        }

        for (GameRoom r : gameRoomRepository.findAll()) {
            if (r.getName() != null && r.getName().equals(name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room with this name already exists");
            }
        }

        String roomId = UUID.randomUUID().toString();

        if (maxPlayers < 2 || maxPlayers > 6) maxPlayers = 6;
        if (minBet == null || (minBet != 10L && minBet != 50L && minBet != 100L)) minBet = 10L;
        if (turnTimeLimit == null || (turnTimeLimit != 15000L && turnTimeLimit != 30000L)) turnTimeLimit = 30000L;

        GameRoom room = new GameRoom(roomId, name, isPrivate, password, maxPlayers, minBet, turnTimeLimit);
        GameRoom saved = gameRoomRepository.save(room);
        lobbyRoomsCache.refresh();
        log.info("[LOBBY CREATE] room={} name='{}' private={} maxPlayers={} minBet={} turnTimeLimit={}ms",
                roomId, name, isPrivate, maxPlayers, minBet, turnTimeLimit);
        return saved;
    }

    public GameRoom joinRoomByName(String roomName, String username, String password) {
        String roomId = null;
        for (GameRoom r : gameRoomRepository.findAll()) {
            if (r.getName() != null && r.getName().equals(roomName)) {
                roomId = r.getRoomId();
                break;
            }
        }
        if (roomId == null) {
            log.warn("[LOBBY JOIN] user={} | roomName='{}' | result=NOT_FOUND", username, roomName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found with name: " + roomName);
        }
        return joinRoom(roomId, username, password);
    }

    public GameRoom joinRoom(String roomId, String username, String password) {
        for (GameRoom r : gameRoomRepository.findAll()) {
            if (r.getPlayers() != null && r.getPlayers().contains(username)) {
                if (!r.getRoomId().equals(roomId)) {
                    log.info("[LOBBY KICK FROM OLD ROOM] user={} leaving room={} to join room={}",
                            username, r.getRoomId(), roomId);
                    GameRoom updatedOldRoom = leaveRoom(r.getRoomId(), username);
                    if (updatedOldRoom != null) {
                        gameServiceClient.notifyRoom(r.getRoomId());
                    }
                }
            }
        }

        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if ("IN_PROGRESS".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot join a room while the game is in progress");
        }

        if (room.isPrivate() && !Objects.equals(room.getPassword(), password)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
        }

        List<String> players = room.getPlayers();
        if (players == null) players = new ArrayList<>();
        else players = new ArrayList<>(players);

        if (!players.contains(username)) {
            if (players.size() >= room.getMaxPlayers()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full");
            }
            players.add(username);
            room.setPlayers(players);

            Map<String, Boolean> readyStatus = room.getReadyStatus();
            if (readyStatus == null) readyStatus = new HashMap<>();
            else readyStatus = new HashMap<>(readyStatus);

            readyStatus.put(username, false);
            room.setReadyStatus(readyStatus);

            gameRoomRepository.save(room);
            lobbyRoomsCache.refresh();
            log.info("[LOBBY JOIN] user={} joined room={} name='{}' | players={}/{}",
                    username, roomId, room.getName(), players.size(), room.getMaxPlayers());
        } else {
            log.info("[LOBBY JOIN] user={} already in room={} name='{}'", username, roomId, room.getName());
        }
        return room;
    }

    public GameRoom takeSeat(String roomId, String username, int seatNumber) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if ("IN_PROGRESS".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change seats while the game is in progress");
        }

        if (!room.getPlayers().contains(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must join the room first");
        }

        if (seatNumber < 0 || seatNumber >= room.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid seat number");
        }

        Map<Integer, String> seats = room.getSeats();
        if (seats == null) seats = new HashMap<>();
        else seats = new HashMap<>(seats);

        if (seats.containsKey(seatNumber) && !seats.get(seatNumber).equals(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat is already taken");
        }

        seats.values().removeIf(u -> u.equals(username));

        seats.put(seatNumber, username);
        room.setSeats(seats);

        GameRoom saved = gameRoomRepository.save(room);
        lobbyRoomsCache.refresh();
        log.info("[LOBBY SEAT] user={} took seat={} in room={} | seats={}",
                username, seatNumber, roomId, seats);
        return saved;
    }

    public GameRoom leaveRoom(String roomId, String username) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        List<String> players = room.getPlayers();
        if (players != null && players.contains(username)) {
            players = new ArrayList<>(players);
            players.remove(username);
            room.setPlayers(players);

            Map<String, Boolean> readyStatus = room.getReadyStatus();
            if (readyStatus != null) {
                readyStatus = new HashMap<>(readyStatus);
                readyStatus.remove(username);
                room.setReadyStatus(readyStatus);
            }

            Map<Integer, String> seats = room.getSeats();
            if (seats != null) {
                seats = new HashMap<>(seats);
                seats.values().removeIf(u -> u.equals(username));
                room.setSeats(seats);
            }

            if (players.isEmpty()) {
                gameRoomRepository.delete(room);
                lobbyRoomsCache.refresh();
                log.info("[LOBBY LEAVE] user={} left room={} | room deleted (empty)", username, roomId);
                return null;
            } else {
                GameRoom saved = gameRoomRepository.save(room);
                lobbyRoomsCache.refresh();
                log.info("[LOBBY LEAVE] user={} left room={} | remainingPlayers={}", username, roomId, players);
                return saved;
            }
        }
        log.info("[LOBBY LEAVE] user={} not in room={}", username, roomId);
        return room;
    }

    public GameRoom setReady(String roomId, String username, boolean ready) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!"WAITING".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change ready status while the game is in progress");
        }

        Map<String, Boolean> readyStatus = room.getReadyStatus();
        if (readyStatus == null) readyStatus = new HashMap<>();
        else readyStatus = new HashMap<>(readyStatus);

        readyStatus.put(username, ready);
        room.setReadyStatus(readyStatus);
        room = gameRoomRepository.save(room);
        lobbyRoomsCache.refresh();
        log.info("[LOBBY READY] user={} ready={} in room={} | readyStatus={}",
                username, ready, roomId, readyStatus);

        GameRoom result = checkGameStart(room);
        if (result != room) {
            lobbyRoomsCache.refresh();
        }
        return result;
    }

    private GameRoom checkGameStart(GameRoom room) {
        List<String> seatedPlayers = room.getSeatedPlayersOrder();
        if (seatedPlayers != null && seatedPlayers.size() > 1) {
            Map<String, Boolean> readyStatus = room.getReadyStatus();
            boolean allReady = seatedPlayers.stream().allMatch(p -> readyStatus.getOrDefault(p, false));
            if (allReady) {
                log.info("[LOBBY START TRIGGER] room={} | all seated players ready: {}", room.getRoomId(), seatedPlayers);
                return gameServiceClient.startGame(room.getRoomId());
            }
            log.info("[LOBBY START WAIT] room={} | seated={} not all ready yet", room.getRoomId(), seatedPlayers);
        }
        return room;
    }

    public List<GameRoom> getAllRooms() {
        List<GameRoom> rooms = lobbyRoomsCache.getAll();
        log.info("[LOBBY LIST] requested | totalRooms={} | source=cache", rooms.size());
        return rooms;
    }

    public int deleteAllLobbies() {
        long count = gameRoomRepository.count();
        gameRoomRepository.deleteAll();
        lobbyRoomsCache.refresh();
        log.warn("[LOBBY ADMIN] deleted all rooms | count={}", count);
        return (int) count;
    }
}
