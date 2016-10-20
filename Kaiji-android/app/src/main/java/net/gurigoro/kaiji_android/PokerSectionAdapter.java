package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gurigoro.kaiji.Trump;
import net.gurigoro.kaiji.poker.PokerGrpc;
import net.gurigoro.kaiji.poker.PokerOuterClass;

import java.util.List;
import java.util.Random;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by takahito on 2016/10/17.
 */

public class PokerSectionAdapter extends BaseAdapter {
    public enum PokerGameStatus{
        ENTRY,
        BETTING,
        CARD_INPUT,
        RESULT
    }

    public static final int CARD_INPUT_BUTTPN_ID = 967;

    Context context;
    private LayoutInflater inflater;
    private List<PokerPlayer> players;
    private long gameRoomId;

    private boolean isCommunicating = false;
    private PokerGameStatus gameStatus = PokerGameStatus.ENTRY;

    private int playerPos = 0;
    private long fieldBetPoint = 0;
    private boolean isBetted = false;
    private int bettingStage = 0;

    private PokerPlayer nextPlayer(){
        playerPos++;
        if(playerPos >= players.size()) {
            playerPos = 0;
        }
        if(players.get(playerPos).isFolded()) {
            return nextPlayer();
        }else {
            return players.get(playerPos);
        }
    }

    public List<PokerPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<PokerPlayer> players) {
        this.players = players;
    }

    public long getGameRoomId() {
        return gameRoomId;
    }

    public void setGameRoomId(long gameRoomId) {
        this.gameRoomId = gameRoomId;
    }

    public boolean isCommunicating() {
        return isCommunicating;
    }

    public void setCommunicating(boolean communicating) {
        isCommunicating = communicating;
    }

    public PokerGameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(PokerGameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public PokerSectionAdapter(Context context) {
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

        final PokerPlayer player = players.get(position);

        LinearLayout innerLayout = (LinearLayout) convertView.findViewById(R.id.player_inner_layout);

        playerNameTextView.setText(player.getUserName());
        playerSubValueTextView.setText(player.getUserPoint() + "Pt.");


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
                innerLayout.addView(unEntryButton);

                break;
            }
            case BETTING: {
                View innerView = inflater.inflate(R.layout.poker_betting_layout, null);
                innerLayout.addView(innerView);
                Button callButton = (Button) innerView.findViewById(R.id.poker_call_button);
                Button raiseButton = (Button) innerView.findViewById(R.id.poker_raise_button);
                Button foldButton = (Button) innerView.findViewById(R.id.poker_fold_button);
                final EditText bettingPointField = (EditText) innerView.findViewById(R.id.poker_bet_point_edittext);

                playerSubValueTextView.append(", Bet." + player.getBetPoint() + "Pt.");

                boolean allcalled =  true;
                for (PokerPlayer pokerPlayer : players) {
                    if(pokerPlayer.isFolded()) continue;
                    if(!pokerPlayer.isCalled())  allcalled = false;
                }
                if(allcalled){
                    if(bettingStage == 0){
                        for (PokerPlayer pokerPlayer : players) {
                            pokerPlayer.setCalled(false);
                        }
                        bettingStage = 1;
                        isBetted = false;
                        new AlertDialog.Builder(context)
                                .setTitle("ファーストベット終了")
                                .setMessage("カードの交換を行ってから、セカンドベットに進みます")
                                .setPositiveButton("OK" , null)
                                .show();
                    }else{
                        gameStatus = PokerGameStatus.CARD_INPUT;
                        notifyDataSetChanged();
                    }
                }

                if(player.isFolded()){
                    playerNameTextView.setTextColor(context.getColor(android.R.color.secondary_text_dark));
                    playerNameTextView.append("(フォールド)");
                }

                if(!isBetted){
                    callButton.setVisibility(View.GONE);
                    raiseButton.setText("ベット");
                    if(player.isFolded()){
                        innerView.setVisibility(View.GONE);
                    }
                }else{
                    callButton.setVisibility(View.VISIBLE);
                    raiseButton.setText("レイズ");
                    if(position != playerPos){
                        innerView.setVisibility(View.GONE);
                    }else{
                        innerView.setVisibility(View.VISIBLE);
                    }
                }

                foldButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(context)
                                .setTitle("警告")
                                .setMessage("本当にフォールドしてよろしいですか？これ以降このプレイヤーはゲームに参加できなくなります。")
                                .setNegativeButton("キャンセル", null)
                                .setPositiveButton("フォールド", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {

                                        final ProgressDialog dialog = new ProgressDialog(context);
                                        dialog.setTitle("通信中");
                                        dialog.setMessage("フォールドしています。");
                                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                        dialog.setCancelable(false);
                                        dialog.show();

                                        new AsyncTask<Void, Void, Boolean>(){

                                            @Override
                                            protected Boolean doInBackground(Void... params) {
                                                if(ConnectConfig.OFFLINE) {
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
                                                        PokerGrpc.PokerBlockingStub stub = PokerGrpc.newBlockingStub(channel);

                                                        PokerOuterClass.FoldRequest.Builder builder = PokerOuterClass.FoldRequest.newBuilder()
                                                                .setAccessToken(key)
                                                                .setUserId(player.getUserId())
                                                                .setGameRoomId(gameRoomId);

                                                        PokerOuterClass.FoldReply reply
                                                                = stub.fold(builder.build());

                                                        return reply.getIsSucceed();
                                                    }catch (Exception e){
                                                        e.printStackTrace();
                                                        return false;
                                                    }
                                                }
                                            }

                                            @Override
                                            protected void onPostExecute(Boolean result) {
                                                dialog.dismiss();
                                                if(result) {
                                                    player.setFolded(true);
                                                    if(fieldBetPoint != 0) nextPlayer();
                                                }else{
                                                    new AlertDialog.Builder(context)
                                                            .setTitle("通信に失敗しました")
                                                            .setMessage("管理者に問い合わせてください")
                                                            .setPositiveButton("OK", null)
                                                            .show();
                                                }
                                                notifyDataSetChanged();
                                            }

                                        }.execute();

                                    }
                                })
                                .show();
                    }
                });

                callButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final ProgressDialog dialog = new ProgressDialog(context);
                        dialog.setTitle("通信中");
                        dialog.setMessage("コールしています。");
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCancelable(false);
                        dialog.show();

                        new AsyncTask<Void, Void, Boolean>(){

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                if(ConnectConfig.OFFLINE) {
                                    player.setCalled(true);
                                    player.setBetPoint(fieldBetPoint);
                                    nextPlayer();
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
                                        PokerGrpc.PokerBlockingStub stub = PokerGrpc.newBlockingStub(channel);

                                        PokerOuterClass.CallRequest.Builder builder = PokerOuterClass.CallRequest.newBuilder()
                                                .setAccessToken(key)
                                                .setUserId(player.getUserId())
                                                .setGameRoomId(gameRoomId);
                                        PokerPlayer nextPlayer = null;


                                        PokerOuterClass.CallReply reply
                                                = stub.call(builder.build());

                                        switch (reply.getResult()){
                                            case SUCCEED:
                                                player.setCalled(true);
                                                player.setBetPoint(fieldBetPoint);
                                                nextPlayer();
                                                return true;
                                            case NO_ENOUGH_POINTS:
                                                new AlertDialog.Builder(context)
                                                        .setTitle("ポイント不足")
                                                        .setMessage("所持ポイントが足りません。")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                                return true;
                                            case NOT_ENOUGH_TO_RAISE:
                                                break;
                                            case UNKNOWN_FAILED:
                                                break;
                                            case UNRECOGNIZED:
                                                break;
                                        }
                                        return false;
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                dialog.dismiss();
                                if(result) {
                                }else{
                                    new AlertDialog.Builder(context)
                                            .setTitle("通信に失敗しました")
                                            .setMessage("管理者に問い合わせてください")
                                            .setPositiveButton("OK", null)
                                            .show();
                                }
                                notifyDataSetChanged();
                            }

                        }.execute();

                    }
                });

                raiseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long tmp = 0;

                        try {
                            tmp = Long.parseLong(bettingPointField.getText().toString()) * 100;
                        }catch (Exception e){
                            if(e instanceof NumberFormatException){
                                new AlertDialog.Builder(context)
                                        .setMessage("ベット額を正しく入力してください。")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }else{
                                e.printStackTrace();
                            }
                            return;
                        }

                        final long betPoint = tmp;

                        final ProgressDialog dialog = new ProgressDialog(context);
                        dialog.setTitle("通信中");
                        dialog.setMessage(fieldBetPoint != 0 ? "コールしています。" : "ベットしています。");
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCancelable(false);
                        dialog.show();

                        new AsyncTask<Void, Void, Boolean>(){

                            @Override
                            protected Boolean doInBackground(Void... params) {
                                if(ConnectConfig.OFFLINE) {
                                    fieldBetPoint += betPoint;
                                    player.setBetPoint(fieldBetPoint);
                                    player.setCalled(false);
                                    isBetted = true;
                                    playerPos = position;
                                    nextPlayer();
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
                                        PokerGrpc.PokerBlockingStub stub = PokerGrpc.newBlockingStub(channel);

                                        PokerOuterClass.RaiseRequest.Builder builder = PokerOuterClass.RaiseRequest.newBuilder()
                                                .setAccessToken(key)
                                                .setUserId(player.getUserId())
                                                .setGameRoomId(gameRoomId)
                                                .setBetPoints(betPoint);

                                        playerPos = position;

                                        PokerOuterClass.RaiseReply reply
                                                = stub.raise(builder.build());

                                        fieldBetPoint = reply.getFieldBetPoints();


                                        switch (reply.getResult()){
                                            case SUCCEED:
                                                player.setBetPoint(fieldBetPoint);
                                                player.setCalled(false);
                                                nextPlayer();
                                                isBetted = true;
                                                return true;
                                            case NO_ENOUGH_POINTS:
                                                new AlertDialog.Builder(context)
                                                        .setTitle("ポイント不足")
                                                        .setMessage("所持ポイントが足りません。")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                                return true;
                                            case NOT_ENOUGH_TO_RAISE:
                                                new AlertDialog.Builder(context)
                                                        .setTitle("レイズ失敗")
                                                        .setMessage("前の人のベット額に満たないレイズはできません。")
                                                        .setPositiveButton("OK" , null)
                                                        .show();
                                                return true;
                                            case UNKNOWN_FAILED:
                                                break;
                                            case UNRECOGNIZED:
                                                break;
                                        }
                                        return false;
                                    }catch (Exception e){
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                dialog.dismiss();
                                if(result) {
                                }else{
                                    new AlertDialog.Builder(context)
                                            .setTitle("通信に失敗しました")
                                            .setMessage("管理者に問い合わせてください")
                                            .setPositiveButton("OK", null)
                                            .show();
                                }
                                notifyDataSetChanged();
                            }

                        }.execute();
                    }
                });


                break;
            }
            case CARD_INPUT: {
                View innerView = inflater.inflate(R.layout.poker_card_input_layout, null);
                innerLayout.addView(innerView);
                LinearLayout cardsLayout = (LinearLayout) innerView.findViewById(R.id.poker_card_input_card_layout);

                boolean isAllEnd = true;
                for (PokerPlayer pokerPlayer : players) {
                    if(pokerPlayer.getHand() == PokerPlayer.PokerHand.UNKNOWN) isAllEnd = false;
                }
                if(isAllEnd){
                    final ProgressDialog dialog = new ProgressDialog(context);
                    dialog.setTitle("通信中");
                    dialog.setMessage("結果を取得しています。");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();

                    new AsyncTask<Void, Void, Boolean>(){

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if(ConnectConfig.OFFLINE) {
                                Random random = new Random();
                                player.setGotPoints(random.nextInt());
                                if(random.nextBoolean()){
                                    player.setGameResult(GamePlayer.GameResult.WIN);
                                }else{
                                    player.setGameResult(GamePlayer.GameResult.LOSE);
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
                                    PokerGrpc.PokerBlockingStub stub = PokerGrpc.newBlockingStub(channel);

                                    PokerOuterClass.GetGameResultRequest.Builder builder = PokerOuterClass.GetGameResultRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);

                                    PokerOuterClass.GetGameResultReply reply
                                            = stub.getGameResult(builder.build());

                                    if(!reply.getIsSucceed()) return false;

                                    for (PokerOuterClass.PlayerResult result : reply.getPlayerResultsList()) {
                                        for (PokerPlayer pokerPlayer : players) {
                                            if(result.getUserId() != pokerPlayer.getUserId()) continue;
                                            switch (result.getGameResult()){
                                                case LOSE:
                                                    pokerPlayer.setGameResult(GamePlayer.GameResult.LOSE);
                                                    break;
                                                case TIE:
                                                    pokerPlayer.setGameResult(GamePlayer.GameResult.TIE);
                                                    break;
                                                case WIN:
                                                    pokerPlayer.setGameResult(GamePlayer.GameResult.WIN);
                                                    break;
                                                case UNRECOGNIZED:
                                                    break;
                                            }
                                            pokerPlayer.setGotPoints(result.getGotPoints());
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
                            if(result) {
                                gameStatus = PokerGameStatus.RESULT;
                            }else{
                                new AlertDialog.Builder(context)
                                        .setTitle("通信に失敗しました")
                                        .setMessage("管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            notifyDataSetChanged();
                        }

                    }.execute();
                }

                boolean isImputtedFiveCards
                        = player.getHand() == PokerPlayer.PokerHand.UNKNOWN
                        && player.getCards().size() == 5;
                if(isImputtedFiveCards){
                    final ProgressDialog dialog = new ProgressDialog(context);
                    dialog.setTitle("通信中");
                    dialog.setMessage("ハンドを取得しています。");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();

                    new AsyncTask<Void, Void, Boolean>(){

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if(ConnectConfig.OFFLINE) {
                                player.setHand(PokerPlayer.PokerHand.HIGH_CARDS);
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
                                    PokerGrpc.PokerBlockingStub stub = PokerGrpc.newBlockingStub(channel);

                                    PokerOuterClass.SetPlayersCardsRequest.Builder builder = PokerOuterClass.SetPlayersCardsRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setUserId(player.getUserId())
                                            .setGameRoomId(gameRoomId);

                                    Trump.TrumpCards.Builder cardsBuilder = Trump.TrumpCards.newBuilder();
                                    for (TrumpCard trumpCard : player.getCards()) {
                                        cardsBuilder.addCards(trumpCard.getGrpcTrumpCard());
                                    }
                                    builder.setPlayerCards(cardsBuilder.build());

                                    PokerOuterClass.SetPlayersCardsReply reply
                                            = stub.setPlayersCards(builder.build());

                                    if(!reply.getIsSucceed()) return false;

                                    switch(reply.getHand()){
                                        case UNKNOWN:
                                            break;
                                        case HIGH_CARDS:
                                            player.setHand(PokerPlayer.PokerHand.HIGH_CARDS);
                                            break;
                                        case ONE_PAIR:
                                            player.setHand(PokerPlayer.PokerHand.ONE_PAIR);
                                            break;
                                        case TWO_PAIRS:
                                            player.setHand(PokerPlayer.PokerHand.TWO_PAIRS);
                                            break;
                                        case THREE_OF_A_KIND:
                                            player.setHand(PokerPlayer.PokerHand.THREE_OF_A_KIND);
                                            break;
                                        case STRAIGHT:
                                            player.setHand(PokerPlayer.PokerHand.STRAIGHT);
                                            break;
                                        case FLUSH:
                                            player.setHand(PokerPlayer.PokerHand.FLUSH);
                                            break;
                                        case FULL_HOUSE:
                                            player.setHand(PokerPlayer.PokerHand.FULL_HOUSE);
                                            break;
                                        case FOUR_OF_A_KIND:
                                            player.setHand(PokerPlayer.PokerHand.FOUR_OF_A_KIND);
                                            break;
                                        case STRAIGHT_FLUSH:
                                            player.setHand(PokerPlayer.PokerHand.STRAIGHT_FLUSH);
                                            break;
                                        case ROYAL_STRAIGHT_FLUSH:
                                            player.setHand(PokerPlayer.PokerHand.ROYAL_STRAIGHT_FLUSH);
                                            break;
                                        case UNRECOGNIZED:
                                            break;
                                    }

                                    return false;
                                }catch (Exception e){
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            dialog.dismiss();
                            if(result) {
                            }else{
                                new AlertDialog.Builder(context)
                                        .setTitle("通信に失敗しました")
                                        .setMessage("管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            notifyDataSetChanged();
                        }

                    }.execute();
                }

                switch (player.getHand()){
                    case UNKNOWN:
                        playerSubValueTextView.setText("");
                        break;
                    case HIGH_CARDS:
                        playerSubValueTextView.setText("ハイカード");
                        break;
                    case ONE_PAIR:
                        playerSubValueTextView.setText("1ペア");
                        break;
                    case TWO_PAIRS:
                        playerSubValueTextView.setText("2ペア");
                        break;
                    case THREE_OF_A_KIND:
                        playerSubValueTextView.setText("3カード");
                        break;
                    case STRAIGHT:
                        playerSubValueTextView.setText("ストレート");
                        break;
                    case FLUSH:
                        playerSubValueTextView.setText("フラッシュ");
                        break;
                    case FULL_HOUSE:
                        playerSubValueTextView.setText("フルハウス");
                        break;
                    case FOUR_OF_A_KIND:
                        playerSubValueTextView.setText("4カード");
                        break;
                    case STRAIGHT_FLUSH:
                        playerSubValueTextView.setText("ストレートフラッシュ");
                        break;
                    case ROYAL_STRAIGHT_FLUSH:
                        playerSubValueTextView.setText("ロイヤルストレートフラッシュ");
                        break;
                }

                for(int i = 0; i < 5; i++){
                    ImageView imageView = new ImageView(context);
                    imageView.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    Util.convertDpToPx(context, 100),
                                    Util.convertDpToPx(context, 150)));
                    if(player.getCards().size() > i){
                        imageView.setImageDrawable(player.getCards().get(i).getDrawable(context));
                    }else{
                        imageView.setImageDrawable(context.getDrawable(R.drawable.z01));
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ((ListView)parent).performItemClick(v, position, CARD_INPUT_BUTTPN_ID);
                            }
                        });
                    }
                    cardsLayout.addView(imageView);
                }
                break;
            }
            case RESULT: {
                View innerView = inflater.inflate(R.layout.game_result_layout, null);
                innerLayout.addView(innerView);
                TextView messageTextView = (TextView) innerView.findViewById(R.id.game_result_message_textview);
                TextView pointTextView = (TextView) innerView.findViewById(R.id.game_result_point_textview);
                switch (player.getGameResult()){
                    case WIN:
                        messageTextView.setText("勝ち");
                        pointTextView.setTextColor(Color.RED);
                        break;
                    case LOSE:
                        messageTextView.setText("負け");
                        pointTextView.setTextColor(Color.BLUE);
                        break;
                    case TIE:
                        messageTextView.setText("引き分け");
                        break;
                }
                pointTextView.setText(String.format("%+dPt.", player.getGotPoints()));
                playerSubValueTextView.setText((player.getUserPoint() + player.getGotPoints()) + "Pt.");

                ((ListView)parent).performItemClick(null, 0, 0);

                break;
            }
        }

        return convertView;

    }
}
