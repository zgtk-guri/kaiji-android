package net.gurigoro.kaiji_android;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by takahito on 2016/10/17.
 */

public class PokerPlayer extends GamePlayer{
    public PokerPlayer() {
        cards = new ArrayList<>();
    }

    private List<TrumpCard> cards;
    private PokerHand hand = PokerHand.UNKNOWN;

    private boolean isCalled = false;
    private boolean isFolded = false;


    public enum PokerHand {
        UNKNOWN,
        HIGH_CARDS,
        ONE_PAIR,
        TWO_PAIRS,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_STRAIGHT_FLUSH
    }

    public List<TrumpCard> getCards() {
        return cards;
    }

    public PokerHand getHand() {
        return hand;
    }

    public void setHand(PokerHand hand) {
        this.hand = hand;
    }

    public boolean isCalled() {
        return isCalled;
    }

    public void setCalled(boolean called) {
        isCalled = called;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

}
