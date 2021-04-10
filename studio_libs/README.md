# Launcher build dependent on more system java libs.


## Notes
Only use android studio build launcher for Development,don't release!!!


## Make new java libs:

first,cd the project root dir

### framework.jar
BUILD COMMAND:
$ make framework -j8
RENAME COMMAND:
$ cp /out/target/product/s9863a1h10/obj/JAVA_LIBRARIES/framework_intermediates/javalib.jar ./framework.jar

### plugin_core.jar
BUILD COMMAND:
$ make PluginCoreLib -j8
RENAME COMMAND:
$ cp out/target/product/s9863a1h10/obj/JAVA_LIBRARIES/PluginCoreLib_intermediates/javalib.jar ./plugin_core.jar


### sysui_shared.jar
BUILD COMMAND:
$ make SystemUISharedLib -j8
RENAME COMMAND:
$ cp out/target/product/s9863a1h10/obj/JAVA_LIBRARIES/SystemUISharedLib_intermediates/javalib.jar ./sysui_shared.jar


### launcher_protos.jar
BUILD COMMAND:
$ make launcherprotosnano -j8
RENAME COMMAND:
$ cp out/target/product/s9863a1h10/obj/JAVA_LIBRARIES/launcherprotosnano_intermediates/javalib.jar ./launcher_protos.jar

### IconLoader.aar
BUILD WAYS:
1)import Launcher3 project to android studio.
2)modify Launcher3 build.gradle,use IconLoader project.eg:
    implementation project(':IconLoader')
    //implementation(name: 'IconLoader', ext: 'aar')
3)Build->Make Module'IconLoader', temp fix the SimpleIconCache.java build error.eg:
user.getIdentifier() ==> user.hashCode()
info.isInstantApp() ==> false
4)Re Build Module'IconLoader', rename the out aar file:
$ cp iconloaderlib/build/outputs/aar/IconLoader-debug.aar ./IconLoader.aar
5)restore Launcher3 build.gradle & SimpleIconCache.java

## Use new java libs:
use the new lib files replace the old libs, rebuild launcher on android studio.



