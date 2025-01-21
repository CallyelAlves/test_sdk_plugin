var megaMatcherPlugin = {
    recognizeFace: function(image, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "MegaMatcherPlugin", "recognizeFace", [image]);
    }
};

module.exports = megaMatcherPlugin;
