package com.intellisrc.net

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

/**
 * @since 12/29/17.
 */
@CompileStatic
class UDPServer {
    static interface ServerCallback {
        String exec(String clientCommand)
    }
    /**
     * Set this flag to true to exit the server
     */
    private boolean exit = false
    /**
     * Constructor and launch server
     * @param port
     * @param callback
     */
    UDPServer(int port, ServerCallback callback) {
        DatagramSocket serverSocket = new DatagramSocket(port)
        byte[] receiveData = new byte[1024]
        while(exit) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length)
            serverSocket.receive(receivePacket)
            String sentence = new String( receivePacket.data )
            Log.v("RECEIVED: " + sentence)
            InetAddress IPAddress = receivePacket.address
            int remotePort = receivePacket.port
            String capitalizedSentence = sentence.toUpperCase()
            byte[] sendData = capitalizedSentence.bytes
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remotePort)
            serverSocket.send(sendPacket)
        }
    }
}
