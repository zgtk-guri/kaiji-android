package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gurigoro.kaiji.baccarat.BaccaratGrpc;
import net.gurigoro.kaiji.baccarat.BaccaratOuterClass;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static net.gurigoro.kaiji_android.BaccaratSectionAdapter.GameStatus.BETTING;
import static net.gurigoro.kaiji_android.BaccaratSectionAdapter.GameStatus.RESULT;

/**
 * Created by takahito on 2016/10/22.
 */

public class BaccaratSectionAdapter extends BaseAdapter {


    public enum GameStatus{
        ENTRY,
        BETTING,
        RESULT
    }

    private Context context;
    private LayoutInflater inflater;
    private List<BaccaratPlayer> players;
    private long gameRoomId;

    private boolean isCommunicating = false;
    private GameStatus gameStatus = GameStatus.ENTRY;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<BaccaratPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<BaccaratPlayer> players) {
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

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public BaccaratSectionAdapter(Context context) {
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

        final BaccaratPlayer player = players.get(position);

        LinearLayout innerLayout = (LinearLayout) convertView.findViewById(R.id.player_inner_layout);

        playerNameTextView.setText(player.getUserName());
        if(player.getUserId() == BlackJackPlayer.DEALER_ID){
            playerSubValueTextView.setText("");
        }else{
            playerSubValueTextView.setText(player.getUserPoint() + "Pt.");
        }

        switch (gameStatus) {
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
                if (player.getUserId() != BlackJackPlayer.DEALER_ID) {
                    innerLayout.addView(unEntryButton);
                }
                break;
            }

            case BETTING:{
                View innerView = inflater.inflate(R.layout.baccarat_betting_layout, null);
                innerLayout.addView(innerView);
                final EditText betPointField = (EditText) innerView.findViewById(R.id.baccarat_bet_point_field);
                final Button playerBetButton = (Button) innerView.findViewById(R.id.baccarat_betting_player_button);
                Button bankerBetButton = (Button) innerView.findViewById(R.id.baccarat_betting_banker_button);
                Button tieBetButton = (Button) innerView.findViewById(R.id.baccarat_betting_tie_button);

                if (player.getBetSide() != BaccaratPlayer.BetSide.NONE) {
                    playerBetButton.setEnabled(false);
                    bankerBetButton.setEnabled(false);
                    tieBetButton.setEnabled(false);

                }

                boolean allBet = true;
                for (BaccaratPlayer baccaratPlayer : players) {
                    if (baccaratPlayer.getBetSide() == BaccaratPlayer.BetSide.NONE) allBet = false;
                }
                if (allBet && !isCommunicating()) {
                    setCommunicating(true);
                    final ProgressDialog dialog = new ProgressDialog(context);
                    dialog.setTitle("通信中");
                    dialog.setMessage("通信中です。");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();

                    new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if (ConnectConfig.OFFLINE) {
                                // TODO: not implemented
                                return true;
                            } else {
                                try {
                                    String addr = ConnectConfig.getServerAddress(context);
                                    String key = ConnectConfig.getAccessKey(context);
                                    int port = ConnectConfig.getServerPort(context);

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BaccaratGrpc.BaccaratBlockingStub stub = BaccaratGrpc.newBlockingStub(channel);

                                    BaccaratOuterClass.StartOpeningCardsRequest.Builder showCardsBuilder = BaccaratOuterClass.StartOpeningCardsRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);

                                    BaccaratOuterClass.GetGameResultRequest.Builder resultBuilder = BaccaratOuterClass.GetGameResultRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);

                                    BaccaratOuterClass.DestroyGameRoomRequest.Builder destroyBuilder = BaccaratOuterClass.DestroyGameRoomRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId);

                                    BaccaratOuterClass.StartOpeningCardsReply showCardsReply
                                            = stub.startOpeningCards(showCardsBuilder.build());

                                    BaccaratOuterClass.GetGameResultReply resultReply = stub.getGameResult(resultBuilder.build());

                                    BaccaratOuterClass.DestroyGameRoomReply destroyReply = stub.destroyGameRoom(destroyBuilder.build());

                                    if (showCardsReply.getResult() == BaccaratOuterClass.StartOpeningCardsReply.StartOpeningCardsResult.SUCCEED
                                            && resultReply.getResult() == BaccaratOuterClass.GetGameResultReply.GetGameRequestResult.SUCCEED
                                            && destroyReply.getIsSucceed()) {
                                        for (BaccaratOuterClass.PlayerResult result : resultReply.getPlayerResultsList()) {
                                            for (BaccaratPlayer baccaratPlayer : players) {
                                                if (result.getUserId() == baccaratPlayer.getUserId()) {
                                                    switch (result.getGameResult()) {
                                                        case LOSE:
                                                            player.setGameResult(GamePlayer.GameResult.LOSE);
                                                            break;
                                                        case WIN:
                                                            player.setGameResult(GamePlayer.GameResult.WIN);
                                                            break;
                                                        case TIE:
                                                            player.setGameResult(GamePlayer.GameResult.TIE);
                                                            break;
                                                        case UNRECOGNIZED:
                                                            break;
                                                    }
                                                    player.setGotPoints(result.getGotPoints());
                                                }
                                            }

                                        }
                                        return true;
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return false;
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            dialog.dismiss();
                            setCommunicating(false);
                            if (result) {
                                gameStatus = RESULT;
                            } else {
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

                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int tmpBet;
                        try {
                            tmpBet = Integer.parseInt(betPointField.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            new AlertDialog.Builder(context)
                                    .setTitle("エラー")
                                    .setMessage("ベット額を正しく入力してください。")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        final int bet = tmpBet * 100;
                        final BaccaratPlayer.BetSide betSide;
                        if (v.getId() == R.id.baccarat_betting_player_button) {
                            betSide = BaccaratPlayer.BetSide.PLAYER;
                        } else if (v.getId() == R.id.baccarat_betting_banker_button) {
                            betSide = BaccaratPlayer.BetSide.BANKER;
                        } else if (v.getId() == R.id.baccarat_betting_tie_button) {
                            betSide = BaccaratPlayer.BetSide.TIE;
                        } else {
                            return;
                        }
                        final ProgressDialog dialog = new ProgressDialog(context);
                        dialog.setTitle("通信中");
                        dialog.setMessage("ベット中です。");
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setCancelable(false);
                        dialog.show();

                        new AsyncTask<Void, Void, BaccaratOuterClass.BettingResult>() {

                            @Override
                            protected BaccaratOuterClass.BettingResult doInBackground(Void... params) {
                                if (ConnectConfig.OFFLINE) {
                                    player.setBetSide(betSide);
                                    player.setBetPoint(bet);
                                    return BaccaratOuterClass.BettingResult.SUCCEED;
                                } else {
                                    try {
                                        String addr = ConnectConfig.getServerAddress(context);
                                        String key = ConnectConfig.getAccessKey(context);
                                        int port = ConnectConfig.getServerPort(context);

                                        ManagedChannel channel = ManagedChannelBuilder
                                                .forAddress(addr, port)
                                                .usePlaintext(true)
                                                .build();
                                        BaccaratGrpc.BaccaratBlockingStub stub = BaccaratGrpc.newBlockingStub(channel);

                                        BaccaratOuterClass.BetRequest.Builder builder = BaccaratOuterClass.BetRequest.newBuilder()
                                                .setAccessToken(key)
                                                .setUserId(player.getUserId())
                                                .setGameRoomId(gameRoomId)
                                                .setBetPoints(bet);


                                        BaccaratOuterClass.BetReply reply
                                                = stub.bet(builder.build());

                                        return reply.getResult();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return BaccaratOuterClass.BettingResult.UNKNOWN_FAILED;
                                    }
                                }
                            }

                            @Override
                            protected void onPostExecute(BaccaratOuterClass.BettingResult result) {
                                dialog.dismiss();
                                switch (result) {
                                    case SUCCEED:
                                        player.setBetSide(betSide);
                                        player.setBetPoint(bet);
                                        break;
                                    case NO_ENOUGH_POINTS:
                                        new AlertDialog.Builder(context)
                                                .setTitle("ベット失敗")
                                                .setMessage("ポイントが足りません。")
                                                .setPositiveButton("OK", null)
                                                .show();
                                        break;
                                    case UNKNOWN_FAILED:
                                    case UNRECOGNIZED:
                                        new AlertDialog.Builder(context)
                                                .setTitle("通信に失敗しました")
                                                .setMessage("管理者に問い合わせてください")
                                                .setPositiveButton("OK", null)
                                                .show();
                                        break;
                                }
                                notifyDataSetChanged();
                            }

                        }.execute();
                    }
                };

                playerBetButton.setOnClickListener(onClickListener);
                bankerBetButton.setOnClickListener(onClickListener);
                tieBetButton.setOnClickListener(onClickListener);


                break;
        }
            case RESULT: {
                if (player.getUserId() == BlackJackPlayer.DEALER_ID) break;
                View innerView = inflater.inflate(R.layout.game_result_layout, null);
                innerLayout.addView(innerView);
                TextView messageTextView = (TextView) innerView.findViewById(R.id.game_result_message_textview);
                TextView pointTextView = (TextView) innerView.findViewById(R.id.game_result_point_textview);
                switch (player.getGameResult()) {
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

                ((ListView) parent).performItemClick(null, 0, 0);

                break;
            }
        }

        return convertView;
    }
}
