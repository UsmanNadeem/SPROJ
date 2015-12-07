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

`java -jar target/SPROJ-1.0-jar-with-dependencies.jar app.apk com/example/android/bluetoothchat/ <l or d>`

	where: com/example/android/bluetoothchat/ is the package of interest in your app
	l means find leaks and d means display all sources and sinks