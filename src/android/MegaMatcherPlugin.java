import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class MegaMatcherPlugin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("recognizeFace")) {
            this.startFaceChecks(callbackContext);
            return true;
        }
        return false;
    }

    private void startFaceChecks(CallbackContext callbackContext) {
        try {
            // Inicializa e executa a lógica da classe FaceChecks
            FaceChecks faceChecks = new FaceChecks();
            faceChecks.checks(); // Chama o método principal para reconhecimento facial

            callbackContext.success("Face checks initiated successfully");
        } catch (Exception e) {
            callbackContext.error("Error initiating face checks: " + e.getMessage());
        }
    }
}
