package net.gurigoro.kaiji_android;

import java.util.List;

/**
 * Created by takahito on 2016/10/14.
 */

public class BlackJackPlayer {
    public BlackJackPlayer() {
        cards = new List[2];
        cardPoint = new long[2];
    }

    // if userId is -1, it's a dealer.
    public static final int DEALER_ID = -1;
    int userId;
    String userName;
    long userPoint;
    long betPoint;

    List<TrumpCard> cards[];
    long cardPoint[];

    boolean isSplit;
    boolean isBust;
    boolean canHit;
    boolean canStand;
    boolean canSplit;
    boolean canDoubleDown;


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getUserPoint() {
        return userPoint;
    }

    public void setUserPoint(long userPoint) {
        this.userPoint = userPoint;
    }

    public long getBetPoint() {
        return betPoint;
    }

    public void setBetPoint(long betPoint) {
        this.betPoint = betPoint;
    }

    public List<TrumpCard>[] getCards() {
        return cards;
    }

    public void setCards(List<TrumpCard>[] cards) {
        this.cards = cards;
    }

    public long[] getCardPoint() {
        return cardPoint;
    }

    public void setCardPoint(long[] cardPoint) {
        this.cardPoint = cardPoint;
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
