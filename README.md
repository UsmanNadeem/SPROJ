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

`java -jar target/SPROJ-1.0-jar-with-dependencies.jar <args>`

	Arguments:
		-apk <filename>
		-dex <filename>
		-d OR -displayOnly 				Do not find leaks Only display sources and sinks

e.g to find leaks in app.apk execute:

	java -jar target/SPROJ-1.0-jar-with-dependencies.jar -apk bluetooth.apk

Or if you want to write output to a file:

	java -jar target/SPROJ-1.0-jar-with-dependencies.jar -apk bluetooth.apk > output.txt
