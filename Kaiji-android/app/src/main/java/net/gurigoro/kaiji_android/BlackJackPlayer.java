package net.gurigoro.kaiji_android;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by takahito on 2016/10/14.
 */

public class BlackJackPlayer extends GamePlayer{
    public BlackJackPlayer() {
        cards = new List[2];
        cards[0] = new ArrayList<>();
        cards[1] = new ArrayList<>();
        cardPoint = new long[2];
    }

    // if userId is -1, it's a dealer.
    public static final int DEALER_ID = -1;

    private List<TrumpCard> cards[];
    private long cardPoint[];


    private boolean isBetted = false;
    private boolean isFirstDealed = false;

    private boolean isSplit = false;
    private boolean isBust = false;
    private boolean isBustSecondHands = false;
    private boolean canHit = false;
    private boolean canHitSecondHands = false;
    private boolean canStand = false;
    private boolean canStandSecondHands = false;
    private boolean canSplit = false;
    private boolean canDoubleDown = false;




    public boolean isCanStandSecondHands() {
        return canStandSecondHands;
    }

    public void setCanStandSecondHands(boolean canStandSecondHands) {
        this.canStandSecondHands = canStandSecondHands;
    }

    public boolean isBustSecondHands() {
        return isBustSecondHands;
    }

    public void setBustSecondHands(boolean bustSecondHands) {
        isBustSecondHands = bustSecondHands;
    }

    public boolean isCanHitSecondHands() {
        return canHitSecondHands;
    }

    public void setCanHitSecondHands(boolean canHitSecondHands) {
        this.canHitSecondHands = canHitSecondHands;
    }

    public boolean isFirstDealed() {
        return isFirstDealed;
    }

    public void setFirstDealed(boolean firstDealed) {
        isFirstDealed = firstDealed;
    }

    public boolean isBetted() {
        return isBetted;
    }

    public void setBetted(boolean betted) {
        isBetted = betted;
    }


    public List<TrumpCard>[] getCards() {
        return cards;
    }

    public long[] getCardPoint() {
        return cardPoint;
    }

    public boolean isSplit() {
        return isSplit;
    }

    public void setSplit(boolean split) {
        isSplit = split;
    }

    public boolean isBust() {
        return isBust;
    }

    public void setBust(boolean bust) {
        isBust = bust;
    }

    public boolean isCanHit() {
        return canHit;
    }

    public void setCanHit(boolean canHit) {
        this.canHit = canHit;
    }

    public boolean isCanStand() {
        return canStand;
    }

    public void setCanStand(boolean canStand) {
        this.canStand = canStand;
    }

    public boolean isCanSplit() {
        return canSplit;
    }

    public void setCanSplit(boolean canSplit) {
        this.canSplit = canSplit;
    }

    public boolean isCanDoubleDown() {
        return canDoubleDown;
    }

    public void setCanDoubleDown(boolean canDoubleDown) {
        this.canDoubleDown = canDoubleDown;
    }
}
