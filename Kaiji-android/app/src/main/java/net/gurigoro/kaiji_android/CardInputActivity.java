package net.gurigoro.kaiji_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.Serializable;

public class CardInputActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, NumberPicker.OnValueChangeListener{

    public static final String CARD_KEY = "card";
    public static final String DATA_BUNDLE_KEY = "data_bundle";

    ImageView cardImageVIew;
    NumberPicker numberPicker;
    RadioGroup radioGroup;
    Button okButton, cancelButton;

    Bundle bundle;

    TrumpCard card;

    private void applyImage(){
        cardImageVIew.setImageDrawable(card.getDrawable(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_input);
        setTitle("カード入力");

        cardImageVIew = (ImageView) findViewById(R.id.card_select_inageview);
        numberPicker = (NumberPicker) findViewById(R.id.card_select_number_picker);
        radioGroup = (RadioGroup) findViewById(R.id.card_select_radio_group);
        okButton = (Button) findViewById(R.id.card_select_ok_button);
        cancelButton = (Button) findViewById(R.id.card_select_cancel_button);

        Intent requestIntent = getIntent();
        if(requestIntent.hasExtra(DATA_BUNDLE_KEY)){
            bundle = requestIntent.getBundleExtra(DATA_BUNDLE_KEY);
        }else{
            bundle = null;
        }

        if(requestIntent.hasExtra(CARD_KEY)){
            card = (TrumpCard) requestIntent.getSerializableExtra(CARD_KEY);
            if(!card.isFaceDown()){
                numberPicker.setValue(card.getNumber());
                switch (card.getSuit()){
                    case SPADE:
                        ((RadioButton)findViewById(R.id.card_select_spade_button)).setChecked(true);
                        break;
                    case CLUB:
                        ((RadioButton)findViewById(R.id.card_select_club_button)).setChecked(true);
                        break;
                    case HEART:
                        ((RadioButton)findViewById(R.id.card_select_heart_button)).setChecked(true);
                        break;
                    case DIAMOND:
                        ((RadioButton)findViewById(R.id.card_select_diamond_button)).setChecked(true);
                        break;
                }
                applyImage();
            }else{
                card.setNumber(1);
                card.setSuit(TrumpCard.TrumpSuit.SPADE);
                numberPicker.setValue(1);
                ((RadioButton)findViewById(R.id.card_select_spade_button)).setChecked(true);
                applyImage();
            }
        }else{
            card = new TrumpCard();
            card.setNumber(1);
            card.setSuit(TrumpCard.TrumpSuit.SPADE);
            numberPicker.setValue(1);
            ((RadioButton)findViewById(R.id.card_select_spade_button)).setChecked(true);
            applyImage();
        }

        card.setFaceDown(false);

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(13);

        radioGroup.setOnCheckedChangeListener(this);
        numberPicker.setOnValueChangedListener(this);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(CARD_KEY, (Serializable) card);
                if(bundle != null) intent.putExtra(DATA_BUNDLE_KEY, bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId){
            case R.id.card_select_spade_button:
                card.setSuit(TrumpCard.TrumpSuit.SPADE);
                break;
            case R.id.card_select_club_button:
                card.setSuit(TrumpCard.TrumpSuit.CLUB);
                break;
            case R.id.card_select_heart_button:
                card.setSuit(TrumpCard.TrumpSuit.HEART);
                break;
            case R.id.card_select_diamond_button:
                card.setSuit(TrumpCard.TrumpSuit.DIAMOND);
                break;
        }
        applyImage();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        card.setNumber(newVal);
        applyImage();
    }
}
