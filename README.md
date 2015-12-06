# SPROJ #
Sproj

## Building ##

If you do not have Maven installed:

`sudo apt-get install maven`

If you have Maven, the build script should run the correct command:

`./build.sh`

This will create a file:

`target/SPROJ-0.1-jar-with-dependencies.jar`

## Running ##

`java -jar target/SPROJ-0.1-jar-with-dependencies.jar <classes.dex/apkfile> <com/example/android/bluetoothchat/>`
`where com/example/android/bluetoothchat/ is the package of interest`
