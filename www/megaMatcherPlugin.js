var megaMatcherPlugin = {
    recognizeFace: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "MegaMatcherPlugin", "recognizeFace", []);
    }
};

module.exports = megaMatcherPlugin;
