package net.gurigoro.kaiji_android;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class PokerFragment extends Fragment {


    public static final String TAG = "poker_fragment";

    public PokerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("ポーカー");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_poker, container, false);
    }

}
