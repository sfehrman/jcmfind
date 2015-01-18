# jcmfind
Java Class Method Finder utility

jcmfind will "walk" a (unix) file system looking for the Java claas name and/or method name specified.  It will recursively open and search nested zip, ear, war, jar files.  There are a few options for changing the search behavior and the displayed output.

This project is made available under the Common Development and Distribution License (CDDL) 1.0.
http://opensource.org/licenses/CDDL-1.0

This project requires maven to build ...

* Change to the "mavenproject" folder
* run: mvn clean compile package install assembly:assembly
* unpack the "target/jcm.zip" file to local folder and set your PATH to its "bin" folder.
* run the "jcmfind" commmand
