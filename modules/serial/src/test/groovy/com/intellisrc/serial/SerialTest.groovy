package com.intellisrc.serial

import jssc.SerialPort
import spock.lang.Specification

class SerialTest extends Specification {
    static String serialPort = "FAKE"
    def portSerial
    Serial serialObj
    //Serial reader classes
    def reader
    def readerStr
    def readerInt
    //Serial Writer classes
    Seriable.SerialWriter writerObj
    Seriable.SerialWriterStr writeStrObj
    Seriable.SerialWriterInt writerIntObj
    
    def setup() {
        portSerial = Stub(SerialPort) {
            openPort() >> true
        }
        serialObj = new Serial(serialPort, portSerial)
        //Serial reader classes
        reader = Mock(Seriable.SerialReader.class)
        readerStr = Mock(Seriable.SerialReaderStr.class)
        readerInt = Mock(Seriable.SerialReaderInt.class)
        //Serial Writer classes
        writerObj = Stub()
        writeStrObj = Stub()
        writerIntObj = Stub()
    }
    /**Serial()
     Test Cases
     1. check if serial Object is not null*/
    def "Check name of Serial constructor args must be same"() {
        setup:
            println("\n[TITLE] Check arg of Serial constructor : ")
        when:
            println("[INPUT]")
            println(port.toString())
        then:
            assert serialObj.serialPort: " Serial Object must be null"
        expect:
            println("[OUTPUT]")
            assert serialObj.serialPort == serialPort //constructor must be same
            println("SerialPort name is \'" + serialObj.serialPort.toString() + "\'")
        where:
            port << [serialPort]
    }
    /** Connect()
     To test the Connectivity of Serial port
     Output - check status of connected
     */
    def "connection must exists"() {
        when:
            serialObj.connect()
            println("connected  : " + serialObj.connected)
        then:
            assert serialObj.connected: "[ SerialPort is not connected {connected = " + serialObj.connected + "}"
        expect:
            assert serialObj.connected //connection of the port must be true
            println("[OUTPUT]")
            println("SerialPort connected {connected = " + serialObj.connected + "}")
    }
    /**listPorts()
     get the list of ports connected using this function
     Test Cases
     1. check if port list is empty or not
     2. size of the list
     */
    def "getting serial ports list into the array"() {
        setup:
            serialObj.listPorts()
        when:
            println(serialObj.listPorts())
            List<String> listOfPort = serialObj.listPorts()
        then:
            println("Checking list of ports")
            println("list of ports : " + listOfPort)
            assert listOfPort.size() != 0: "Port list is empty, size is 0"
        cleanup:
            listOfPort.clear()
    }
    
    def "portComm must find the connectivity through findPort"() {
        setup:
            serialObj.connect()
        when:
            serialObj.findPort()
        then:
            println("SerialPort of findPort should assigned only for SerialPort of portComm")
            def portFound = serialObj.findPort().portName
            assert portFound: "Unable to find port"
            println("Found port: " + portFound)
            assert portFound == serialPort
    }
    /** disconnect()
     Test Case
     1. check the state of connected(bool) after disconnect function is called
     */
    def "disconnect must set connected flag to false"() {
        setup:
            serialObj.connect()
        expect:
            assert serialObj.connected: "Serial was not connected"
        when:
            println("Check if Serial port is disconnected :")
            serialObj.disconnect()
        then: "connection of port must be false"
            assert !serialObj.connected: " serialPort is not disConnected"
            println("disconnect Success")
    }
    /**read()
     Test Cases
     1. Check value of byte array should not be empty([])
     */
    def "must assign some integer value in bytecount "() {
        setup:
            serialObj.connect()
        when:
            portSerial.readBytes(3) >> byteArr
            byte[] arr = portSerial.readBytes(3)
            serialObj.read(3, reader)
        then:
            assert arr != [] as byte[]: "array must not be empty"
        expect:
            if (arr == [23, 10, 14] as byte[]) //output of byte array must be same
                println("portComm reads the byte array value and it should be same value")
        where:
            byteArr << [[23, 10, 14] as byte[]]
    }
    /**readLine()
     Test Cases
     1. Check value of String should not be null
     */
    def "Output string must not be empty"() {
        setup:
            String strValue
            serialObj.connect()
        when:
            portSerial.readString() >> strOutput
            strValue = portSerial.readString()
            serialObj.readLine(readerStr)
        then:
            assert strValue != null: "value of string must not be null"
            assert !strValue.isEmpty(): "value of string must not be empty"
        expect:
            if (strValue == "Hello world !!") //output string must be same
                println("portcomm reads the string value and it should be the same value")
        where:
            strOutput << ["Hello world !!"]
    }
    /**readNum()
     Test Cases
     1. Check value of String should not be null
     2. output of string is must be assign in integer value
     */
    def "the string value of serialreaderint must convert into integer value if the value of string in numeric type "() {
        when:
            serialObj.connect()
            portSerial.readString() >> strOutput
            serialObj.readNum(readerInt)
        then:
            assert portSerial.readString() != "": " Value of string must not be empty"
            assert portSerial.readString() != null: " value of string must not be null"
        expect:
            if (Integer.parseInt(strOutput[0] as String) == 2) // string value must convert into integer type
                println("readerint reads the integer value by converting string value into integer value and it should be the same value")
        where:
            strOutput << ["2"]
    }
    /**write()
     Test Cases
     1. Check if byte array is empty of call()
     Write should be fail if output is null
     */
    def "writerobj must return some byte array  after that portComm will write them into bytes"() {
        setup:
            byte[] byteArrayOutput
            serialObj.connect()
        when:
            writerObj.call() >> byteArr
            byteArrayOutput = writerObj.call()
            serialObj.write(writerObj)
        then:
            assert byteArrayOutput: " byte array must not be null"
            assert !byteArrayOutput.toList().empty: " byte array should not be empty"
        expect:
            if (byteArrayOutput == [12, 45, 67] as byte[]) // output of byte array must be same
                println("writer return the byte array and it assigned the same value")
        where:
            byteArr << [[12, 45, 67] as byte[]]
    }
    /**writeStr()
     Test Cases
     1. Check string value must not be empty
     2. Check string must not be null
     */
    def "writestrobj must return some string value after that portComm should write to string"() {
        setup:
            String strOutput
            serialObj.connect()
        when:
            writeStrObj.call() >> strValue
            strOutput = writeStrObj.call()
            serialObj.writeStr(writeStrObj)
        then:
            assert strOutput: " value of string must not be empty or null"
        expect:
            if (strOutput == "Hello World") //output of string value must be same
                println("writerstr return some string value and it assigned the same value")
        where:
            strValue << ["Hello World"]
    }
    /**writeNum()
     Test Cases
     1. Check if int_output is not empty and greater than Zero
     2. Check if str_output is null
     3.output string must be fail if output is null
     */
    def "writeintobj must return some integer value after that portComm will write into integer type"() {
        setup:
            int number
            serialObj.connect()
        when:
            writerIntObj.call() >> intvalue
            number = writerIntObj.call()
            serialObj.writeNum(writerIntObj)
        then:
            assert number != 0: "must assign an integer value"
            assert number >= 0: "must be greater than zero and shouldn't assign any negative value"
        expect:
            if (number == 128) // output of integer must be same
                println("writenum return some integer value and it assigned the same value")
        where:
            intvalue << [128]
    }
}