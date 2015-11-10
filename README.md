# FaceDetector
Find human faces in images using [JavaCV](https://github.com/bytedeco/javacv). Work in progress.

Create Gradlew wrapper using `gradle wrapper` then run the face detector using `gradlew run`.

Works currently only on Windows x86.
To try to change this, specify a different Jar as the `classifier` in `build.gradle`: 
`compile group: 'org.bytedeco.javacpp-presets', name: 'opencv', version: '3.0.0-1.1', classifier: "windows-x86"`

