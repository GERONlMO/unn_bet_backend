package ru.meowmure.game.util;

import org.slf4j.Logger;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.PlayerState;

import java.util.List;
import java.util.stream.Collectors;

public final class GameRoomSnapshotLogger {

    private GameRoomSnapshotLogger() {
    }

    public static void logSnapshot(Logger log, String event, GameRoom room) {
        if (room == null) {
            log.info("[SNAPSHOT] {} | room=null", event);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[ROOM ").append(room.getRoomId()).append("] ").append(event);
        sb.append(" | name=").append(room.getName());
        sb.append(" | status=").append(room.getStatus());
        sb.append(" | round=").append(room.getCurrentRound());
        sb.append(" | pot=").append(room.getPot());
        sb.append(" | highestBet=").append(room.getCurrentHighestBet());
        sb.append(" | turn=").append(room.getCurrentTurn());
        sb.append(" | transition=").append(room.getRoundTransitionPending());
        sb.append(" | table=").append(formatTableCards(room));

        if (room.getSeats() != null && !room.getSeats().isEmpty()) {
            sb.append(" | seats=").append(room.getSeats());
        }
        if (room.getReadyStatus() != null && !room.getReadyStatus().isEmpty()) {
            sb.append(" | ready=").append(room.getReadyStatus());
        }
        if (room.getWinner() != null) {
            sb.append(" | winner=").append(room.getWinner());
            sb.append(" | combination=").append(room.getWinningCombination());
        }

        if (room.getPlayerStates() != null) {
            for (PlayerState ps : room.getPlayerStates().values()) {
                sb.append(" || player=").append(ps.getUsername());
                sb.append("{bet=").append(ps.getCurrentBet());
                sb.append(",folded=").append(ps.isFolded());
                sb.append(",acted=").append(ps.isActedThisRound());
                sb.append(",allIn=").append(ps.isAllIn());
                if (ps.getBestCombination() != null) {
                    sb.append(",combo=").append(ps.getBestCombination());
                }
                sb.append(",cards=").append(formatHoleCards(ps.getCards()));
                sb.append("}");
            }
        }

        log.info(sb.toString());
    }

    private static String formatTableCards(GameRoom room) {
        if (room.getTableDeck() == null || room.getTableDeck().getCards() == null) {
            return "[]";
        }
        return room.getTableDeck().getCards().stream()
                .map(GameRoomSnapshotLogger::formatCard)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String formatHoleCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return "[]";
        }
        return cards.stream()
                .map(GameRoomSnapshotLogger::formatCard)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String formatCard(Card card) {
        if (card == null || card.getValue() <= 0) {
            return "??";
        }
        String rank = switch (card.getValue()) {
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            case 14 -> "A";
            default -> String.valueOf(card.getValue());
        };
        String suit = card.getSuit() != null ? card.getSuit().substring(0, 1) : "?";
        return rank + suit;
    }
}
