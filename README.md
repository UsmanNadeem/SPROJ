# SPROJ #
Sproj

## Building ##

If you do not have Maven installed:

`sudo apt-get install maven`

If you have Maven, the build script should run the correct command:

`./build.sh`

This will create a file:

`target/SPROJ-1.0-jar-with-dependencies.jar`

## Running ##

`java -jar target/SPROJ-1.0-jar-with-dependencies.jar bluetooth.apk com/example/android/bluetoothchat/ <l or d>`

	where: com/example/android/bluetoothchat/ is the package of interest in your app
	l means find leaks and d means display all sources and sinks

e.g to find leaks in com/example/android/bluetooth package of app.apk(bluetoothchat) execute

	java -jar target/SPROJ-1.0-jar-with-dependencies.jar app.apk com/example/android/bluetoothchat/ l

or even with a less precise package name

	java -jar target/SPROJ-1.0-jar-with-dependencies.jar app.apk com/ l