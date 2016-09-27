package net.gurigoro.kaiji_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class DeveloperModeActivity extends AppCompatActivity {

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ScanQrActivity.TAG){
            if(resultCode == RESULT_OK){
                Toast.makeText(
                        this,
                        data.getCharSequenceExtra(ScanQrActivity.QR_VALUE_KEY),
                        Toast.LENGTH_SHORT)
                        .show();
            }else{
                Toast.makeText(this, "canceled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_mode);

        findViewById(R.id.dev_mode_qr_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeveloperModeActivity.this, ScanQrActivity.class);
                startActivityForResult(intent, ScanQrActivity.TAG);
            }
        });
    }
}
