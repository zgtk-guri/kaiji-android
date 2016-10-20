package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gurigoro.kaiji.poker.PokerGrpc;
import net.gurigoro.kaiji.poker.PokerOuterClass;

import java.util.List;

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
    public View getView(final int position, View convertView, ViewGroup parent) {
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

                if(isBetted){
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
                break;
            }
            case RESULT: {
                break;
            }
        }

        return convertView;

    }
}
