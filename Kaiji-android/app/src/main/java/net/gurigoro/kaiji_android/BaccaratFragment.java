package net.gurigoro.kaiji_android;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import net.gurigoro.kaiji.KaijiGrpc;
import net.gurigoro.kaiji.KaijiOuterClass;
import net.gurigoro.kaiji.baccarat.BaccaratGrpc;
import net.gurigoro.kaiji.baccarat.BaccaratOuterClass;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


/**
 * A simple {@link Fragment} subclass.
 */
public class BaccaratFragment extends Fragment {
    public static final String TAG = "baccarat_fragment";

    private static final int SCAN_QR_REQ_CODE = 909;

    private Button entryButton, startButton, endButton;
    private ListView mainListView;

    private BaccaratSectionAdapter adapter;
    private List<BaccaratPlayer> players;

    public BaccaratFragment() {
        // Required empty public constructor
    }

    private void initializePlayers(){
        players = new ArrayList<>();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCAN_QR_REQ_CODE){
            if(resultCode == Activity.RESULT_OK){
                String idStr = data.getStringExtra(ScanQrActivity.QR_VALUE_KEY);
                final int id = Integer.parseInt(idStr);

                for (BaccaratPlayer player : players) {
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
                            BaccaratPlayer player = new BaccaratPlayer();
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
                            BaccaratPlayer player = new BaccaratPlayer();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("バカラ");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_baccarat, container, false);
        entryButton = (Button) view.findViewById(R.id.baccarat_entry_button);
        startButton = (Button) view.findViewById(R.id.baccarat_start_button);
        endButton = (Button) view.findViewById(R.id.baccarat_end_button);
        mainListView = (ListView) view.findViewById(R.id.baccarat_main_listview);

        adapter = new BaccaratSectionAdapter(getContext());
        initializePlayers();
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
                if(players.size() < 1){
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
                                BaccaratGrpc.BaccaratBlockingStub stub = BaccaratGrpc.newBlockingStub(channel);

                                BaccaratOuterClass.CreateNewGameRoomRequest.Builder builder = BaccaratOuterClass.CreateNewGameRoomRequest.newBuilder()
                                        .setAccessToken(key);

                                for (BaccaratPlayer player : players) {
                                    builder.addUsersId(player.getUserId());

                                }
                                BaccaratOuterClass.CreateNewGameRoomReply reply
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


                            adapter.setGameStatus(BaccaratSectionAdapter.GameStatus.BETTING);
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

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializePlayers();
                mainListView.setAdapter(null);
                adapter = new BaccaratSectionAdapter(getContext());
                adapter.setPlayers(players);
                mainListView.setAdapter(adapter);
                entryButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.VISIBLE);
                endButton.setVisibility(View.GONE);
            }
        });

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view == null){
                    endButton.setVisibility(View.VISIBLE);
                }
            }
        });


        return view;
    }

}
