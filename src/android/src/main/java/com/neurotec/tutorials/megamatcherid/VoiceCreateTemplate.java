package com.neurotec.tutorials.megamatcherid;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NIcaoWarnings;
import com.neurotec.mega.matcher.id.client.NLivenessMode;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.IOException;
import java.util.Arrays;

public class VoiceCreateTemplate extends BaseActivity {

    private Button mCreateTemplateButton;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_create_template);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        mCreateTemplateButton = (Button) findViewById(R.id.create_template_button);
        mCreateTemplateButton.setOnClickListener(v -> createTemplate());
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

    // Helper function to save template.
    private void saveTemplate(byte[] template) {
        showToast("Select where to save the template");
        requestToSaveFile("MMIDTemplate.dat", path -> {
            try {
                saveDataToUri(VoiceCreateTemplate.this, path, template);
                showToast("MMIDTemplate saved to " + path.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed saveDataToUri in requestToSaveFile function: " + e.getMessage());
            }
        });
    }

    // Main activity function.
    private void createTemplate() {
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

                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.VOICE);

                // Start template creation.
                byte[] registrationKey = mMMID.startCreateTemplate();

                // After template creation is finished, the registration key must be sent to the licensing server to acquire the server key.
                byte[] serverKey = mOperationApi.validate(registrationKey);

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishOperation(serverKey);

                // Check if template creation succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                // Get quality and age from result.
                showToast(String.format("Success with %s quality", result.getQuality()));

                // Get template from create template action.
                byte[] resultTemplate = result.getTemplate();

                // Save template to file.
                saveTemplate(resultTemplate);

            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mCreateTemplateButton.setEnabled(true);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mCreateTemplateButton.setEnabled(false);
        });
    }
}
