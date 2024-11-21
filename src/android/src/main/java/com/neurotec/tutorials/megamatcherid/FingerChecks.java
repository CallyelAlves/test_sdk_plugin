package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NFinger;
import com.neurotec.mega.matcher.id.client.NImageSource;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FingerChecks extends BaseActivity {

    private Button mStartChecksButton;
    private Button mCaptureButton;
    private TextView mResultTextView;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finger_checks);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(v -> mMMID.force());
        mCaptureButton.setEnabled(false);
        mStartChecksButton = (Button) findViewById(R.id.start_checks_button);
        mStartChecksButton.setOnClickListener(v -> checksFromImage());
        mResultTextView = findViewById(R.id.finger_result);
    }

    // Clean up after completing.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mMMID.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main activity function.
    private void checksFromImage() {
        new Thread(() -> {
            try {
                // Find all available cameras.
                String[] cameras = mMMID.getAvailableCameraNames();
                if (cameras.length == 0) {
                    System.out.println("No camera detected");
                }

                // Use the first camera that's available.
                String camera = Arrays.asList(mMMID.getAvailableCameraNames()).get(0);
                mMMID.setCurrentCamera(camera);
                showToast("Capturing with: " + camera);

                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                // Sets the required finger count, which is reset after the capture. Default is 4.
                mMMID.setRequiredFingersCount(4);

                // Sets whether the finger import image is captured by a scanner or by a camera. Default is true.
                mMMID.setFingerImageSource(NImageSource.CAMERA);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FINGERS_LEFT_HAND);

                // Start checks.
                byte[] registrationKey = mMMID.startChecks();

                // After checks are finished, the registration key must be sent to the licensing server to acquire the server key.
                byte[] serverKey = mOperationApi.validate(registrationKey);

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishOperation(serverKey);

                // Check if checks succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                // Getting each fingers estimated position and quality.
                StringBuilder resultText = new StringBuilder();
                NFinger[] fingers = result.getFingers();
                List<ImageData> imageList = new ArrayList<>();
                for (int i = 0; i < fingers.length; i++) {
                    imageList.add(new ImageData(fingers[i].getImage(), fingers[i].getEstimatedPosition() + "_image"));
                    imageList.add(new ImageData(fingers[i].getImageBinarized(), fingers[i].getEstimatedPosition() + "_image_binarized"));
                    resultText.append((i + 1)).append(". Quality: ")
                            .append(fingers[i].getQuality())
                            .append(", position: ")
                            .append(fingers[i].getEstimatedPosition())
                            .append("\n");
                }
                mResultTextView.setText(resultText);

                // Saving images and binarized images to download directory.
                for (ImageData data: imageList) {
                    saveData(data);
                }

                // Get quality and age from result.
                showToast(String.format("Success with %s quality, and saved image in download folder", result.getQuality()));

            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mStartChecksButton.setEnabled(true);
                    mCaptureButton.setEnabled(false);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mStartChecksButton.setEnabled(false);
            mCaptureButton.setEnabled(true);
        });

    }

    private static void saveData(ImageData data) throws IOException {
        try {
            Bitmap bmp = data.getImage();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            bmp.recycle();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), data.getName() + ".jpg");
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(byteArray);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
