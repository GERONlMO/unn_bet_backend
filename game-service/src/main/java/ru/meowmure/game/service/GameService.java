package ru.meowmure.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ru.meowmure.game.model.Deck;
import ru.meowmure.game.model.PlayerState;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.util.GameRoomSnapshotLogger;

import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final long ROUND_TRANSITION_DELAY_MS = 1500L;
    private static final long FINISHED_RESET_DELAY_MS = 8000L;

    private final Set<String> scheduledRoundTransitions = ConcurrentHashMap.newKeySet();

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CombinationObserver combinationObserver;
    
    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private WalletClient walletClient;

    @Autowired
    private LobbyRoomsCacheService lobbyRoomsCache;

    private GameRoom saveRoom(GameRoom room) {
        GameRoom saved = gameRoomRepository.save(room);
        lobbyRoomsCache.refresh();
        return saved;
    }

    private void validateCanAct(GameRoom room) {
        if (Boolean.TRUE.equals(room.getRoundTransitionPending())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Round transition in progress");
        }
        if (!"IN_PROGRESS".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not in progress");
        }
    }

    private void scheduleRoomReset(String roomId) {
        new Thread(() -> {
            try {
                Thread.sleep(FINISHED_RESET_DELAY_MS);
                GameRoom resetRoom = resetRoom(roomId);
                sseNotificationService.notifyRoom(roomId, resetRoom);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void deductBalance(String username, Long amount) {
        walletClient.deductBalance(username, amount);
    }

    private Long getBalance(String username) {
        return walletClient.getBalance(username);
    }

    public void finishGame(GameRoom room, String winnerUsernames, Long winAmount, String winningCombination) {
        log.info("[ROOM {}] Game finished. Winners: {}, Combination: {}, Win amount: {}", room.getRoomId(), winnerUsernames, winningCombination, winAmount);
        GameRoomSnapshotLogger.logSnapshot(log, "GAME FINISHED", room);
        room.setStatus("FINISHED");
        room.setWinner(winnerUsernames);
        room.setWinningCombination(winningCombination);
        saveRoom(room);
        
        String[] winners = winnerUsernames.split(",");
        if (winners.length > 0 && winAmount > 0) {
            Long amountPerWinner = winAmount / winners.length;
            for (String winner : winners) {
                log.info("[KAFKA] game-results -> winner={} amount={}", winner.trim(), amountPerWinner);
                kafkaTemplate.send("game-results", winner.trim() + ":" + amountPerWinner);
            }
            publishProfileStats(room, winnerUsernames, amountPerWinner);
            try {
                kafkaTemplate.flush();
            } catch (Exception e) {
                log.warn("[KAFKA] flush failed after game finish room={}: {}", room.getRoomId(), e.getMessage());
            }
        }
    }

    private static final String PROFILE_STATS_PREFIX = "profile:";

    private void publishProfileStats(GameRoom room, String winnerUsernames, long amountPerWinner) {
        if (room.getPlayerStates() == null || room.getPlayerStates().isEmpty()) {
            return;
        }
        String players = String.join(",", room.getPlayerStates().keySet());
        String payload = PROFILE_STATS_PREFIX + players + "|" + winnerUsernames + "|" + amountPerWinner;
        log.info("[KAFKA] game-results (profile) -> {}", payload);
        kafkaTemplate.send("game-results", payload);
    }

    public GameRoom getRoomState(String roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    @Scheduled(fixedDelay = 2000)
    public void checkTimeouts() {
        Iterable<GameRoom> rooms = gameRoomRepository.findAll();
        long now = System.currentTimeMillis();
        for (GameRoom room : rooms) {
            if ("IN_PROGRESS".equals(room.getStatus())
                    && !Boolean.TRUE.equals(room.getRoundTransitionPending())
                    && room.getCurrentTurn() != null && room.getTurnStartTime() != null) {
                Long turnTimeLimit = room.getTurnTimeLimit() != null ? room.getTurnTimeLimit() : 30000L;
                if (now - room.getTurnStartTime() > turnTimeLimit) {
                    String timedOutPlayer = room.getCurrentTurn();
                    log.info("[ROOM {}] Player {} timed out. Auto-folding.", room.getRoomId(), timedOutPlayer);
                    try {
                        GameRoom updatedRoom = fold(room.getRoomId(), timedOutPlayer);
                        sseNotificationService.notifyRoom(room.getRoomId(), updatedRoom);
                        
                        if ("FINISHED".equals(updatedRoom.getStatus())) {
                            scheduleRoomReset(updatedRoom.getRoomId());
                        }
                    } catch (Exception e) {
                        log.warn("[ROOM {}] Failed to auto-fold player {}: {}", room.getRoomId(), timedOutPlayer, e.getMessage());
                    }
                }
            }
        }
    }

    public GameRoom resetRoom(String roomId) {
        log.info("[ROOM {}] Resetting room to WAITING state", roomId);
        GameRoom room = getRoomState(roomId);
        room.setStatus("WAITING");
        room.setPot(0L);
        room.setCurrentHighestBet(0L);
        room.setCurrentRound(0);
        room.setWinner(null);
        room.setWinningCombination(null);
        room.setMainDeck(null);
        room.setTableDeck(null);
        room.setCurrentTurn(null);
        room.setTurnStartTime(null);
        room.setRoundTransitionPending(false);
        scheduledRoundTransitions.remove(roomId);
        room.setPlayerStates(new HashMap<>());
        
        Map<String, Boolean> readyStatus = new HashMap<>();
        if (room.getPlayers() != null) {
            for (String player : room.getPlayers()) {
                readyStatus.put(player, false);
            }
        }
        room.setReadyStatus(readyStatus);
        
        GameRoom saved = saveRoom(room);
        GameRoomSnapshotLogger.logSnapshot(log, "ROOM RESET", saved);
        return saved;
    }

    public GameRoom startGame(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        
        List<String> seatedPlayers = room.getSeatedPlayersOrder();
        if (seatedPlayers.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough seated players to start");
        }
        
        room.setStatus("IN_PROGRESS");
        room.setCurrentRound(0);
        room.setPot(0L);
        room.setCurrentHighestBet(0L);
        room.setWinner(null);
        room.setWinningCombination(null);
        room.setRoundTransitionPending(false);
        
        // Initialize decks
        Deck mainDeck = new Deck();
        mainDeck.initializeStandardDeck();
        mainDeck.shuffle();
        room.setMainDeck(mainDeck);
        
        Deck tableDeck = new Deck();
        // Deal 5 community cards to the table (hidden initially based on round)
        for (int i = 0; i < 5; i++) {
            tableDeck.addCard(mainDeck.draw());
        }
        room.setTableDeck(tableDeck);
        
        // Initialize players
        Map<String, PlayerState> states = new HashMap<>();
        for (String player : seatedPlayers) {
            PlayerState state = new PlayerState(player);
            // Deal 2 cards
            state.getCards().add(mainDeck.draw());
            state.getCards().add(mainDeck.draw());
            state.setActedThisRound(false);
            states.put(player, state);
        }
        room.setPlayerStates(states);
        
        // Set first turn
        room.setCurrentTurn(seatedPlayers.get(0));
        
        log.info("[ROOM {}] Game Started! Round: 0 (Pre-flop). Players: {}", roomId, seatedPlayers);
        
        // --- Automatic Blinds Logic ---
        Long minBet = room.getMinBet() != null ? room.getMinBet() : 10L;
        Long smallBlind = minBet / 2;
        Long bigBlind = minBet;
        
        String sbPlayer = seatedPlayers.get(0);
        String bbPlayer = seatedPlayers.get(1);
        
        // Deduct Small Blind
        PlayerState sbState = states.get(sbPlayer);
        Long sbBalance = getBalance(sbPlayer);
        Long actualSb = Math.min(smallBlind, sbBalance);
        sbState.setCurrentBet(actualSb);
        if (actualSb.equals(sbBalance)) sbState.setAllIn(true);
        room.setPot(room.getPot() + actualSb);
        deductBalance(sbPlayer, actualSb);
        
        // Deduct Big Blind
        PlayerState bbState = states.get(bbPlayer);
        Long bbBalance = getBalance(bbPlayer);
        Long actualBb = Math.min(bigBlind, bbBalance);
        bbState.setCurrentBet(actualBb);
        if (actualBb.equals(bbBalance)) bbState.setAllIn(true);
        room.setPot(room.getPot() + actualBb);
        deductBalance(bbPlayer, actualBb);
        
        room.setCurrentHighestBet(Math.max(actualSb, actualBb));
        
        // The player after the Big Blind should act first (if > 2 players, otherwise SB acts first)
        if (seatedPlayers.size() > 2) {
            room.setCurrentTurn(seatedPlayers.get(2));
        } else {
            room.setCurrentTurn(sbPlayer);
        }
        room.setTurnStartTime(System.currentTimeMillis());
        // ------------------------------
        
        GameRoom saved = saveRoom(room);
        GameRoomSnapshotLogger.logSnapshot(log, "GAME STARTED", saved);
        return saved;
    }

    public GameRoom fold(String roomId, String username) {
        log.info("[ROOM {}] Action: FOLD by user {}", roomId, username);
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        validateCanAct(room);
        if (!username.equals(room.getCurrentTurn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not your turn");
        
        PlayerState state = room.getPlayerStates().get(username);
        state.setFolded(true);
        state.setActedThisRound(true);
        
        nextTurn(room);
        GameRoom result = gameRoomRepository.findById(roomId).orElse(room);
        GameRoomSnapshotLogger.logSnapshot(log, "AFTER FOLD by " + username, result);
        return result;
    }

    public GameRoom raise(String roomId, String username, Long amount) {
        log.info("[ROOM {}] Action: RAISE by user {} amount {}", roomId, username, amount);
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        validateCanAct(room);
        if (!username.equals(room.getCurrentTurn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not your turn");
        
        long playersWhoCanAct = room.getPlayerStates().values().stream()
                .filter(p -> !p.isFolded() && !p.isAllIn())
                .count();
        if (playersWhoCanAct <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot raise, all other players are all-in");
        }

        Long balance = getBalance(username);
        if (amount > balance) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot raise more than your balance");
        }

        PlayerState state = room.getPlayerStates().get(username);
        Long totalBet = state.getCurrentBet() + amount;
        if (totalBet <= room.getCurrentHighestBet()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Raise must be higher than current bet");
        
        if (amount.equals(balance)) {
            state.setAllIn(true);
        }

        // Deduct from wallet synchronously
        deductBalance(username, amount);
        
        state.setCurrentBet(totalBet);
        state.setActedThisRound(true);
        room.setCurrentHighestBet(totalBet);
        room.setPot(room.getPot() + amount);
        
        // Reset actedThisRound for all other active players since the bet was raised
        for (PlayerState p : room.getPlayerStates().values()) {
            if (!p.getUsername().equals(username) && !p.isFolded() && !p.isAllIn()) {
                p.setActedThisRound(false);
            }
        }
        
        nextTurn(room);
        GameRoom result = gameRoomRepository.findById(roomId).orElse(room);
        GameRoomSnapshotLogger.logSnapshot(log, "AFTER RAISE by " + username + " amount=" + amount, result);
        return result;
    }

    public GameRoom check(String roomId, String username) {
        log.info("[ROOM {}] Action: CHECK by user {}", roomId, username);
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        validateCanAct(room);
        if (!username.equals(room.getCurrentTurn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not your turn");
        
        PlayerState state = room.getPlayerStates().get(username);
        if (state.getCurrentBet() < room.getCurrentHighestBet()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check, must call or fold");
        
        state.setActedThisRound(true);
        
        nextTurn(room);
        GameRoom result = gameRoomRepository.findById(roomId).orElse(room);
        GameRoomSnapshotLogger.logSnapshot(log, "AFTER CHECK by " + username, result);
        return result;
    }

    public GameRoom call(String roomId, String username) {
        log.info("[ROOM {}] Action: CALL by user {}", roomId, username);
        GameRoom room = gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        validateCanAct(room);
        if (!username.equals(room.getCurrentTurn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not your turn");
        
        PlayerState state = room.getPlayerStates().get(username);
        Long amountToCall = room.getCurrentHighestBet() - state.getCurrentBet();
        
        if (amountToCall > 0) {
            Long balance = getBalance(username);
            Long actualCallAmount = amountToCall;
            
            if (balance <= amountToCall) {
                actualCallAmount = balance;
                state.setAllIn(true);
            }
            
            deductBalance(username, actualCallAmount);
            state.setCurrentBet(state.getCurrentBet() + actualCallAmount);
            room.setPot(room.getPot() + actualCallAmount);
        }
        
        state.setActedThisRound(true);
        
        nextTurn(room);
        GameRoom result = gameRoomRepository.findById(roomId).orElse(room);
        GameRoomSnapshotLogger.logSnapshot(log, "AFTER CALL by " + username, result);
        return result;
    }

    private void nextTurn(GameRoom room) {
        String roomId = room.getRoomId();
        List<PlayerState> activePlayers = room.getPlayerStates().values().stream()
                .filter(p -> !p.isFolded())
                .toList();

        if (activePlayers.size() == 1) {
            log.info("[ROOM {}] Game Ended! Only one player left: {}", roomId, activePlayers.get(0).getUsername());
            finishGame(room, activePlayers.get(0).getUsername(), room.getPot(), "Last player standing");
            return;
        }

        boolean roundOver = activePlayers.stream()
                .allMatch(p -> p.isAllIn() || (p.isActedThisRound() && p.getCurrentBet().equals(room.getCurrentHighestBet())));

        if (roundOver) {
            beginRoundTransition(room);
            return;
        } else {
            // Find next active player
            List<String> players = room.getSeatedPlayersOrder();
            int currentIndex = players.indexOf(room.getCurrentTurn());
            
            for (int i = 1; i <= players.size(); i++) {
                int nextIndex = (currentIndex + i) % players.size();
                PlayerState ps = room.getPlayerStates().get(players.get(nextIndex));
                if (ps != null && !ps.isFolded() && !ps.isAllIn()) {
                    room.setCurrentTurn(players.get(nextIndex));
                    room.setTurnStartTime(System.currentTimeMillis());
                    log.info("[ROOM {}] Next turn passed to {}", roomId, room.getCurrentTurn());
                    break;
                }
            }
            saveRoom(room);
        }
    }

    private void beginRoundTransition(GameRoom room) {
        String roomId = room.getRoomId();
        log.info("[ROOM {}] Betting round ended. Waiting {}ms before advancing.", roomId, ROUND_TRANSITION_DELAY_MS);
        room.setRoundTransitionPending(true);
        room.setCurrentTurn(null);
        room.setTurnStartTime(null);
        saveRoom(room);
        GameRoomSnapshotLogger.logSnapshot(log, "ROUND TRANSITION PENDING (bets visible)", room);
        scheduleRoundAdvance(roomId);
    }

    private void scheduleRoundAdvance(String roomId) {
        if (!scheduledRoundTransitions.add(roomId)) {
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep(ROUND_TRANSITION_DELAY_MS);
                advanceRoundAfterTransition(roomId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                scheduledRoundTransitions.remove(roomId);
            }
        }).start();
    }

    private void advanceRoundAfterTransition(String roomId) {
        GameRoom room = gameRoomRepository.findById(roomId).orElse(null);
        if (room == null || !Boolean.TRUE.equals(room.getRoundTransitionPending())) {
            return;
        }
        room.setRoundTransitionPending(false);
        applyRoundAdvance(room);

        GameRoom updatedRoom = gameRoomRepository.findById(roomId).orElse(room);
        GameRoomSnapshotLogger.logSnapshot(log, "ROUND ADVANCED (after delay)", updatedRoom);
        sseNotificationService.notifyRoom(roomId, updatedRoom);

        if ("FINISHED".equals(updatedRoom.getStatus())) {
            scheduleRoomReset(roomId);
        }
    }

    private void applyRoundAdvance(GameRoom room) {
        String roomId = room.getRoomId();
        List<PlayerState> activePlayers = room.getPlayerStates().values().stream()
                .filter(p -> !p.isFolded())
                .toList();

        room.setCurrentRound(room.getCurrentRound() + 1);

        String roundName = "Unknown";
        switch (room.getCurrentRound()) {
            case 1: roundName = "Flop"; break;
            case 2: roundName = "Turn"; break;
            case 3: roundName = "River"; break;
            case 4: roundName = "Showdown"; break;
        }
        log.info("[ROOM {}] Round Ended! Advanced to round {} ({})", roomId, room.getCurrentRound(), roundName);

        if (room.getCurrentRound() >= 4) {
            List<CombinationObserver.HandResult> results = combinationObserver.evaluateAllHands(activePlayers, room.getTableDeck().getCards());
            List<CombinationObserver.HandResult> winners = combinationObserver.getWinners(results);

            String winnerNames = winners.stream().map(w -> w.username).collect(java.util.stream.Collectors.joining(","));
            String winningComb = winners.get(0).combinationName;

            log.info("[ROOM {}] Showdown! Winner(s): {} with {}", roomId, winnerNames, winningComb);

            for (CombinationObserver.HandResult res : results) {
                PlayerState p = room.getPlayerStates().get(res.username);
                p.setBestCombination(res.combinationName);
                log.info("[ROOM {}] Player {} had combination: {} (Cards: {}, {})", roomId, res.username, res.combinationName, p.getCards().get(0).getValue(), p.getCards().get(1).getValue());
            }

            finishGame(room, winnerNames, room.getPot(), winningComb);
            return;
        }

        activePlayers.forEach(p -> {
            if (!p.isAllIn()) p.setActedThisRound(false);
        });
        room.setCurrentHighestBet(0L);
        activePlayers.forEach(p -> p.setCurrentBet(0L));

        boolean foundFirst = false;
        for (String p : room.getSeatedPlayersOrder()) {
            PlayerState ps = room.getPlayerStates().get(p);
            if (ps != null && !ps.isFolded() && !ps.isAllIn()) {
                room.setCurrentTurn(p);
                foundFirst = true;
                break;
            }
        }

        if (!foundFirst) {
            log.info("[ROOM {}] No active players found to start next round. Fast-forwarding.", roomId);
            room.setCurrentTurn(null);
            room.setTurnStartTime(null);
            saveRoom(room);
            applyRoundAdvance(room);
            return;
        }
        log.info("[ROOM {}] Next turn passed to {} for round {}", roomId, room.getCurrentTurn(), room.getCurrentRound());
        room.setTurnStartTime(System.currentTimeMillis());
        saveRoom(room);
    }
}
