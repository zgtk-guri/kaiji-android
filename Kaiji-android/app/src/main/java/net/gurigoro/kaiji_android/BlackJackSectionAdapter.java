package net.gurigoro.kaiji_android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

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

    public BlackJackGameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(BlackJackGameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    BlackJackGameStatus gameStatus = BlackJackGameStatus.ENTRY;


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
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.player_section,parent,false);
        TextView playerNameTextView = (TextView) convertView.findViewById(R.id.player_name_textview);
        TextView playerSubValueTextView = (TextView) convertView.findViewById(R.id.player_sub_value_textview);

        LinearLayout innerLayout = (LinearLayout) convertView.findViewById(R.id.player_inner_layout);

        playerNameTextView.setText(players.get(position).getUserName());
        if(players.get(position).getUserId() == BlackJackPlayer.DEALER_ID){
            playerSubValueTextView.setText("");
        }else{
            playerSubValueTextView.setText(players.get(position).getUserPoint() + "Pt.");
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
                if(players.get(position).getUserId() != BlackJackPlayer.DEALER_ID){
                    innerLayout.addView(unEntryButton);
                }
                break;
            }
            case BETTING: {
                break;
            }
            case FIRST_DEAL: {
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
