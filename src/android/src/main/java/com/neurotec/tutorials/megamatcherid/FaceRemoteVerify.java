package com.neurotec.tutorials.megamatcherid;

import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.neurotec.mega.matcher.id.client.NCapturePreviewEvent;
import com.neurotec.mega.matcher.id.client.MegaMatcherIdClient;
import com.neurotec.mega.matcher.id.client.NIcaoWarnings;
import com.neurotec.mega.matcher.id.client.NLivenessAction;
import com.neurotec.mega.matcher.id.client.NLivenessMode;
import com.neurotec.mega.matcher.id.client.NOperationResult;
import com.neurotec.mega.matcher.id.client.NStatus;
import com.neurotec.mega.matcher.id.client.NModality;
import com.neurotec.tutorials.utils.BaseActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// ================================================
// Uncomment to build Remote Verification on MMID Web
// ================================================
// import com.neurotec.megamatcheridmanagement.client.rest.ApiClient;
// import com.neurotec.megamatcheridmanagement.client.rest.api.OperationsApi;


// ===================================================================================
// To build this tutorial, first copy the mega-matcher-id-management-client-user.jar
// from the MegaMatcherId_Server_Client_X.zip to the Bin/Android directory.
// Then uncomment code labeled 'Uncomment to build Remote Verification on MMID Web' in
// the RemoteVerify.java and build.gradle files.
// ===================================================================================

public final class FaceRemoteVerify extends BaseActivity {

    private TextView mScoreValue;
    private TextView mActionValue;
    private TextView mIcaoValue;

    private Spinner mLivenessModeSpinner;
    private RadioButton mIcaoTrueButton;
    private EditText mSubjectIdEditText;

    private MegaMatcherIdClient mMMID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setting up activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_remote_verify);

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

        mSubjectIdEditText = findViewById(R.id.subject_id_edit_text);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> remoteVerify());
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

    // Main activity function.
    private void remoteVerify() {
        new Thread(() -> {
            try {
                // Get Liveness Mode, ICAO Compliance and MMID template data.
                NLivenessMode livenessMode = getLivenessMode();
                boolean icao = getIcaoCheck();

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

                // Sets modality to be used in this operation.
                mMMID.setActiveModality(NModality.FACE);

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
                byte[] serverKey = operationsApi.remoteVerify(encryptedTemplate, mSubjectIdEditText.getText().toString(), null, "FACE_MODALITY");

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

                /*
                The final result also includes:
                    - Image/token image
                    - JPEG2000 image/token image.
                    - Template: includes a MegaMatcher on Card template. This is only returned when using a non-trial MegaMatcherId template.
                Also, as NOperationResult extends NCapturePreview, it can be used to get all of the preview properties.
                */
            } catch (Exception e) {
                e.printStackTrace();
                showDialog(e.getMessage());
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
