# JeditFX
JeditFX is a very lightweight texteditor written in JavaFX similar to Notepad on Windows or Textedit on OSX.
The purpose was to do a small demo app in javafx for opening large files.
The app uses in the background RichtextFX which is a great component handling the heap memore in a very good manner.

Actually the editor can handle files as big as the RAM is. Some source is already there to extend this function to limitless file size. 

The name JeditFX is derived from Jedit but is far from being Jedit with a nicer GUI. Maybe sometimes in the future it will be there but actually it is only a simple text editor with undo support. Originally I had always trouble with Jedit and large files which I do not think they can fix soon because of the limitation of the Swing framework in this regard.

Commented out in the maven file is also the compiling with GraalVM. Basically functional but there are issues with the keyboard shortcuts and therefore I decided to go with JPackage option. To build you need JDK14 because of the jpackage and the faster startup time.

This is my first github project and I do not know how I can do the build of the project automatically. Maybe someone else can pick this up. Just create an issue and I can give you the right to write to the repo.
