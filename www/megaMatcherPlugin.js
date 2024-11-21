module.exports = {

    get: function (success, failure) {
        cordova.exec(success, failure, "MegaMatcherPlugin", "get", []);
    }

}
