package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FaceImportNTemplate extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int NTEMPLATE_REQUEST_CODE = 3;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_import_ntemplate);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        // Setting up activity components
        Button importNTemplateButton = findViewById(R.id.import_ntemplate_button);
        importNTemplateButton.setOnClickListener(v -> openFile(NTEMPLATE_REQUEST_CODE, "*/*"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == NTEMPLATE_REQUEST_CODE) {
                importNTemplate(uriToBytes(data.getData()));
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

    // Helper function to open image.
    private void openFile(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
    }

    // Main activity function.
    private void importNTemplate(byte[] nTemplateBytes) {
        new Thread(() -> {
            try {
                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

                // Start NTemplate import.
                byte[] registrationKey = mMMID.startImportNTemplate(nTemplateBytes);

                // After import NTemplate is finished, the registration key must be sent to the licensing server to acquire the server key.
                byte[] serverKey = mOperationApi.validate(registrationKey);

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishOperation(serverKey);

                // Check if import NTemplate succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Importing failed: %s", result.getStatus()));
                    return;
                }

                // Get template from import NTemplate action.
                byte[] resultTemplate = result.getTemplate();

                // Save template to file.
                saveTemplate(resultTemplate);
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed importNTemplate function: " + e.getMessage());
            }
        }).start();
    }

    // Helper function to save template.
    private void saveTemplate(byte[] template) {
        showToast("Select where to save the template");
        requestToSaveFile("MMIDTemplate.dat", path -> {
            try {
                saveDataToUri(FaceImportNTemplate.this, path, template);
                showToast("MMIDTemplate saved to " + path.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed saveDataToUri in requestToSaveFile function: " + e.getMessage());
            }
        });
    }
}
