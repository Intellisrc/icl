package com.intellisrc.etc

import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @since 18/03/09.
 */
class ZipTest extends Specification {
    def "Test Zip and Unzip"() {
        setup:
            def str = """Some string to compress... because this message is to looooong to store!
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec vitae dui lorem. Ut vitae lorem id turpis feugiat fringilla. Suspendisse et gravida elit. Phasellus eleifend eget sapien et tempor. Fusce ullamcorper augue orci, vitae semper metus lacinia quis. Aenean non facilisis tortor. Curabitur in pharetra arcu. Cras nunc erat, scelerisque a tempus a, blandit id velit. Integer consequat consequat nisi, id vehicula elit blandit et. Proin accumsan placerat est, eget rutrum felis pharetra a. Ut varius ullamcorper arcu, sit amet dignissim metus tempus ac. Fusce tristique nisl eu tristique egestas.
            In in ligula porttitor, finibus turpis id, sollicitudin arcu. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nulla sapien dui, lobortis in elementum ac, fermentum nec quam. Cras dapibus mauris et ante tincidunt, ac aliquam leo lacinia. Sed feugiat nunc a turpis dapibus, vitae convallis tellus semper. Ut magna dui, finibus et augue nec, consequat hendrerit dolor. Donec scelerisque metus sed faucibus volutpat. In hac habitasse platea dictumst. Morbi eros ligula, lobortis sit amet turpis a, mollis placerat magna. Nulla laoreet pharetra urna, quis auctor purus ultricies quis. Quisque non tincidunt arcu. Duis scelerisque magna tortor, nec dapibus felis luctus eu. Mauris iaculis vitae nisl sit amet iaculis. Donec ultrices et purus et placerat. Maecenas nisi urna, aliquam in elementum quis, varius quis lorem. Donec vitae nisi id mi facilisis posuere."""
            println "Original size: " + str.size()
            def bytes = str.bytes
        expect:
            def zipbytes = Zip.gzip(bytes)
            println "Compressed size: " + zipbytes.size()
            def zipped = zipbytes.encodeBase64().toString()
            assert zipped
            println zipped
            assert str.size() > zipbytes.size()
        when:
            def unzipped = Zip.gunzip(zipped.decodeBase64())
        then:
            assert bytes == unzipped
    }

    def "unzip should correctly decompress data"() {
        given:
            Map<String, byte[]> expected = [
                "file1.txt": "This is the first file".getBytes(),
                "file2.txt": "This is the second file".getBytes()
            ]
            OutputStream outputStream = Zip.zip(expected)
            ByteArrayOutputStream baos = (ByteArrayOutputStream) outputStream
            InputStream inputStream = new ByteArrayInputStream(baos.toByteArray())

        when:
            Map<String, byte[]> namesData = Zip.unzip(inputStream)

        then:
            namesData == expected
    }

    void "decompressZip method should unzip a file correctly"() {
        given:
            // Create a temporary zip file with a single file
            File zipFile = File.createTempFile("test", ".zip")
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))
            zipOutputStream.putNextEntry(new ZipEntry("test.txt"))
            zipOutputStream.write("This is a test".getBytes())
            zipOutputStream.closeEntry()
            zipOutputStream.close()

            // Create a temporary directory to extract the zip file to
            File destDir = File.createTempDir()

        when:
            File result = Zip.decompressZip(zipFile, destDir)

        then:
            result == destDir
            new File(result, "test.txt").text == "This is a test"

        cleanup:
            if(zipFile.exists()) {
                zipFile.delete()
            }
            if(destDir.exists()) {
                destDir.deleteDir()
            }
    }

    def "test compressDir creates a zip file from a directory"() {
        given:
            File srcDir = File.createTempDir()
            File file1 = new File(srcDir, "file1.txt")
            file1.text = "file1 content"
            File subDir = new File(srcDir, "subdir")
            subDir.mkdirs()
            File file2 = new File(subDir, "file2.txt")
            file2.text = "file2 content"
            File zipFile = File.createTempFile("test","zip")

        when:
            File result = Zip.compressDir(srcDir, zipFile)

        then:
            result == zipFile
            result.exists()
            result.length() > 0

        cleanup:
            if(srcDir.exists()) {
                srcDir.deleteDir()
            }
            if(zipFile.exists()) {
                zipFile.delete()
            }
    }

    def "test gunzip method"() {
        given:
            File file = File.createTempFile("test", ".txt")
            file.text = "Hello"
            assert Zip.gzip(file)
            File zipped = new File(file.absolutePath + ".gz")
            assert zipped.exists()
        when:
            boolean result = Zip.gunzip(zipped)
        then:
            assert result : "Unable to gunzip"
            assert file.exists()
        cleanup:
            if(file.exists()) {
                file.delete()
            }
    }

    def "test gunzip method with non-gz file"() {
        given:
            File file = File.createTempFile("test", ".txt")
        when:
            Zip.gunzip(file)
        then:
            thrown Zip.InvalidExtensionException
        cleanup:
            file.delete()
    }

    def "test gunzip method with non-existing file"() {
        given:
            File file = new File("non_existing.txt")
        when:
            Zip.gunzip(file)
        then:
            thrown FileNotFoundException
    }
}