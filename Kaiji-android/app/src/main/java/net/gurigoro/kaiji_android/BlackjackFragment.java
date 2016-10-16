package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import net.gurigoro.kaiji.Trump;
import net.gurigoro.kaiji.blackjack.BlackJackGrpc;
import net.gurigoro.kaiji.blackjack.BlackJackOuterClass;
import net.gurigoro.kaiji.KaijiGrpc;
import net.gurigoro.kaiji.KaijiOuterClass;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class BlackjackFragment extends Fragment {

    public static final String TAG = "blackjack_fragment";

    private Button startButton, entryButton;
    private ListView mainListView;

    private List<BlackJackPlayer> players;
    private BlackJackSectionAdapter adapter;

    private static final int SCAN_QR_REQ_CODE = 447;
    private static final int CARD_INPUT_FIRST_DEAL_REQ_CODE = 759;
    private static final int CARD_INPUT_HIT_REQ_CODE = 179;
    private static final int CARD_INPUT_DOUBLE_DOWN_REQ_CODE = 826;

    @Override
    public void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if(requestCode == SCAN_QR_REQ_CODE){
            if(resultCode == RESULT_OK){
                String idStr = data.getStringExtra(ScanQrActivity.QR_VALUE_KEY);
                final int id = Integer.parseInt(idStr);

                for (BlackJackPlayer player : players) {
                    if(player.getUserId() == id){
                        new AlertDialog.Builder(getContext())
                                .setTitle("エラー")
                                .setMessage("同じ人が複数参加することはできません。")
                                .setPositiveButton("OK", null)
                                .show();
                        return;
                    }
                }

                final ProgressDialog dialog = new ProgressDialog(getContext());
                dialog.setTitle("通信中");
                dialog.setMessage("ユーザ情報取得中。");
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);
                dialog.show();

                new AsyncTask<Void, Void, KaijiOuterClass.GetUserReply>(){

                    @Override
                    protected KaijiOuterClass.GetUserReply doInBackground(Void... params) {
                        if(ConnectConfig.OFFLINE) {
                            return null;
                        }else{
                            try {
                                String addr = ConnectConfig.getServerAddress(getContext());
                                String key = ConnectConfig.getAccessKey(getContext());
                                int port = ConnectConfig.getServerPort(getContext());

                                ManagedChannel channel = ManagedChannelBuilder
                                        .forAddress(addr, port)
                                        .usePlaintext(true)
                                        .build();
                                KaijiGrpc.KaijiBlockingStub stub = KaijiGrpc.newBlockingStub(channel);

                                KaijiOuterClass.GetUserByIdRequest.Builder builder = KaijiOuterClass.GetUserByIdRequest.newBuilder()
                                        .setAccessToken(key)
                                        .setUserId(id);

                                KaijiOuterClass.GetUserReply reply = stub.getUserById(builder.build());
                                return reply;

                            }catch (Exception e){
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }

                    @Override
                    protected void onPostExecute(KaijiOuterClass.GetUserReply reply) {
                        dialog.dismiss();
                        if(ConnectConfig.OFFLINE) {
                            BlackJackPlayer player = new BlackJackPlayer();
                            player.setUserId(id);
                            player.setUserName(String.valueOf(id));
                            player.setUserPoint(10000);
                            players.add(player);
                            adapter.notifyDataSetChanged();
                        }else if(reply != null){
                            if(!reply.getIsFound()){
                                new AlertDialog.Builder(getContext())
                                        .setTitle("エラー")
                                        .setMessage("該当するユーザーが見つかりません")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                            BlackJackPlayer player = new BlackJackPlayer();
                            player.setUserId(id);
                            player.setUserName(reply.getName());
                            player.setUserPoint(reply.getPoint());
                            players.add(player);
                            adapter.notifyDataSetChanged();
                        }else{
                            new AlertDialog.Builder(getContext())
                                    .setTitle("通信に失敗しました")
                                    .setMessage("ユーザー情報取得に失敗しました。再試行するか、管理者に問い合わせてください")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }

                }.execute();

            }
        }else if(requestCode == CARD_INPUT_FIRST_DEAL_REQ_CODE){
            if(resultCode == RESULT_OK) {
                TrumpCard card = (TrumpCard) data.getSerializableExtra(CardInputActivity.CARD_KEY);
                Bundle bundle = data.getBundleExtra(CardInputActivity.DATA_BUNDLE_KEY);
                int position = bundle.getInt("position");
                players.get(position).getCards()[0].add(card);
                adapter.notifyDataSetChanged();
            }
        }else if(requestCode == CARD_INPUT_HIT_REQ_CODE){
            if(resultCode == RESULT_OK){
                final TrumpCard card = (TrumpCard) data.getSerializableExtra(CardInputActivity.CARD_KEY);
                Bundle bundle = data.getBundleExtra(CardInputActivity.DATA_BUNDLE_KEY);
                final int position = bundle.getInt("position");
                final long gameRoomId = adapter.getGameRoomId();

                if(players.get(position).getUserId() != BlackJackPlayer.DEALER_ID) {
                    final int handsIndex = bundle.getInt("handsindex");

                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setTitle("通信中");
                    dialog.setMessage("ヒットしています");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();

                    new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if (ConnectConfig.OFFLINE) {
                                BlackJackPlayer player = players.get(position);
                                player.getCards()[handsIndex].add(card);
                                player.getCardPoint()[handsIndex] += card.getNumber();
                                if (player.getCardPoint()[handsIndex] > 21) {
                                    if (handsIndex == 0) {
                                        player.setBust(true);
                                        player.setCanHit(false);
                                        player.setCanStand(false);
                                    } else {
                                        player.setBustSecondHands(true);
                                        player.setCanHitSecondHands(false);
                                        player.setCanStandSecondHands(false);
                                    }
                                } else {
                                    if (handsIndex == 0) {
                                        player.setCanHit(true);
                                        player.setCanStand(true);
                                    } else {
                                        player.setCanHitSecondHands(true);
                                        player.setCanStandSecondHands(true);
                                    }
                                }
                                player.setCanDoubleDown(false);
                                player.setCanSplit(false);
                                return true;
                            } else {
                                try {
                                    String addr = ConnectConfig.getServerAddress(getContext());
                                    String key = ConnectConfig.getAccessKey(getContext());
                                    int port = ConnectConfig.getServerPort(getContext());

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                    BlackJackOuterClass.HitRequest.Builder builder = BlackJackOuterClass.HitRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId)
                                            .setUserId(players.get(position).getUserId())
                                            .setHandsIndex(handsIndex)
                                            .setCard(card.getGrpcTrumpCard());

                                    BlackJackOuterClass.HitReply reply = stub.hit(builder.build());

                                    if (!reply.getIsSucceed()) return false;

                                    BlackJackPlayer player = players.get(position);
                                    player.getCardPoint()[handsIndex] = reply.getCardPoints();
                                    player.getCards()[handsIndex].add(card);
                                    player.setCanDoubleDown(false);
                                    player.setCanSplit(false);
                                    if (handsIndex == 0) {
                                        player.setBust(reply.getIsBusted());
                                        player.setCanHit(false);
                                        player.setCanStand(false);
                                        for (BlackJackOuterClass.PlayerAction action : reply.getAllowedActionsList()) {
                                            switch (action) {
                                                case UNKNOWN:
                                                    break;
                                                case HIT:
                                                    player.setCanHit(true);
                                                    break;
                                                case STAND:
                                                    player.setCanStand(true);
                                                    break;
                                                case SPLIT:
                                                    break;
                                                case DOUBLEDOWN:
                                                    break;
                                                case UNRECOGNIZED:
                                                    break;
                                            }
                                        }

                                    } else {
                                        player.setBustSecondHands(reply.getIsBusted());
                                        player.setCanHitSecondHands(false);
                                        player.setCanStandSecondHands(false);
                                        for (BlackJackOuterClass.PlayerAction action : reply.getAllowedActionsList()) {
                                            switch (action) {
                                                case UNKNOWN:
                                                    break;
                                                case HIT:
                                                    player.setCanHitSecondHands(true);
                                                    break;
                                                case STAND:
                                                    player.setCanStandSecondHands(true);
                                                    break;
                                                case SPLIT:
                                                    break;
                                                case DOUBLEDOWN:
                                                    break;
                                                case UNRECOGNIZED:
                                                    break;
                                            }
                                        }
                                    }

                                    return true;


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            dialog.dismiss();
                            if (result) {
                                adapter.notifyDataSetChanged();
                            } else {
                                new AlertDialog.Builder(getContext())
                                        .setTitle("通信に失敗しました")
                                        .setMessage("ヒットに失敗しました。再試行するか、管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }

                    }.execute();

                }else{
                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setTitle("通信中");
                    dialog.setMessage("ヒットしています");
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setCancelable(false);
                    dialog.show();

                    new AsyncTask<Void, Void, Boolean>() {

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            if (ConnectConfig.OFFLINE) {
                                BlackJackPlayer dealer = players.get(position);
                                dealer.getCards()[0].add(card);
                                if(dealer.getCardPoint()[0] == 0){
                                    dealer.getCardPoint()[0] = dealer.getCards()[0].get(0).getNumber();
                                }
                                dealer.getCardPoint()[0] += card.getNumber();
                                if (dealer.getCardPoint()[0] > 21) {
                                    dealer.setBust(true);
                                    dealer.setCanHit(false);
                                    dealer.setCanStand(false);
                                } else if(dealer.getCardPoint()[0] > 16){
                                    dealer.setCanHit(false);
                                    dealer.setCanStand(false);
                                    dealer.setBust(false);
                                }else{
                                    dealer.setCanHit(true);
                                    dealer.setCanStand(false);
                                    dealer.setBust(false);
                                }
                                return true;
                            } else {
                                try {
                                    String addr = ConnectConfig.getServerAddress(getContext());
                                    String key = ConnectConfig.getAccessKey(getContext());
                                    int port = ConnectConfig.getServerPort(getContext());

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                    BlackJackOuterClass.SetNextDealersCardRequest.Builder builder = BlackJackOuterClass.SetNextDealersCardRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId)
                                            .setCard(card.getGrpcTrumpCard());

                                    BlackJackOuterClass.SetNextDealersCardReply reply = stub.setNextDealersCard(builder.build());

                                    if (!reply.getIsSucceed()) return false;

                                    BlackJackPlayer dealer = players.get(position);
                                    dealer.getCardPoint()[0] = reply.getCardPoints();
                                    dealer.getCards()[0].add(card);
                                    dealer.setCanDoubleDown(false);
                                    dealer.setCanSplit(false);
                                    if(reply.getIsBusted()){
                                        dealer.setBust(true);
                                        dealer.setCanHit(false);
                                        dealer.setCanStand(false);
                                    }else if(reply.getShouldHit()){
                                        dealer.setBust(false);
                                        dealer.setCanHit(true);
                                        dealer.setCanStand(false);
                                    }else{
                                        dealer.setBust(false);
                                        dealer.setCanHit(false);
                                        dealer.setCanStand(false);
                                    }

                                    return true;


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            dialog.dismiss();
                            if (result) {
                                if(!players.get(position).isCanHit()){

                                    final ProgressDialog dialog = new ProgressDialog(getContext());
                                    dialog.setTitle("通信中");
                                    dialog.setMessage("結果を取得しています");
                                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                    dialog.setCancelable(false);
                                    dialog.show();

                                    new AsyncTask<Void, Void, Boolean>() {

                                        @Override
                                        protected Boolean doInBackground(Void... params) {
                                            if (ConnectConfig.OFFLINE) {
                                                long dealerPoint = players.get(0).getCardPoint()[0];
                                                boolean dealerIsBust = players.get(0).isBust();
                                                for (BlackJackPlayer player : players) {
                                                    if(player.getUserId() == BlackJackPlayer.DEALER_ID) continue;
                                                    if(player.isBust()){
                                                        player.setGotPoints(-player.getBetPoint());
                                                    }else if(dealerIsBust){
                                                        player.setGotPoints(player.getBetPoint());
                                                    }else if(dealerPoint > player.getCardPoint()[0]){
                                                        player.setGotPoints(-player.getBetPoint());
                                                    }else if(dealerPoint < player.getCardPoint()[0]){
                                                        player.setGotPoints(player.getBetPoint());
                                                    }else{
                                                        player.setGotPoints(0);
                                                    }
                                                    if(player.isSplit()){
                                                        if(player.isBustSecondHands()){
                                                            player.setGotPoints(player.getGotPoints() - player.getBetPoint());
                                                        }else if(dealerIsBust){
                                                            player.setGotPoints(player.getGotPoints() + player.getBetPoint());
                                                        }else if(dealerPoint > player.getCardPoint()[1]){
                                                            player.setGotPoints(player.getGotPoints() - player.getBetPoint());
                                                        }else if(dealerPoint < player.getCardPoint()[1]){
                                                            player.setGotPoints(player.getGotPoints() + player.getBetPoint());
                                                        }else{

                                                        }
                                                    }
                                                    if(player.getGotPoints() == 0){
                                                        player.setGameResult(BlackJackPlayer.GameResult.TIE);
                                                    }else if(player.getGotPoints() > 0){
                                                        player.setGameResult(BlackJackPlayer.GameResult.WIN);
                                                    }else{
                                                        player.setGameResult(BlackJackPlayer.GameResult.LOSE);
                                                    }
                                                }
                                                return true;
                                            } else {
                                                try {
                                                    String addr = ConnectConfig.getServerAddress(getContext());
                                                    String key = ConnectConfig.getAccessKey(getContext());
                                                    int port = ConnectConfig.getServerPort(getContext());

                                                    ManagedChannel channel = ManagedChannelBuilder
                                                            .forAddress(addr, port)
                                                            .usePlaintext(true)
                                                            .build();
                                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                                    BlackJackOuterClass.GetGameResultRequest.Builder builder = BlackJackOuterClass.GetGameResultRequest.newBuilder()
                                                            .setAccessToken(key)
                                                            .setGameRoomId(gameRoomId);

                                                    BlackJackOuterClass.GetGameResultReply reply = stub.getGameResult(builder.build());

                                                    if (!reply.getIsSucceed()) return false;

                                                    for (BlackJackOuterClass.PlayerResult playerResult : reply.getPlayerResultsList()) {
                                                        for (BlackJackPlayer player : players) {
                                                            if(player.getUserId() != playerResult.getUserId()) continue;

                                                            switch (playerResult.getGameResult()){
                                                                case LOSE:
                                                                    player.setGameResult(BlackJackPlayer.GameResult.LOSE);
                                                                    break;
                                                                case TIE:
                                                                    player.setGameResult(BlackJackPlayer.GameResult.TIE);
                                                                    break;
                                                                case WIN:
                                                                    player.setGameResult(BlackJackPlayer.GameResult.WIN);
                                                                    break;
                                                                case UNRECOGNIZED:
                                                                    break;
                                                            }
                                                            player.setGotPoints(playerResult.getGotPoints());
                                                            break;
                                                        }
                                                    }

                                                    return true;


                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    return false;
                                                }
                                            }
                                        }

                                        @Override
                                        protected void onPostExecute(Boolean result) {
                                            dialog.dismiss();
                                            if (result) {
                                                adapter.setGameStatus(BlackJackSectionAdapter.BlackJackGameStatus.RESULT);
                                                adapter.notifyDataSetChanged();
                                            } else {
                                                new AlertDialog.Builder(getContext())
                                                        .setTitle("通信に失敗しました")
                                                        .setMessage("結果の取得に失敗しました。再試行するか、管理者に問い合わせてください")
                                                        .setPositiveButton("OK", null)
                                                        .show();
                                            }
                                        }

                                    }.execute();


                                }else{
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                new AlertDialog.Builder(getContext())
                                        .setTitle("通信に失敗しました")
                                        .setMessage("ヒットに失敗しました。再試行するか、管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }

                    }.execute();

                }
            }
        }else if(requestCode == CARD_INPUT_DOUBLE_DOWN_REQ_CODE){
            if(resultCode == RESULT_OK){
                final TrumpCard card = (TrumpCard) data.getSerializableExtra(CardInputActivity.CARD_KEY);
                Bundle bundle = data.getBundleExtra(CardInputActivity.DATA_BUNDLE_KEY);
                final int position = bundle.getInt("position");
                final long gameRoomId = adapter.getGameRoomId();

                final ProgressDialog dialog = new ProgressDialog(getContext());
                dialog.setTitle("通信中");
                dialog.setMessage("ダブルダウンしています");
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);
                dialog.show();

                new AsyncTask<Void, Void, Boolean>(){

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        if(ConnectConfig.OFFLINE) {
                            BlackJackPlayer player = players.get(position);
                            player.getCards()[0].add(card);
                            player.getCardPoint()[0] += card.getNumber();
                            if(player.getCardPoint()[0] > 21){
                                player.setBust(true);
                            }
                            player.setCanHit(false);
                            player.setCanStand(false);
                            player.setCanDoubleDown(false);
                            player.setCanSplit(false);
                            return true;
                        }else{
                            try {
                                String addr = ConnectConfig.getServerAddress(getContext());
                                String key = ConnectConfig.getAccessKey(getContext());
                                int port = ConnectConfig.getServerPort(getContext());

                                ManagedChannel channel = ManagedChannelBuilder
                                        .forAddress(addr, port)
                                        .usePlaintext(true)
                                        .build();
                                BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                BlackJackOuterClass.DoubleDownRequest.Builder builder = BlackJackOuterClass.DoubleDownRequest.newBuilder()
                                        .setAccessToken(key)
                                        .setGameRoomId(gameRoomId)
                                        .setUserId(players.get(position).getUserId())
                                        .setCard(card.getGrpcTrumpCard());

                                BlackJackOuterClass.DoubleDownReply reply = stub.doubleDown(builder.build());

                                if(!reply.getIsSucceed()) return false;

                                BlackJackPlayer player = players.get(position);
                                player.getCardPoint()[0] = reply.getCardPoints();
                                player.getCards()[0].add(card);
                                player.setCanHit(false);
                                player.setCanStand(false);
                                player.setCanDoubleDown(false);
                                player.setCanSplit(false);


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
                            adapter.notifyDataSetChanged();
                        }else{
                            new AlertDialog.Builder(getContext())
                                    .setTitle("通信に失敗しました")
                                    .setMessage("ダブルダウンに失敗しました。再試行するか、管理者に問い合わせてください")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }

                }.execute();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("ブラックジャック");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blackjack, container, false);
        entryButton = (Button) view.findViewById(R.id.blackjack_entry_button);
        startButton = (Button) view.findViewById(R.id.blackjack_start_button);
        mainListView = (ListView) view.findViewById(R.id.blackjack_main_listview);

        adapter = new BlackJackSectionAdapter(getContext());
        players = new ArrayList<>();
        BlackJackPlayer dealer = new BlackJackPlayer();
        dealer.setUserId(BlackJackPlayer.DEALER_ID);
        dealer.setUserName("ディーラー");
        players.add(dealer);
        adapter.setPlayers(players);
        mainListView.setAdapter(adapter);

        entryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(),ScanQrActivity.class);
                startActivityForResult(intent, SCAN_QR_REQ_CODE);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(players.size() <= 1){
                    new AlertDialog.Builder(getContext())
                            .setTitle("参加者がいません")
                            .setMessage("エントリーしてください。")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                final ProgressDialog dialog = new ProgressDialog(getContext());
                dialog.setTitle("通信中");
                dialog.setMessage("エントリー中です。");
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
                                String addr = ConnectConfig.getServerAddress(getContext());
                                String key = ConnectConfig.getAccessKey(getContext());
                                int port = ConnectConfig.getServerPort(getContext());

                                ManagedChannel channel = ManagedChannelBuilder
                                        .forAddress(addr, port)
                                        .usePlaintext(true)
                                        .build();
                                BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                BlackJackOuterClass.CreateNewGameRoomRequest.Builder builder = BlackJackOuterClass.CreateNewGameRoomRequest.newBuilder()
                                        .setAccessToken(key);

                                for (BlackJackPlayer player : players) {
                                    if(player.getUserId() != BlackJackPlayer.DEALER_ID){
                                        builder.addUsersId(player.getUserId());
                                    }
                                }
                                BlackJackOuterClass.CreateNewGameRoomReply reply
                                        = stub.createNewGameRoom(builder.build());

                                adapter.setGameRoomId(reply.getGameRoomId());
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
                        if(result){
                            entryButton.setVisibility(View.GONE);
                            startButton.setVisibility(View.GONE);

                            adapter.setGameStatus(BlackJackSectionAdapter.BlackJackGameStatus.BETTING);
                            adapter.notifyDataSetChanged();
                        }else{
                            new AlertDialog.Builder(getContext())
                                    .setTitle("通信に失敗しました")
                                    .setMessage("エントリーに失敗しました。再試行するか、管理者に問い合わせてください")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }

                }.execute();
            }
        });

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if(view.getId() == R.id.bj_first_deal_card_1 || view.getId() == R.id.bj_first_deal_card_2){
                    // Set First Dealed Card
                    Bundle bundle = new Bundle();
                    bundle.putInt("position", position);
                    Intent intent = new Intent(getContext(), CardInputActivity.class);
                    intent.putExtra(CardInputActivity.DATA_BUNDLE_KEY, bundle);
                    startActivityForResult(intent, CARD_INPUT_FIRST_DEAL_REQ_CODE);

                }else if(view.getId() == R.id.bj_action_hit_button){
                    Bundle bundle  = new Bundle();
                    bundle.putInt("position", position);
                    bundle.putInt("handsindex", (int) id);
                    Intent intent = new Intent(getContext(), CardInputActivity.class);
                    intent.putExtra(CardInputActivity.DATA_BUNDLE_KEY, bundle);
                    startActivityForResult(intent, CARD_INPUT_HIT_REQ_CODE);

                }else if(view.getId() == R.id.bj_action_stand_button){
                    final long gameRoomId = adapter.getGameRoomId();
                    final int handsIndex = (int) id;

                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setTitle("通信中");
                    dialog.setMessage("スタンドしています");
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
                                    String addr = ConnectConfig.getServerAddress(getContext());
                                    String key = ConnectConfig.getAccessKey(getContext());
                                    int port = ConnectConfig.getServerPort(getContext());

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                    BlackJackOuterClass.StandRequest.Builder builder = BlackJackOuterClass.StandRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId)
                                            .setUserId(players.get(position).getUserId())
                                            .setHandsIndex(handsIndex);

                                    BlackJackOuterClass.StandReply reply = stub.stand(builder.build());

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
                            if(result){
                                BlackJackPlayer player = players.get(position);
                                player.setCanSplit(false);
                                player.setCanDoubleDown(false);
                                if(handsIndex == 0){
                                    player.setCanHit(false);
                                    player.setCanStand(false);
                                }else{
                                    player.setCanHitSecondHands(false);
                                    player.setCanStandSecondHands(false);
                                }
                                adapter.notifyDataSetChanged();
                            }else{
                                new AlertDialog.Builder(getContext())
                                        .setTitle("通信に失敗しました")
                                        .setMessage("スタンドに失敗しました。再試行するか、管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }

                    }.execute();
                }else if(view.getId() == R.id.bj_action_split_button){
                    final long gameRoomId = adapter.getGameRoomId();

                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setTitle("通信中");
                    dialog.setMessage("スプリットしています");
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
                                    String addr = ConnectConfig.getServerAddress(getContext());
                                    String key = ConnectConfig.getAccessKey(getContext());
                                    int port = ConnectConfig.getServerPort(getContext());

                                    ManagedChannel channel = ManagedChannelBuilder
                                            .forAddress(addr, port)
                                            .usePlaintext(true)
                                            .build();
                                    BlackJackGrpc.BlackJackBlockingStub stub = BlackJackGrpc.newBlockingStub(channel);

                                    BlackJackOuterClass.SplitRequest.Builder builder = BlackJackOuterClass.SplitRequest.newBuilder()
                                            .setAccessToken(key)
                                            .setGameRoomId(gameRoomId)
                                            .setUserId(players.get(position).getUserId());

                                    BlackJackOuterClass.SplitReply reply = stub.split(builder.build());

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
                            if(result){
                                BlackJackPlayer player = players.get(position);
                                player.setSplit(true);
                                player.setCanSplit(false);
                                player.setCanDoubleDown(false);
                                player.setCanHit(true);
                                player.setCanStand(true);
                                player.setCanHitSecondHands(true);
                                player.setCanStandSecondHands(true);

                                TrumpCard trumpCard = player.getCards()[0].get(1);
                                player.getCards()[0].remove(1);
                                player.getCards()[1].add(trumpCard);

                                player.getCardPoint()[1] = player.getCardPoint()[0] /= 2;


                                adapter.notifyDataSetChanged();
                            }else{
                                new AlertDialog.Builder(getContext())
                                        .setTitle("通信に失敗しました")
                                        .setMessage("スプリットに失敗しました。再試行するか、管理者に問い合わせてください")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }

                    }.execute();
                }else if(view.getId() == R.id.bj_action_double_down_button){
                    Bundle bundle  = new Bundle();
                    bundle.putInt("position", position);
                    Intent intent = new Intent(getContext(), CardInputActivity.class);
                    intent.putExtra(CardInputActivity.DATA_BUNDLE_KEY, bundle);
                    startActivityForResult(intent, CARD_INPUT_DOUBLE_DOWN_REQ_CODE);
                }
            }
        });

        return view;
    }

}
