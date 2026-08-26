---
name: img
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for image handling - resizing, cropping and rotating BufferedImages or image files (BuffImgTools, FileImgTools), converting between File, byte arrays and BufferedImage (Converter), named frames (FrameShot), and geometry/trigonometry helpers (Metry). Deliberately keeps dependencies minimal (no OpenCV). Triggers on com.intellisrc.img imports in an ICL project. Includes core.
---

# ICL img Module

Image operations for ICL without OpenCV — works on `java.awt.image.BufferedImage`, files and byte arrays. Includes core; uses Thumbnailator (bundled).

```groovy
implementation 'com.intellisrc:img:2.10.5'   // + core + groovy
```

Package: `com.intellisrc.img`.

## In-memory operations — BuffImgTools (static)

```groovy
import com.intellisrc.img.BuffImgTools

img = BuffImgTools.resize(img, 640, 480)          // also resizeWidth(img, w), resizeHeight(img, h)
img = BuffImgTools.resizeCentered(img, 500)       // scale + center-crop to square; resizeTopLeft(...)
img = BuffImgTools.crop(img, x, y, w, h)
img = BuffImgTools.rotate(img, 90)                // ONLY 0/90/180/270 (assertion otherwise)
img = BuffImgTools.copy(img)
BuffImgTools.getSize(img)                         // Size (width/height)
BuffImgTools.show(img)                            // debug window
```

## File operations — FileImgTools (static)

```groovy
import com.intellisrc.img.FileImgTools

FileImgTools.resize(new File("in.jpg"), new File("out.png"), 800, 600)  // format from extension
FileImgTools.resizeWidth(in, out, 800)            // also resizeHeight, resizeCentered, resizeTopLeft
FileImgTools.rotate(in, out, 90) ; FileImgTools.crop(in, out, x, y, w, h)
FileImgTools.getSize(file) ; FileImgTools.isValidJPG(file)
```

## Converter — between representations

```groovy
import com.intellisrc.img.Converter

BufferedImage img = Converter.FileToBuffered(file)
Converter.BufferedToFile(img, file)
BufferedImage b2 = Converter.bytesToBuffered(width, height, bytes)   // default TYPE_3BYTE_BGR
byte[] raw = Converter.bufferedToBytes(img)      // byte-backed types only (GRAY/BGR/ABGR)
// also bufferedToShortArray / bufferedToIntArray
```

## FrameShot — named frame

```groovy
FrameShot shot = new FrameShot(new File("frame_001.jpg"))   // converts to 3BYTE_BGR (OpenCV-friendly)
FrameShot shot2 = new FrameShot(img, "thumb.jpg")
shot.image ; shot.name
shot.save(new File("out/")) ; shot.release()    // release frees memory for large batches
```

## Metry — geometry

Static helpers over `java.awt.geom`: `translateCoords`, `intersect(line, shape)`, `getLineSlope`, `getYcrossValue`, `resizeRect`, `coordsToTopLeft/Center(rect)` (convert between center-based and top-left rect representations), `rotateRect(rect, angle, pivot)`.

## Gotchas

- `rotate` only accepts multiples of 90.
- `bufferedToBytes` logs a warning and returns empty for non-byte-backed image types — convert type first.
- Prefer `FileImgTools` for one-shot pipelines (reads and writes for you) and `BuffImgTools` when reusing the same image across several operations.
