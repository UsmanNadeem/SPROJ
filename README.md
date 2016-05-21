# SPROJ #
There are two branches the output for master branch is modified to enable patching. The other branch has user readable output.

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
		or
		-dex <filename>
		-d OR -displayOnly 				Do not find leaks Only display sources and sinks

e.g to find leaks in app.apk execute:

	java -jar SPROJ-forDemo.jar SPROJ -apk appName.apk

Or if you want to write output to a file:

	java -jar SPROJ-forDemo.jar SPROJ -apk appName.apk > output.txt

FOR PATCHING:

	To decompile:
		apktool d appName.apk
	
	To write leaks to a file:
		java -jar SPROJ-forPatcher.jar SPROJ -apk appName.apk > input.txt
	
	To Patch: 
		java Patcher appName

	To build apk again:
		apktool b appName
		
	Run to generate key:
		keytool -genkey -v -keystore my-releasekey.keystore -alias alias_name -keyalg RSA -validity 10000

	Sign the apk:
		jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore my-releasekey.keystore {appName}/dist/{appName}.apk alias_name
