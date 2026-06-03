package ru.meowmure.game.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;

import ru.meowmure.game.dto.PlayerProfileSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RedisHash("GameRoom")
public class GameRoom {

    @Id
    private String roomId;
    private String name;
    private boolean isPrivate;
    private String password;
    
    private List<String> players;
    private Map<String, Boolean> readyStatus;
    
    // Game State
    private Deck mainDeck;
    private Deck tableDeck;
    private Map<String, PlayerState> playerStates;
    private String currentTurn;
    private Long currentHighestBet;
    
    private Long pot;
    private String status;
    private int currentRound;
    private String winner;
    private String winningCombination;

    private int maxPlayers;
    private Long minBet;
    private Long turnTimeLimit;
    private Map<Integer, String> seats;
    private Long turnStartTime;
    private Boolean roundTransitionPending;
    @Transient
    private Long viewerBalance;
    @Transient
    private Map<String, Long> playerBalances;
    @Transient
    private Map<String, PlayerProfileSummary> playerProfiles;

    public GameRoom() {
        this.players = new ArrayList<>();
        this.readyStatus = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.seats = new HashMap<>();
        this.pot = 0L;
        this.currentHighestBet = 0L;
        this.status = "WAITING";
        this.currentRound = 0;
        this.winner = null;
        this.winningCombination = null;
        this.maxPlayers = 6;
        this.minBet = 10L;
        this.turnTimeLimit = 30000L;
        this.turnStartTime = null;
        this.roundTransitionPending = false;
    }

    public GameRoom(String roomId, String name, boolean isPrivate, String password, int maxPlayers, Long minBet, Long turnTimeLimit) {
        this.roomId = roomId;
        this.name = name;
        this.isPrivate = isPrivate;
        this.password = password;
        this.players = new ArrayList<>();
        this.readyStatus = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.seats = new HashMap<>();
        this.pot = 0L;
        this.currentHighestBet = 0L;
        this.status = "WAITING";
        this.currentRound = 0;
        this.winner = null;
        this.winningCombination = null;
        this.maxPlayers = maxPlayers;
        this.minBet = minBet;
        this.turnTimeLimit = turnTimeLimit != null ? turnTimeLimit : 30000L;
        this.turnStartTime = null;
        this.roundTransitionPending = false;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public Map<String, Boolean> getReadyStatus() {
        return readyStatus;
    }

    public void setReadyStatus(Map<String, Boolean> readyStatus) {
        this.readyStatus = readyStatus;
    }

    public Map<String, PlayerState> getPlayerStates() {
        return playerStates;
    }

    public void setPlayerStates(Map<String, PlayerState> playerStates) {
        this.playerStates = playerStates;
    }

    public Deck getMainDeck() {
        return mainDeck;
    }

    public void setMainDeck(Deck mainDeck) {
        this.mainDeck = mainDeck;
    }

    public Deck getTableDeck() {
        return tableDeck;
    }

    public void setTableDeck(Deck tableDeck) {
        this.tableDeck = tableDeck;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public Long getCurrentHighestBet() {
        return currentHighestBet;
    }

    public void setCurrentHighestBet(Long currentHighestBet) {
        this.currentHighestBet = currentHighestBet;
    }

    public Long getPot() {
        return pot;
    }

    public void setPot(Long pot) {
        this.pot = pot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getWinningCombination() {
        return winningCombination;
    }

    public void setWinningCombination(String winningCombination) {
        this.winningCombination = winningCombination;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Long getMinBet() {
        return minBet;
    }

    public void setMinBet(Long minBet) {
        this.minBet = minBet;
    }

    public Long getTurnTimeLimit() {
        return turnTimeLimit;
    }

    public void setTurnTimeLimit(Long turnTimeLimit) {
        this.turnTimeLimit = turnTimeLimit;
    }

    public Map<Integer, String> getSeats() {
        return seats;
    }

    public void setSeats(Map<Integer, String> seats) {
        this.seats = seats;
    }

    public List<String> getSeatedPlayersOrder() {
        List<String> order = new ArrayList<>();
        if (seats != null) {
            for (int i = 0; i < maxPlayers; i++) {
                if (seats.containsKey(i)) {
                    order.add(seats.get(i));
                }
            }
        }
        return order;
    }

    public Long getTurnStartTime() {
        return turnStartTime;
    }

    public void setTurnStartTime(Long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    public Boolean getRoundTransitionPending() {
        return roundTransitionPending;
    }

    public void setRoundTransitionPending(Boolean roundTransitionPending) {
        this.roundTransitionPending = roundTransitionPending;
    }

    public Long getViewerBalance() {
        return viewerBalance;
    }

    public void setViewerBalance(Long viewerBalance) {
        this.viewerBalance = viewerBalance;
    }

    public Map<String, Long> getPlayerBalances() {
        return playerBalances;
    }

    public void setPlayerBalances(Map<String, Long> playerBalances) {
        this.playerBalances = playerBalances;
    }

    public Map<String, PlayerProfileSummary> getPlayerProfiles() {
        return playerProfiles;
    }

    public void setPlayerProfiles(Map<String, PlayerProfileSummary> playerProfiles) {
        this.playerProfiles = playerProfiles;
    }

    public GameRoom createViewForUser(String username) {
        GameRoom view = new GameRoom();
        view.setRoomId(this.roomId);
        view.setName(this.name);
        view.setPrivate(this.isPrivate);
        view.setPassword(this.password);
        view.setPlayers(this.players != null ? new ArrayList<>(this.players) : new ArrayList<>());
        if ("WAITING".equals(this.status)) {
            view.setReadyStatus(this.readyStatus != null ? new HashMap<>(this.readyStatus) : new HashMap<>());
        } else {
            view.setReadyStatus(null);
        }
        view.setSeats(this.seats != null ? new HashMap<>(this.seats) : new HashMap<>());
        view.setMaxPlayers(this.maxPlayers);
        view.setMinBet(this.minBet);
        view.setTurnTimeLimit(this.turnTimeLimit);
        view.setCurrentTurn(this.currentTurn);
        view.setTurnStartTime(this.turnStartTime);
        view.setRoundTransitionPending(this.roundTransitionPending);
        view.setCurrentHighestBet(this.currentHighestBet);
        view.setPot(this.pot);
        view.setStatus(this.status);
        view.setCurrentRound(this.currentRound);
        view.setWinner(this.winner);
        view.setWinningCombination(this.winningCombination);
        
        // Hide main deck completely
        view.setMainDeck(null);
        
        // Mask table deck based on the current round
        if (this.tableDeck != null && this.tableDeck.getCards() != null) {
            int visibleCount = 0;
            if ("FINISHED".equals(this.status)) visibleCount = 5;
            else if (this.currentRound == 1) visibleCount = 3; // Flop
            else if (this.currentRound == 2) visibleCount = 4; // Turn
            else if (this.currentRound >= 3) visibleCount = 5; // River

            List<Card> realCards = this.tableDeck.getCards();
            List<Card> tableCardsView = new ArrayList<>();
            for (int i = 0; i < realCards.size(); i++) {
                if (i < visibleCount) {
                    tableCardsView.add(realCards.get(i));
                } else {
                    tableCardsView.add(new Card(-1, "empty"));
                }
            }
            Deck tableDeckView = new Deck();
            tableDeckView.setCards(tableCardsView);
            view.setTableDeck(tableDeckView);
        } else {
            view.setTableDeck(null);
        }
        
        // Hide other players' cards
        Map<String, PlayerState> viewStates = new HashMap<>();
        if (this.playerStates != null) {
            for (Map.Entry<String, PlayerState> entry : this.playerStates.entrySet()) {
                PlayerState originalState = entry.getValue();
                PlayerState stateView = new PlayerState(originalState.getUsername());
                stateView.setCurrentBet(originalState.getCurrentBet());
                stateView.setFolded(originalState.isFolded());
                stateView.setActedThisRound(originalState.isActedThisRound());
                stateView.setBestCombination(originalState.getBestCombination());
                stateView.setAllIn(originalState.isAllIn());
                
                List<Card> cardViews = new ArrayList<>();
                if (originalState.getCards() != null) {
                    if (entry.getKey().equals(username) || "FINISHED".equals(this.status)) {
                        // Show cards to the owner, or to everyone if game is finished
                        cardViews.addAll(originalState.getCards());
                    } else {
                        // Hide cards from others
                        for (int i = 0; i < originalState.getCards().size(); i++) {
                            cardViews.add(new Card(-1, "empty"));
                        }
                    }
                }
                stateView.setCards(cardViews);
                viewStates.put(entry.getKey(), stateView);
            }
        }
        view.setPlayerStates(viewStates);
        
        return view;
    }
}
