package net.gurigoro.kaiji_android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import net.gurigoro.kaiji.KaijiGrpc;
import net.gurigoro.kaiji.KaijiOuterClass;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static android.app.Activity.RESULT_OK;


public class AddUserFragment extends Fragment {
   public static final String TAG = "add_user_fragment";

    private EditText idEditText;
    private Button scanButton;
    private EditText nameEditText;
    private CheckBox anonymousCheckBox;
    private Button addButton;

    private static final int SCAN_QR_REQ_CODE = 768;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SCAN_QR_REQ_CODE){
            if(resultCode == RESULT_OK){
                String idstr = data.getStringExtra(ScanQrActivity.QR_VALUE_KEY);
                idEditText.setText(idstr);
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
        getActivity().setTitle("ユーザー新規登録");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_user, container, false);

        idEditText = (EditText) view.findViewById(R.id.add_user_id_edittext);
        nameEditText = (EditText) view.findViewById(R.id.add_user_name_edittext);
        scanButton = (Button) view.findViewById(R.id.add_user_scan_button);
        anonymousCheckBox = (CheckBox) view.findViewById(R.id.add_user_anonymous_checkbox);
        addButton = (Button) view.findViewById(R.id.add_user_add_button);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ScanQrActivity.class);
                startActivityForResult(intent, SCAN_QR_REQ_CODE);
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long tmpId;
                try {
                  tmpId = Long.parseLong(idEditText.getText().toString());
                } catch (NumberFormatException e) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("ID")
                            .setMessage("IDが間違っています。")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                final long id = tmpId;
                final String name = nameEditText.getText().toString();
                if (name.equals("")) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("名前")
                            .setMessage("名前が空になっています。")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                final boolean isAnonymous = anonymousCheckBox.isChecked();

                final ProgressDialog dialog = new ProgressDialog(getContext());
                dialog.setTitle("通信中");
                dialog.setMessage("ユーザ追加中。");
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
                                KaijiGrpc.KaijiBlockingStub stub = KaijiGrpc.newBlockingStub(channel);

                                KaijiOuterClass.AddUserRequest.Builder builder = KaijiOuterClass.AddUserRequest.newBuilder()
                                        .setAccessToken(key)
                                        .setUserId(id)
                                        .setName(name)
                                        .setIsAnonymous(isAnonymous)
                                        .setIsAvailable(true);

                                KaijiOuterClass.AddUserReply reply = stub.addUser(builder.build());

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
                            Toast.makeText(getContext(), "ユーザー登録成功しました！", Toast.LENGTH_SHORT).show();
                            idEditText.setText("");
                            nameEditText.setText("");
                            anonymousCheckBox.setChecked(false);
                        }else{
                            new AlertDialog.Builder(getContext())
                                    .setTitle("エラー")
                                    .setMessage("ユーザー登録に失敗しました。")
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
