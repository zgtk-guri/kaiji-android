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
import android.widget.Button;
import android.widget.ListView;

import net.gurigoro.kaiji.blackjack.BlackJackGrpc;
import net.gurigoro.kaiji.blackjack.BlackJackOuterClass;
import net.gurigoro.kaiji.KaijiGrpc;
import net.gurigoro.kaiji.KaijiOuterClass;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


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

        return view;
    }

}
