# IMG Module (ICL.img)

Classes for using Images (BufferedImage, File, FrameShot)  
and non-opencv related code, trying to keep dependencies  
to a minimum. It also includes common geometric operations.

[JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/modules/img/docs/)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/img)

## Buffered Image Tools

* `BuffImgTools` : Do operations on BufferedImages (rotate, crop, etc)
* `FileImgTools` : Do operations on image files (convenient wrapper around BuffImgTools)
* `Converter` : Convert from and to File, BufferedImage, byte array, etc.

### Example

```groovy
File imgFile = new File("image.jpg")
if(FileImgTools.isValidJPG(imgFile)) {
    BufferedImage image = ImageIO.read(imgFile)
    image = BuffImageTools.resizeCentered(image, 500)  // resize to 500 in width
    image = BuffImageTools.crop(image, 0, 0, 500, 500) // Make it square
    image = BuffImageTools.rotate(image, 90)           // Rotate 90 degrees 
    BufferedImage copy = BuffImageTools.copy(image)    // copy image
    BufferedImage.show(copy)                           // Display image on a frame 
    byte[] bytes = Converter.bufferedToBytes(copy)     // Convert to byte array
}
```

## FrameShot

Represents a single frame (BufferedImage with name). 
Used to be sure that we keep the original file name when 
handling BufferedImages. 

## Metry

Performs geometric operations (e.g, Trigonometry) using
`java.awt.geom` classes. Some examples are:

* translate coordinates (useful to work on scaled images)
* intersection between shapes
* calculate slop of lines
* resize shapes
* rotate rectangles
