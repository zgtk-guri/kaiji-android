package net.gurigoro.kaiji_android;

import android.content.Context;
import android.graphics.drawable.Drawable;

import net.gurigoro.kaiji.Trump;

/**
 * Created by takahito on 2016/10/14.
 */

public class TrumpCard {
    public enum TrumpSuit{
        SPADE,
        CLUB,
        HEART,
        DIAMOND
    }

    // 1 - 13
    int number;
    TrumpSuit suit;
    boolean isFaceDown;
    boolean isJoker;

    public TrumpCard(){
        number = 0;
        suit = TrumpSuit.SPADE;
        isFaceDown = true;
    }

    public TrumpCard(int number, TrumpSuit suit){
        this.number = number;
        this.suit = suit;
        isFaceDown = false;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number){
        if(number < 1 || number > 13){
            return;
        }
        this.number = number;
    }

    public TrumpSuit getSuit() {
        return suit;
    }

    public void setSuit(TrumpSuit suit) {
        this.suit = suit;
    }

    public boolean isFaceDown() {
        return isFaceDown;
    }

    public void setFaceDown(boolean faceDown) {
        isFaceDown = faceDown;
    }

    public boolean isJoker() {
        return isJoker;
    }

    public void setJoker(boolean joker) {
        isJoker = joker;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TrumpCard
                && ((TrumpCard)o).number == number
                &&((TrumpCard)o).suit == suit;
    }

    public Drawable getDrawable(Context context){
        if(isFaceDown){
            if(suit == TrumpSuit.HEART || suit == TrumpSuit.DIAMOND){
                return context.getDrawable(R.drawable.z02);
            }else{
                return  context.getDrawable(R.drawable.z01);
            }
        }else if(isJoker) {
            return context.getDrawable(R.drawable.x01);
        }else{
            String resName = "z01";
            switch (suit){
                case SPADE:
                    resName = String.format("s%02d", number);
                    break;
                case CLUB:
                    resName = String.format("c%02d", number);
                    break;
                case HEART:
                    resName = String.format("h%02d", number);
                    break;
                case DIAMOND:
                    resName = String.format("d%02d", number);
                    break;
            }
            return context.getDrawable(context.getResources().getIdentifier(resName, "drawable", context.getPackageName()));
        }

    }
}
