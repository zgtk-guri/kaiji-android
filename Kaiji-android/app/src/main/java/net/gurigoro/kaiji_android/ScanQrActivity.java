package net.gurigoro.kaiji_android;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class ScanQrActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;
    private Button cancelButton;
    private CameraSource cameraSource;

    public final static int TAG = 1858;
    public final static String QR_VALUE_KEY = "qr_value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        // Initialize
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        BarcodeProcessorFactory barcodeProcessorFactory = new BarcodeProcessorFactory();
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeProcessorFactory).build());
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(10.0f)
                .setAutoFocusEnabled(true)
                .build();

        surfaceView = (SurfaceView) findViewById(R.id.scan_qr_surface_view);
        cancelButton = (Button) findViewById(R.id.scan_qr_cancel_button);

        surfaceView.getHolder().addCallback(this);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraSource.stop();
        cameraSource.release();

        surfaceView.getHolder().removeCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            cameraSource.start(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private class BarcodeProcessorFactory implements MultiProcessor.Factory<Barcode>{

        @Override
        public Tracker<Barcode> create(Barcode barcode) {
            return new BarcodeTracker();
        }
    }

    private class BarcodeTracker extends Tracker<Barcode> {

        @Override
        public void onNewItem(int id, final Barcode item) {
            ScanQrActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.putExtra(QR_VALUE_KEY, item.rawValue);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });
        }
    }
}
