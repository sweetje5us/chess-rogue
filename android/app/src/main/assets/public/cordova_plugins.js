  cordova.define('cordova/plugin_list', function(require, exports, module) {
    module.exports = [
      {
          "id": "com.medievaldash.adplugin.adPlugin",
          "file": "plugins/com.medievaldash.adplugin/www/adPlugin.js",
          "pluginId": "com.medievaldash.adplugin",
        "clobbers": [
          "cordova.plugins.adPlugin"
        ]
        }
    ];
    module.exports.metadata =
    // TOP OF METADATA
    {
      "com.medievaldash.adplugin": "1.0.0"
    };
    // BOTTOM OF METADATA
    });
    