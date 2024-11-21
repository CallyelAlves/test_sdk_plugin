package com.neurotec.tutorials.megamatcherid;

import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.tutorials.utils.BaseActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class VoiceVerify extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int TEMPLATE_REQUEST_CODE = 3;

    private MegaMatcherIdClient mMMID = null;

    private Button mStartVerifyButton;
    private byte[] mTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_verify);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        Button selectTemplateButton = findViewById(R.id.open_template_button);
        selectTemplateButton.setOnClickListener(v -> selectTemplate(TEMPLATE_REQUEST_CODE, "*/*"));
        mStartVerifyButton = findViewById(R.id.start_verify);
        mStartVerifyButton.setOnClickListener(v -> verify(mTemplate));
        mStartVerifyButton.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TEMPLATE_REQUEST_CODE) {
                mTemplate = uriToBytes(data.getData());
                mStartVerifyButton.setEnabled(true);
            }
        }
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

    // Helper function to get bytes from uri.
    private byte[] uriToBytes(Uri uri) {
        byte[] buf = new byte[0];

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int bytesRead;
            byte[] tempBuffer = new byte[READ_BUFFER_SIZE];
            while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
                buffer.write(tempBuffer, 0, bytesRead);
            }
            buf = buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Failed uriToBytes function: " + e.getMessage());
        }

        return buf;
    }

    // Helper function to select template.
    private void selectTemplate(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
    }

    // Main activity function.
    private void verify(byte[] data) {
        new Thread(() -> {
            try {
                // Get MMID template data.
                byte[] template = data;

                // Find all available cameras.
                String[] microphones = mMMID.getAvailableMicrophoneNames();
                if (microphones.length == 0) {
                    System.out.println("No microphone detected");
                }

                // Use the first camera that's available.
                String microphone = Arrays.asList(mMMID.getAvailableMicrophoneNames()).get(0);
                mMMID.setCurrentMicrophone(microphone);
                showToast("Selected microphone: " + microphone);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.VOICE);

                // Verify using the first available camera.
                NOperationResult result = mMMID.verify(template);

                // Check if verification succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                // Get matching score from result.
                showToast(String.format("Success matching score: %d", result.getMatchingScore()));
            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mStartVerifyButton.setEnabled(true);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mStartVerifyButton.setEnabled(false);
        });
    }
}
