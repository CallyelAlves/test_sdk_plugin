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
    protected void onDestroy() {
        super.onDestroy();
        try {
            mMMID.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checks() {
        new Thread(() -> {
            try {
                System.out.println("Camera initialized successfully");
                System.out.println("Available cameras: " + Arrays.toString(mMMID.getAvailableCameraNames()));

                NLivenessMode livenessMode = getLivenessMode();
                Boolean icao = getIcaoCheck();

                String[] cameras = mMMID.getAvailableCameraNames();
                if (cameras.length == 0) {
                    System.out.println("No camera detected");
                }

                String camera = Arrays.asList(mMMID.getAvailableCameraNames()).get(1);
                mMMID.setCurrentCamera(camera);
                showToast("Capturing with: " + camera);

                mMMID.setCapturePreviewListener(this::updatePreview);

                mMMID.setLivenessMode(livenessMode);
                mMMID.setCheckIcaoCompliance(icao);

                mMMID.setIcaoWarningThreshold(NIcaoWarnings.SKIN_REFLECTION, (byte) 50);

                mMMID.setQualityThreshold((byte) 50);

                mMMID.setLivenessThreshold((byte) 50);

                mMMID.setPassiveLivenessSensitivityThreshold((byte) 30);

                mMMID.setPassiveLivenessQualityThreshold((byte) 40);

                ApiClient client = new ApiClient();
                client.setBasePath("http://licensing.megamatcherid.online/rs");
                client.setApiKey("ijb476bil6eit7864bqkp802c5");
                client.setConnectTimeout(10000);
                mOperationApi = new OperationApi(client);

                mMMID.setActiveModality(NModality.FACE);

                byte[] registrationKey = mMMID.startChecks();

                byte[] serverKey = mOperationApi.validate(registrationKey);

                NOperationResult result = mMMID.finishOperation(serverKey);

                if (result.getStatus() != NStatus.SUCCESS) {
                    showToast(String.format("Capturing failed: %s", result.getStatus()));
                    return;
                }

                showToast(String.format("Success with %s quality, Estimated age: %d", result.getQuality(), result.getAge()));
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

    private void updatePreview(NCapturePreviewEvent event) {
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
