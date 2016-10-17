package net.gurigoro.kaiji_android;

/**
 * Created by takahito on 2016/10/17.
 */

public abstract class GamePlayer {
    private int userId;
    private String userName;
    private long userPoint;
    private long betPoint;

    private boolean isCommunicating = false;


    public enum GameResult{
        WIN,
        LOSE,
        TIE
    }

    private GameResult gameResult;
    private long gotPoints = 0;

    public long getGotPoints() {
        return gotPoints;
    }

    public void setGotPoints(long gotPoints) {
        this.gotPoints = gotPoints;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public void setGameResult(GameResult gameResult) {
        this.gameResult = gameResult;
    }

    public boolean isCommunicating() {
        return isCommunicating;
    }

    public void setCommunicating(boolean communicating) {
        isCommunicating = communicating;
    }

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


}
