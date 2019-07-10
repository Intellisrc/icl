package com.intellisrc.etc

import spock.lang.Specification
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
}