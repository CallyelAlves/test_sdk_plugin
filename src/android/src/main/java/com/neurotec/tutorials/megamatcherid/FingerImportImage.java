package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FingerImportImage extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int IMAGE_REQUEST_CODE = 3;

    private RadioButton mCameraSourceButton;
    private TextView mResultTextView;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finger_import_image);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        // Setting up activity components;
        mCameraSourceButton = findViewById(R.id.source_camera_button);
        Button importImageButton = findViewById(R.id.tutorials_import_image_button);
        importImageButton.setOnClickListener(v -> openFile(IMAGE_REQUEST_CODE, "image/*"));
        mResultTextView = findViewById(R.id.finger_result);
    }

    // Activity components results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_REQUEST_CODE) {
                importImage(uriToBytes(data.getData()));
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

    // Helper function to open image.
    private void openFile(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        startActivityForResult(intent, requestCode);
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

    private NImageSource getImageSource() {
        if (mCameraSourceButton.isChecked()) {
            return NImageSource.CAMERA;
        } else {
            return NImageSource.SCANNER;
        }
    }

    // Main activity function.
    private void importImage(byte[] imageBytes) {
        new Thread(() -> {
            try {
                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                // Sets the required finger count, which is reset after the capture. Default is 4.
                mMMID.setRequiredFingersCount(4);

                // Sets whether the finger import image is captured by a scanner or by a camera. Default is true.
                mMMID.setFingerImageSource(getImageSource());

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FINGERS_LEFT_HAND);

                // Start checks from image.
                byte[] registrationKey = mMMID.startImportImage(imageBytes);

                // After checks are finished finished, the registration key must be sent to the licensing server to acquire the server key.
                byte[] serverKey = mOperationApi.validate(registrationKey);

                // The server key is used to get the final result.
                NOperationResult result = mMMID.finishOperation(serverKey);

                // Check if image checks succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.valueOf(result.getStatus()));
                    return;
                }

                // Get quality and age from result.
                showToast(String.format("Success with %s quality", result.getQuality()));

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

                // Get template from import Image action.
                byte[] resultTemplate = result.getTemplate();

                // Save template to file
                saveTemplate(resultTemplate);

            } catch (Exception e) {
                e.printStackTrace();
                showDialog("Failed checksFromImage function: " + e.getMessage());
            }
        }).start();
    }

    // Helper function to save template.
    private void saveTemplate(byte[] template) {
        showToast("Select where to save the template");
        requestToSaveFile("MMIDTemplate.dat", path -> {
            try {
                saveDataToUri(FingerImportImage.this, path, template);
                showToast("MMIDTemplate saved to " + path.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Failed saveDataToUri in requestToSaveFile function: " + e.getMessage());
            }
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