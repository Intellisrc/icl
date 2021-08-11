package com.intellisrc.net

import groovy.transform.CompileStatic

/**
 * @since 12/29/17.
 */
@CompileStatic
class UDPClient {
    final Inet4Address host
    final int port
    protected DatagramSocket clientSocket
    interface Response {
        void call(String response)
    }

    /**
     * Initialize client
     * @param host
     * @param port
     */
    UDPClient(Inet4Address host, int port) {
        this.host = host
        this.port = port
        clientSocket = new DatagramSocket()
    }
    /**
     * Send request
     * @param message
     * @param response
     * @param packetSize
     */
    void send(String message, Response response, int packetSize = 1024) {
        byte[] sendData = message.bytes
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port)
        clientSocket.send(sendPacket)
        /*
         * Wait for server to respond
         */
        Thread.start {
            byte[] receiveData = new byte[packetSize]
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length)
            clientSocket.receive(receivePacket)
            response.call(new String(receivePacket.data))
        }
    }
    /**
     * close client
     */
    void quit() {
        clientSocket.close()
    }

}
