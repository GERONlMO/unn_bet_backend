package ru.meowmure.game.service;

import org.springframework.stereotype.Service;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.PlayerState;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CombinationObserver {

    // Ranks: 1=High Card, 2=Pair, 3=Two Pair, 4=Three of a Kind, 5=Straight, 6=Flush, 7=Full House, 8=Four of a Kind, 9=Straight Flush, 10=Royal Flush
    
    public static class HandResult {
        public String username;
        public int rank;
        public String combinationName;
        public List<Integer> tieBreakers;

        public HandResult(String username, int rank, String combinationName, List<Integer> tieBreakers) {
            this.username = username;
            this.rank = rank;
            this.combinationName = combinationName;
            this.tieBreakers = tieBreakers;
        }
    }

    public List<HandResult> evaluateAllHands(List<PlayerState> activePlayers, List<Card> tableCards) {
        if (activePlayers == null || activePlayers.isEmpty()) return new ArrayList<>();
        
        List<HandResult> results = new ArrayList<>();
        for (PlayerState player : activePlayers) {
            List<Card> allCards = new ArrayList<>(tableCards);
            allCards.addAll(player.getCards());
            results.add(evaluatePlayerHand(player.getUsername(), allCards));
        }
        
        // Sort by rank descending, then by tie breakers descending
        results.sort((h1, h2) -> {
            if (h1.rank != h2.rank) {
                return Integer.compare(h2.rank, h1.rank);
            }
            for (int i = 0; i < Math.min(h1.tieBreakers.size(), h2.tieBreakers.size()); i++) {
                if (!h1.tieBreakers.get(i).equals(h2.tieBreakers.get(i))) {
                    return Integer.compare(h2.tieBreakers.get(i), h1.tieBreakers.get(i));
                }
            }
            return 0;
        });
        
        return results;
    }

    public List<HandResult> getWinners(List<HandResult> sortedResults) {
        if (sortedResults.isEmpty()) return new ArrayList<>();
        List<HandResult> winners = new ArrayList<>();
        HandResult best = sortedResults.get(0);
        winners.add(best);
        for (int i = 1; i < sortedResults.size(); i++) {
            HandResult current = sortedResults.get(i);
            if (current.rank == best.rank && current.tieBreakers.equals(best.tieBreakers)) {
                winners.add(current);
            } else {
                break;
            }
        }
        return winners;
    }

    public HandResult evaluateBestHand(List<PlayerState> activePlayers, List<Card> tableCards) {
        List<HandResult> all = evaluateAllHands(activePlayers, tableCards);
        return all.isEmpty() ? null : all.get(0);
    }

    private HandResult evaluatePlayerHand(String username, List<Card> cards) {
        // Very simplified placeholder poker evaluator for now.
        // In a real app, this would check all 7 cards for the best 5-card combination.
        
        Map<Integer, Long> valueCounts = cards.stream()
                .collect(Collectors.groupingBy(Card::getValue, Collectors.counting()));
                
        int maxCount = valueCounts.values().stream().mapToInt(Long::intValue).max().orElse(0);
        
        List<Integer> sortedValues = cards.stream()
                .map(Card::getValue)
                .sorted((v1, v2) -> {
                    long count1 = valueCounts.get(v1);
                    long count2 = valueCounts.get(v2);
                    if (count1 != count2) {
                        return Long.compare(count2, count1); // Higher frequency first
                    }
                    return Integer.compare(v2, v1); // Then higher value first
                })
                .limit(5)
                .collect(Collectors.toList());

        if (maxCount == 4) {
            return new HandResult(username, 8, "Four of a Kind", sortedValues);
        } else if (maxCount == 3) {
            return new HandResult(username, 4, "Three of a Kind", sortedValues);
        } else if (maxCount == 2) {
            long pairs = valueCounts.values().stream().filter(c -> c == 2).count();
            if (pairs >= 2) {
                return new HandResult(username, 3, "Two Pair", sortedValues);
            } else {
                return new HandResult(username, 2, "Pair", sortedValues);
            }
        }
        
        return new HandResult(username, 1, "High Card", sortedValues);
    }
}
