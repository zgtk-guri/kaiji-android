package net.gurigoro.kaiji_android;

/**
 * Created by takahito on 2016/10/22.
 */

public class BaccaratPlayer extends GamePlayer {
    public enum BetSide{
        PLAYER,
        BANKER,
        TIE,
        NONE
    }


    private BetSide betSide = BetSide.NONE;


    public BetSide getBetSide() {
        return betSide;
    }

    public void setBetSide(BetSide betSide) {
        this.betSide = betSide;
    }
}
