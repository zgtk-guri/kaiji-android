package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.gurigoro.kaiji.Trump;
import net.gurigoro.kaiji.blackjack.BlackJackGrpc;
import net.gurigoro.kaiji.blackjack.BlackJackOuterClass;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by takahito on 2016/10/14.
 */

public class BlackJackSectionAdapter extends BaseAdapter {
    public enum BlackJackGameStatus{
        ENTRY,
        BETTING,
        FIRST_DEAL,
        ACTIONS,
        RESULT
    }


    Context context;
    LayoutInflater inflater;
    List<BlackJackPlayer> players;

    BlackJackGameStatus gameStatus = BlackJackGameStatus.ENTRY;
    public BlackJackGameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(BlackJackGameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    long gameRoomId;

    public long getGameRoomId() {
        return gameRoomId;
    }

    public void setGameRoomId(long gameRoomId) {
        this.gameRoomId = gameRoomId;
    }

    public List<BlackJackPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<BlackJackPlayer> players) {
        this.players = players;
    }

    public BlackJackSectionAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return players.size();
    }

    @Override
    public Object getItem(int position) {
        return players.get(position);
    }

    @Override
    public long getItemId(int position) {
        return players.get(position).getUserId();
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        convertView = inflater.inflate(R.layout.player_section,parent,false);
        final TextView playerNameTextView = (TextView) convertView.findViewById(R.id.player_name_textview);
        TextView playerSubValueTextView = (TextView) convertView.findViewById(R.id.player_sub_value_textview);

        final BlackJackPlayer player = players.get(position);

        LinearLayout innerLayout = (LinearLayout) convertView.findViewById(R.id.player_inner_layout);

        playerNameTextView.setText(player.getUserName());
        if(player.getUserId() == BlackJackPlayer.DEALER_ID){
            playerSubValueTextView.setText("");
        }else{
            playerSubValueTextView.setText(player.getUserPoint() + "Pt.");
        }


        switch (gameStatus){
            case ENTRY: {
                Button unEntryButton = new Button(context);
                unEntryButton.setText("退席");
                unEntryButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                unEntryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        players.remove(position);
                        notifyDataSetChanged();
                    }
                });
                if(player.getUserId() != BlackJackPlayer.DEALER_ID){
                    innerLayout.addView(unEntryButton);
                }
                break;
            }
            case BETTING: {
                if(player.getUserId() == BlackJackPlayer.DEALER_ID){
                    break;
                }
                View innerView = inflater.inflate(R.layout.bj_betting_layout, null);
                innerLayout.addView(innerView);
                final Button bettingButton = (Button) innerView.findViewById(R.id.bj_betting_button);
                final EditText bettingPointField = (EditText) innerView.findViewById(R.id.bj_bet_point_field);

                bettingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int betPoint = Integer.parseInt(bettingPointField.getText().toString()) * 100;

                        bettingButton.setEnabled(false);
                        bettingPointField.setEnabled(false);
                        bettingButton.setText("ベット中...");
                        new AsyncTask<Void, Void, Boolean>(){

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                if(ConnectConfig.OFFLINE) {
                                    player.setBetted(true);
                                    player.setBetPoint(betPoint);
                                    return true;
                                }else{
                                    try {
                                        String addr = ConnectConfig.getServerAddress(context);
                                        String key = ConnectConfig.getAccessKey(context);
                                        int port = ConnectConfig.getServerPort(context);

                                        ManagedChannel channel = ManagedChannelBuilder
                                                .forAddress(addr, port)
                                                .usePlaintext(true)
                                                .build();
                                        BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                        BlackJackOuterClass.BettingRequest.Builder builder = BlackJackOuterClass.BettingRequest.newBuilder()
                                                .setAccessToken(key)
                                                .setUserId(player.getUserId())
                                                .setGameRoomId(gameRoomId)
                                                .setBetPoints(betPoint);

                                        BlackJackOuterClass.BettingReply reply
                                                = stub.betting(builder.build());

                                        switch (reply.getResult()){
                                            case SUCCEED:
                                                player.setBetPoint(betPoint);
                                                player.setBetted(true);
                                                break;
                                            case NO_ENOUGH_POINTS:
                                                ((Activity)context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        bettingButton.setEnabled(true);
                                                        bettingButton.setText("ベット");
                                                        bettingPointField.setEnabled(true);
                                                        new AlertDialog.Builder(context)
                                                                .setTitle("ベット失敗")
                                                                .setMessage("ポイントが足りません。")
                                                                .setPositiveButton("OK", null)
                                                                .show();
                                                    }
                                                });
                                                break;
                                            case ALREADY_BETTED:
                                                player.setBetted(true);
                                                break;
                                            case UNKNOWN_FAILED:
                                                return false;
                                            case UNRECOGNIZED:
                                                return false;
                                        }
                                        return true;
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                if(result){
                                    bettingButton.setText("ベット済");
                                    bettingPointField.setText(String.valueOf(player.getBetPoint() / 100));

                                    boolean allBetted = true;
                                    for (BlackJackPlayer blackJackPlayer : players) {
                                        if(blackJackPlayer.getUserId() == BlackJackPlayer.DEALER_ID) continue;
                                        if(!blackJackPlayer.isBetted()) allBetted = false;
                                    }
                                    if(allBetted) {
                                        gameStatus = BlackJackGameStatus.FIRST_DEAL;
                                        notifyDataSetChanged();
                                    }
                                }else{
                                    bettingButton.setEnabled(true);
                                    bettingButton.setText("ベット");
                                    bettingPointField.setEnabled(true);
                                    new AlertDialog.Builder(context)
                                            .setTitle("通信に失敗しました")
                                            .setMessage("ベットに失敗しました。再試行するか、管理者に問い合わせてください")
                                            .setPositiveButton("OK", null)
                                            .show();
                                }
                            }

                        }.execute();
                    }
                });
                break;
            }
            case FIRST_DEAL: {
                if(player.getUserId() != BlackJackPlayer.DEALER_ID) {
                    playerSubValueTextView.setText("ベット " + player.getBetPoint() + "Pt.");
                }

                boolean allCardSet = true;
                for (BlackJackPlayer blackJackPlayer : players) {
                    if(blackJackPlayer.getUserId() == BlackJackPlayer.DEALER_ID) continue;
                    if(!blackJackPlayer.isFirstDealed()) allCardSet = false;
                }
                if(allCardSet){
                    gameStatus = BlackJackGameStatus.ACTIONS;
                    notifyDataSetChanged();

                    final ProgressDialog dialog = new ProgressDialog(context);
                    dialog.setTitle("通信中");
                    dialog.setMessage("サーバにカード情報を送信しています。");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();


                    new AsyncTask<Void, Void, Boolean>(){

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if(ConnectConfig.OFFLINE) {
                                for (BlackJackPlayer blackJackPlayer : players) {
                                    if(blackJackPlayer.getUserId() == BlackJackPlayer.DEALER_ID) continue;
                                    // ポイントは適当。
                                    blackJackPlayer.getCardPoint()[0]
                                            = blackJackPlayer.getCards()[0].get(0).getNumber()
                                            + blackJackPlayer.getCards()[0].get(1).getNumber();
                                    if(blackJackPlayer.getCardPoint()[0] > 21){
                                        blackJackPlayer.setBust(true);
                                    }
                                    if(blackJackPlayer.getUserPoint() > (blackJackPlayer.getBetPoint() * 2) && !blackJackPlayer.isBust()) {
                                        if (blackJackPlayer.getCards()[0].get(0).equals(blackJackPlayer.getCards()[0].get(1))) {
                                            blackJackPlayer.setCanSplit(true);
                                        }
                                        blackJackPlayer.setCanDoubleDown(true);
                                    }
                                    if(!blackJackPlayer.isBust()){
                                        blackJackPlayer.setCanHit(true);
                                        blackJackPlayer.setCanStand(true);
                                    }
                                }
                                return true;
                            }else{
                                try {
                                    String addr = ConnectConfig.getServerAddress(context);
                                    String key = ConnectConfig.getAccessKey(context);
                                    int port = ConnectConfig.getServerPort(context);

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                    BlackJackOuterClass.SetFirstDealedCardsRequest.Builder playerCardsBuilder = BlackJackOuterClass.SetFirstDealedCardsRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);
                                    BlackJackOuterClass.SetFirstDealersCardRequest.Builder dealerCardBuilder = BlackJackOuterClass.SetFirstDealersCardRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);

                                    for (BlackJackPlayer blackJackPlayer : players) {
                                        if(blackJackPlayer.getUserId() == BlackJackPlayer.DEALER_ID){
                                            dealerCardBuilder.setCard(blackJackPlayer.getCards()[0].get(0).getGrpcTrumpCard());
                                        }else {
                                            Trump.TrumpCards.Builder builder1 = Trump.TrumpCards.newBuilder();
                                            for (TrumpCard trumpCard : blackJackPlayer.getCards()[0]) {
                                                builder1.addCards(trumpCard.getGrpcTrumpCard());
                                            }

                                            BlackJackOuterClass.FirstDealPlayerCards firstDealPlayerCards
                                                    = BlackJackOuterClass.FirstDealPlayerCards.newBuilder()
                                                    .setUserId(blackJackPlayer.getUserId())
                                                    .setCards(builder1.build())
                                                    .build();

                                            playerCardsBuilder.addPlayerCards(firstDealPlayerCards);
                                        }

                                    }

                                    BlackJackOuterClass.SetFirstDealersCardReply dealersCardReply = stub.setFirstDealersCard(dealerCardBuilder.build());

                                    BlackJackOuterClass.SetFirstDealedCardsReply firstDealedCardsReply
                                            = stub.setFirstDealedCards(playerCardsBuilder.build());

                                    if(!firstDealedCardsReply.getIsSucceed() || !dealersCardReply.getIsSucceed()){
                                        return false;
                                    }

                                    for (BlackJackOuterClass.AllowedPlayerActions actions : firstDealedCardsReply.getActionsList()) {
                                        BlackJackPlayer blackJackPlayer = null;
                                        for (BlackJackPlayer jackPlayer : players) {
                                            if(jackPlayer.getUserId() == actions.getUserId()){
                                                blackJackPlayer = jackPlayer;
                                                break;
                                            }
                                        }
                                        if(blackJackPlayer == null) continue;
                                        blackJackPlayer.getCardPoint()[0] = actions.getCardPoints();

                                        for (BlackJackOuterClass.PlayerAction action : actions.getActionsList()) {
                                            switch (action){
                                                case UNKNOWN:
                                                    break;
                                                case HIT:
                                                    blackJackPlayer.setCanHit(true);
                                                    break;
                                                case STAND:
                                                    blackJackPlayer.setCanStand(true);
                                                    break;
                                                case SPLIT:
                                                    blackJackPlayer.setCanSplit(true);
                                                    break;
                                                case DOUBLEDOWN:
                                                    blackJackPlayer.setCanDoubleDown(true);
                                                    break;
                                                case UNRECOGNIZED:
                                                    break;
                                            }
                                        }

                                    }
                                    return true;
                                }catch (Exception e){
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            dialog.dismiss();
                            if(result){
                                notifyDataSetChanged();
                            }else{
                                new AlertDialog.Builder(context)
                                        .setTitle("通信に失敗しました")
                                        .setMessage("管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }

                    }.execute();

                    break;
                }


                View innerView = inflater.inflate(R.layout.bj_first_deal_layout, null);
                innerLayout.addView(innerView);
                ImageView cardOneImageView = (ImageView) innerView.findViewById(R.id.bj_first_deal_card_1);
                ImageView cardTwoImageView = (ImageView) innerView.findViewById(R.id.bj_first_deal_card_2);
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView)parent).performItemClick(v, position, v.getId());
                    }
                };
                if(player.getCards()[0].size() == 0){
                    cardOneImageView.setImageDrawable(context.getDrawable(R.drawable.z01));
                    cardTwoImageView.setVisibility(View.GONE);
                    cardOneImageView.setOnClickListener(onClickListener);
                }else if(player.getCards()[0].size() == 1){
                    cardOneImageView.setImageDrawable(player.getCards()[0].get(0).getDrawable(context));
                    cardOneImageView.setOnClickListener(null);
                    cardTwoImageView.setImageDrawable(context.getDrawable(R.drawable.z01));
                    cardTwoImageView.setVisibility(View.VISIBLE);
                    if(player.getUserId() != BlackJackPlayer.DEALER_ID) {
                        cardTwoImageView.setOnClickListener(onClickListener);
                        player.setFirstDealed(true);
                    }
                }else{
                    cardOneImageView.setImageDrawable(player.getCards()[0].get(0).getDrawable(context));
                    cardOneImageView.setOnClickListener(null);
                    cardTwoImageView.setImageDrawable(player.getCards()[0].get(1).getDrawable(context));
                    cardTwoImageView.setOnClickListener(null);
                    player.setFirstDealed(true);
                }
                break;
            }
            case ACTIONS: {

                break;
            }
            case RESULT: {
                break;
            }
        }

        return convertView;
    }
}
