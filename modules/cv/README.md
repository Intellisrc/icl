# CV Module (ICL.cv)

Classes for Computer Vision (extension to OpenCV). 
Convert image formats, crop, rotate images or draw 
objects on top of them. It simplifies grabbing images 
from any video source.

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/cv)

* `Converter` : Convert from and to CV image formats (extends img.Converter)
* `CvTools` : Perform common operations in images : rotate, resize, etc
* `FrameShot` : Extends img.FrameShot to support CV image format
* @ `JpegFormat` : JPEG Format
* @ `MjpegFormat` : MJPEG Format
* @ `MjpegFrame` : MJPEG single frame
* @ `MjpegInputStream` : Provides MJPEG as InputStream
* @ `VideoGrab` : Extract frames from most video formats
* `BufferedVideoGrab` : Get BufferedImages or FrameShot/CvFrameShot from a MJPEG stream
* `FileVideoGrab` : Get File or FrameShot/CvFrameShot images from a directory (as frames)
* `FrameVideoGrab` : Get Frame or FrameShot/CvFrameShot objects from video files
