package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FaceVerifyAgainstNTemplate extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int TEMPLATE_REQUEST_CODE = 3;
    private static final int NTEMPLATE_REQUEST_CODE = 4;

    private Button mSelectMMIDTemplateButton;
    private Button mSelectNTemplateButton;
    private Button mVerifyButton;

    private MegaMatcherIdClient mMMID = null;

    private boolean templateSelected = false;
    private boolean nTemplateSelected = false;

    private byte[] templateBytes;
    private byte[] nTemplateBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_verify_ntemplate);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        // Setting up activity components  
        mSelectMMIDTemplateButton = findViewById(R.id.open_fvtemplate_button);
        mSelectNTemplateButton = findViewById(R.id.open_ntemplate_button);
        mVerifyButton = findViewById(R.id.verify_button);

        mSelectMMIDTemplateButton.setOnClickListener(v -> selectTemplate(TEMPLATE_REQUEST_CODE, "*/*"));
        mSelectNTemplateButton.setOnClickListener(v -> selectNTemplate(NTEMPLATE_REQUEST_CODE, "*/*"));
        mVerifyButton.setOnClickListener(v -> verifyAgainstNTemplate());

        runOnUiThread(() -> {
            mSelectMMIDTemplateButton.setEnabled(true);
            mSelectNTemplateButton.setEnabled(true);
            mVerifyButton.setEnabled(false);
        });
    }

    // Selects templates for verification.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TEMPLATE_REQUEST_CODE) {
                templateSelected = true;
                templateBytes = uriToBytes(data.getData());
                runOnUiThread(() -> {
                    mSelectMMIDTemplateButton.setEnabled(false);
                });
            } else if (requestCode == NTEMPLATE_REQUEST_CODE) {
                nTemplateSelected = true;
                nTemplateBytes = uriToBytes(data.getData());
                runOnUiThread(() -> {
                    mSelectNTemplateButton.setEnabled(false);
                });
            }
            if (templateSelected && nTemplateSelected ) {
                runOnUiThread(() -> {
                    mVerifyButton.setEnabled(true);
                });
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

    // Helper function to select NTemplate
    private void selectNTemplate(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
    }

    // Main activity function.
    private void verifyAgainstNTemplate() {
        new Thread(() -> {
            try {
                // Sets the minimum required verification score that determines if the compared faces are a match.
                mMMID.setMatchingThreshold((byte) 48);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

			    // Verify against NTemplate.
                NOperationResult result = mMMID.verify(templateBytes, nTemplateBytes);

                // Check if verify against NTemplate succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                } 

                // Get matching score from result.
			    showToast(String.format("Verified successfully, matching score: %d", result.getMatchingScore()));

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed verify function: " + e.getMessage());
            }
        }).start();

        runOnUiThread(() -> {
            mVerifyButton.setEnabled(false);
            mSelectNTemplateButton.setEnabled(true);
            mSelectMMIDTemplateButton.setEnabled(true);
        });
    }
}
