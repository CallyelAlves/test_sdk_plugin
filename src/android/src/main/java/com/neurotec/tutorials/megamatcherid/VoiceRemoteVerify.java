package com.neurotec.tutorials.megamatcherid;

import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// ================================================
// Uncomment to build Remote Verification on MMID Web
// ================================================
// import com.neurotec.megamatcheridmanagement.client.rest.ApiClient;
// import com.neurotec.megamatcheridmanagement.client.rest.api.OperationsApi;



public class VoiceRemoteVerify  extends BaseActivity {

    private EditText mSubjectIdEditText;
    private Button mStartButton;

    private MegaMatcherIdClient mMMID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_remote_verify);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        mSubjectIdEditText = findViewById(R.id.subject_id_edit_text);

        mStartButton = findViewById(R.id.start_button);
        mStartButton.setOnClickListener(v -> remoteVerify());
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
    private void remoteVerify() {
        new Thread(() -> {
            try {

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

                byte[] encryptedTemplate = mMMID.startRemoteVerification();

//              ================================================
//              Uncomment to build Remote Verification on MMID Web (also uncomment in build.gradle)
//              ================================================
                showToast("Please uncomment code to enable");
                /*
                // Setting up client for communication with the MMID Web Management.
                ApiClient apiClient = new ApiClient();
                apiClient.setBasePath("https://megamatcherid.online/api");
                apiClient.setConnectTimeout(10000);
                // Base64 encode the username and password
                String credentialsBase64 = Base64.encodeToString("user:admin".getBytes(StandardCharsets.UTF_8), Base64.DEFAULT).trim();
                apiClient.addDefaultHeader("Authorization", "Basic " + credentialsBase64);
                OperationsApi operationsApi = new OperationsApi(apiClient);

                // After capture is finished, the encrypted template must be sent to the Web Management Server for remote verification. This will acquire the server key.
                byte[] serverKey = operationsApi.remoteVerify(encryptedTemplate, mSubjectIdEditText.getText().toString(), null, "VOICE_MODALITY");

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishRemoteVerification(serverKey);

                // Check if verification succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Remote verification failed: %s", result.getStatus()));
                    return;
                } else {
                    showToast("Remote verification succeeded");
                }
                */

            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mStartButton.setEnabled(true);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mStartButton.setEnabled(false);
        });
    }
}
