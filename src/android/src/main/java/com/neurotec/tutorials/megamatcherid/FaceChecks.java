package com.neurotec.tutorials.megamatcherid;

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
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.client.NLivenessMode;
import com.neurotec.mega.matcher.id.client.NOperationResult;

import com.neurotec.mega.matcher.id.server.rest.ApiClient;
import com.neurotec.mega.matcher.id.server.rest.api.OperationApi;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.IOException;
import java.util.Arrays;

public final class FaceChecks extends BaseActivity {

    private TextView mScoreValue;
    private TextView mActionValue;
    private TextView mIcaoValue;

    private Button mStartChecksButton;
    private Spinner mLivenessModeSpinner;
    private RadioButton mIcaoTrueButton;

    private MegaMatcherIdClient mMMID = null;
    private OperationApi mOperationApi = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.face_checks);

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

        // mLivenessModeSpinner = (Spinner) findViewById(R.id.liveness_mode_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, livenessModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLivenessModeSpinner.setAdapter(adapter);

        // mScoreValue = findViewById(R.id.liveness_score_label);
        // mActionValue = findViewById(R.id.liveness_action_label);
        // mIcaoValue = findViewById(R.id.icao_warnings_label);

        // mIcaoTrueButton = findViewById(R.id.icao_true_button);

        // mStartChecksButton = (Button) findViewById(R.id.start_checks_button);
        mStartChecksButton.setOnClickListener(v -> checks());
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
    public void checks() {
        new Thread(() -> {
            try {
                // Get Liveness Mode and ICAO Compliance.
                NLivenessMode livenessMode = getLivenessMode();
                Boolean icao = getIcaoCheck();

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

                // Setting up client for communication with the licensing server.
                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

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

                // Get quality and age from result.
                showToast(String.format("Success with %s quality, Estimated age: %d", result.getQuality(), result.getAge()));
                /*
                The final result also includes:
                    - Image/token image
                    - JPEG2000 image/token image.
                Also, as NOperationResult extends NCapturePreview, it can be used to get all of the preview properties.
                */
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Failed startCapturing function: " + e.getMessage());
            } finally {
                runOnUiThread(() -> {
                    mStartChecksButton.setEnabled(true);
                });
            }
        }).start();

        runOnUiThread(() -> {
            mStartChecksButton.setEnabled(false);
        });
    }

    private NLivenessMode getLivenessMode() {
        return NLivenessMode.values()[(int)mLivenessModeSpinner.getSelectedItemId()];
    }

    private boolean getIcaoCheck() {
        return mIcaoTrueButton.isChecked();
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
