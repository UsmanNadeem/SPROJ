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
		<-apk filename> OR <-dex filename>
		<-d or -displayOnly> Optional argument if you do not want to find leaks. Only displays calls to sources and sinks.

e.g to find leaks in app.apk execute

	java -jar target/SPROJ-1.0-jar-with-dependencies.jar -apk app.apk
