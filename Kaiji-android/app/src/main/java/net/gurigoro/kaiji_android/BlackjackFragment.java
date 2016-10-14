package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class BlackjackFragment extends Fragment {

    public static final String TAG = "blackjack_fragment";

    private Button startButton, entryButton;
    private ListView mainListView;

    private List<BlackJackPlayer> players;
    private BlackJackSectionAdapter adapter;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ScanQrActivity.TAG){
            if(resultCode == Activity.RESULT_OK){
                String idStr = data.getStringExtra(ScanQrActivity.QR_VALUE_KEY);
                int id = Integer.parseInt(idStr);

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

                BlackJackPlayer player = new BlackJackPlayer();
                player.setUserId(id);
                player.setUserName(idStr);
                players.add(player);
                adapter.notifyDataSetChanged();
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
                startActivityForResult(intent, ScanQrActivity.TAG);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                entryButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                adapter.setGameStatus(BlackJackSectionAdapter.BlackJackGameStatus.BETTING);
                adapter.notifyDataSetChanged();
            }
        });

        return view;
    }

}
