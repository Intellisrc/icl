package com.intellisrc.net

import com.intellisrc.core.Log

/**
 * @since 12/29/17.
 */
class UDPClient {
    UDPClient(Inet4Address host, int port) {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in))
        DatagramSocket clientSocket = new DatagramSocket()
        byte[] receiveData = new byte[1024]
        String sentence = inFromUser.readLine()
        byte[] sendData = sentence.bytes
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port)
        clientSocket.send(sendPacket)
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length)
        clientSocket.receive(receivePacket)
        String modifiedSentence = new String(receivePacket.data)
        Log.v("FROM SERVER:" + modifiedSentence)
        clientSocket.close()
    }
}
