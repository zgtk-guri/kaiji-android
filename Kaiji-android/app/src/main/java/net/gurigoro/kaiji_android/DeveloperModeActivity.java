package net.gurigoro.kaiji_android;

import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import net.gurigoro.kaiji.KaijiGrpc;
import net.gurigoro.kaiji.KaijiOuterClass;

import java.sql.Time;
import java.util.Date;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

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
        setTitle("Developer mode");

        findViewById(R.id.dev_mode_qr_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeveloperModeActivity.this, ScanQrActivity.class);
                startActivityForResult(intent, ScanQrActivity.TAG);
            }
        });

        findViewById(R.id.dev_mode_grpc_ping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new pingTask().execute();
            }
        });
    }

    private class pingTask extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            try {
                String address = ConnectConfig.getServerAddress(DeveloperModeActivity.this);
                int port = ConnectConfig.getServerPort(DeveloperModeActivity.this);
                ManagedChannel channel =
                        ManagedChannelBuilder.
                                forAddress(address, port).
                                usePlaintext(true).
                                build();
                KaijiGrpc.KaijiBlockingStub stub = KaijiGrpc.newBlockingStub(channel);

                KaijiOuterClass.PingRequest pingRequest = KaijiOuterClass.PingRequest.newBuilder()
                        .setMessage("Hello from kaiji-android developer mode")
                        .setTime(System.currentTimeMillis())
                        .build();
                KaijiOuterClass.PingReply pingReply = stub.ping(pingRequest);
                long period = System.currentTimeMillis() - pingReply.getTime();
                return pingReply.getMessage() + period;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }


        @Override
        protected void onPostExecute(final String s) {
            DeveloperModeActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DeveloperModeActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
