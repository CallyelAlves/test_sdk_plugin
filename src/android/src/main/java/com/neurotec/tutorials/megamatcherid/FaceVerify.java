package com.neurotec.tutorials.megamatcherid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.neurotec.mega.matcher.id.client.NCapturePreviewEvent;
import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NIcaoWarnings;
import com.neurotec.mega.matcher.id.client.NLivenessAction;
import com.neurotec.mega.matcher.id.client.NLivenessMode;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public final class FaceVerify extends BaseActivity {

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int TEMPLATE_REQUEST_CODE = 3;

    private TextView mScoreValue;
    private TextView mActionValue;
    private TextView mIcaoValue;

    private Spinner mLivenessModeSpinner;
    private RadioButton mIcaoTrueButton;

    private MegaMatcherIdClient mMMID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_verify);

        /*
        Initialize client with the Application ID that has to match the Internet License or Dongle used.
        When using the Cloud service, application id must be set to 1.
        */
        mMMID = new MegaMatcherIdClient(1);

        // Setting up activity components
        String[] livenessModes = new String[NLivenessMode.values().length];
        for (int i = 0; i < NLivenessMode.values().length; ++i) {
            livenessModes[i] = NLivenessMode.get(i).name();
        }

        mLivenessModeSpinner = (Spinner) findViewById(R.id.liveness_mode_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, livenessModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLivenessModeSpinner.setAdapter(adapter);

        mScoreValue = findViewById(R.id.liveness_score_label);
        mActionValue = findViewById(R.id.liveness_action_label);
        mIcaoValue = findViewById(R.id.icao_warnings_label);

        mIcaoTrueButton = findViewById(R.id.icao_true_button);

        Button selectTemplateButton = findViewById(R.id.open_template_button);
        selectTemplateButton.setOnClickListener(v -> selectTemplate(TEMPLATE_REQUEST_CODE, "*/*"));
    }

    // Activity components results.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TEMPLATE_REQUEST_CODE) {
                verify(uriToBytes(data.getData()));
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

    // Helper function to get liveness mode.
    private NLivenessMode getLivenessMode() {
        return NLivenessMode.values()[(int)mLivenessModeSpinner.getSelectedItemId()];
    }

    // Helper function to get ICAO check.
    private boolean getIcaoCheck() {
        return mIcaoTrueButton.isChecked();
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
                // Get Liveness Mode, ICAO Compliance and MMID template data.
                NLivenessMode livenessMode = getLivenessMode();
                Boolean icao = getIcaoCheck();
                byte[] template = data;

                // Find all available cameras.
                String[] cameras = mMMID.getAvailableCameraNames();
                if (cameras.length == 0) {
                    System.out.println("No camera detected");
                }

                // Use the first camera that's available.
                String camera = Arrays.asList(mMMID.getAvailableCameraNames()).get(1);
                mMMID.setCurrentCamera(camera);
                showToast("Capturing with: " + camera);

                // Set the capture preview callback.
                mMMID.setCapturePreviewListener(this::updatePreview);

                // Setting Liveness mode with icao check.
                mMMID.setLivenessMode(livenessMode);
                mMMID.setCheckIcaoCompliance(icao);

                // Sets the minimum required verification score that determines if the compared faces are a match.
                mMMID.setMatchingThreshold((byte) 48);

                /*
                IcaoFilter is used to ignore positional warnings which cannot be turned off by using a threshold.
                Such warnings are: RollLeft, RollRight, YawLeft, YawRight, PitchUp, PitchDown, TooNear, TooFar, TooNorth, TooSouth, TooEast, TooWest.
                For example, to not force the user to position the face at the center of the image, use:
                */
                // mMMID.setIcaoWarningFilter(EnumSet.of(NIcaoPositionWarnings.TOO_EAST, NIcaoPositionWarnings.TOO_SOUTH, NIcaoPositionWarnings.TOO_EAST, NIcaoPositionWarnings.TOO_WEST));

                /*
                The ICAO capturing process can be fined tuned by adjusting each of the ICAO warning thresholds.
                More details about this process can be found in the documentation's section "3.6 ICAO Threshold Meanings".
                */
                mMMID.setIcaoWarningThreshold(NIcaoWarnings.SKIN_REFLECTION, (byte) 50);

                // Sets image quality threshold for image extraction (face recognition), not related to liveness.
                mMMID.setQualityThreshold((byte) 50);

                // Threshold which determines if liveness prediction is confident enough.
                mMMID.setLivenessThreshold((byte) 50);

                /*
                Threshold which regulates the positioning restrictions. The higher the threshold, the more centerted
                the face with respect to image size will have to be. This is also used for speed and less for accuracy.
                */
                mMMID.setPassiveLivenessSensitivityThreshold((byte) 30);

                // Threshold which determines if image quality is suitable for liveness. Used for extraction speed up, not for accuracy.
                mMMID.setPassiveLivenessQualityThreshold((byte) 40);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

                // Verify using the first available camera.
                NOperationResult result = mMMID.verify(template);

                // Check if verification succeeded.
                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                // Get matching score and age from result.
                showToast(String.format("Success matching score: %d", result.getMatchingScore()));

                /*
                The final result also includes:
                    - Image/token image
                    - JPEG2000 image/token image.
                    - Template: includes a MegaMatcher on Card template. This is only returned when using a non-trial MegaMatcherId template.
                Also, as NOperationResult extends NCapturePreview, it can be used to get all of the preview properties.
                */
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed verify function: " + e.getMessage());
            }
        }).start();
    }

    // Callback function to show preview.
    private void updatePreview(NCapturePreviewEvent event) {
        // To get the image use event.getCapturePreview().getImage() function which returns a 32 bit depth RGBA image.
        String score = String.valueOf(event.getCapturePreview().getLivenessScore());
        String actionList = "";
        String icaoWarnings = "";
        for (NLivenessAction action: event.getCapturePreview().getLivenessAction()) {
            actionList += action.name() + " ";
        }
        for (NIcaoWarnings warning: event.getCapturePreview().getIcaoWarnings()) {
            icaoWarnings += warning.name() + " ";
        }
        String finalActionList = actionList;
        String finalIcaoWarnings = icaoWarnings;
        runOnUiThread(() -> {
            mScoreValue.setText(score);
            mActionValue.setText(finalActionList);
            mIcaoValue.setText(finalIcaoWarnings);
        });

        /*
        It's also possible to get:
            - Yaw
            - Roll
            - Pitch
            - Quality
            - BoundingRect: the coordinates of the face rectangle.
            - LivenessTargetYaw: returned when using Active liveness mode.
        */
    }
}
