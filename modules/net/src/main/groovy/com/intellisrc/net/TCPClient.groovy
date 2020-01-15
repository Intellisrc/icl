package com.intellisrc.net

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.nio.charset.Charset

/**
 * TCP Client
 */
@CompileStatic
class TCPClient {
	interface ClientCallback {
		void call(Response response)
	}
    interface Parser {
        Object call(String msg)
    }
    protected InetAddress dstAddress
    protected int dstPort
	protected final synchronized Queue<Request> requestList = [] as Queue<Request>
    public int timeout = 20000

	static enum TCPStatus {
		SENT, NO_CONN, ERROR, NO_RESPONSE
	}

    /**
     * Constructor.
     */
    TCPClient() {}

	/**
	 * Constructor: Connect to localhost at specified port
	 * @param dstPort
	 */
	TCPClient(int dstPort) {
		this.dstAddress = "127.0.0.1".toInet4Address()
		this.dstPort = dstPort
	}

    /**
     * Constructor: Initial settings
     * @param dstAddress
     * @param dstPort
     */
    TCPClient(String dstAddress, int dstPort) {
		this.dstAddress = dstAddress.toInet4Address()
        this.dstPort = dstPort
    }
	
    /**
     * Constructor: Initial settings
     * @param dstAddress
     * @param dstPort
     */
    TCPClient(InetAddress dstAddress, int dstPort) {
        this.dstAddress = dstAddress
        this.dstPort = dstPort
    }

    /**
     * Set Remote Host and Port
     * @param dstAddress : Host IP address to connect to
     * @param dstPort : Port in which the host is listening
     */
    void setRecipient(String dstAddress, int dstPort) {
		this.dstAddress = dstAddress.toInet4Address()
        this.dstPort = dstPort
    }
    void setRecipient(InetAddress dstAddress, int dstPort) {
		this.dstAddress = dstAddress
        this.dstPort = dstPort
    }
	/**
	 * Sends a Message Asynchronously
	 * @param request
	 * @param callback
	 */
	void sendRequest(Request request) {
		requestList << request
		send()
	}
    /**
     * Sends multiple Messages
     * @param request
     * @param callback
     */
    void sendRequest(List<Request> requests) {
        requests.each {
            requestList << it
        }
        send()
    }

	/**
	 * Add a request. Requires to execute send()
	 * @param request
	 * @param callback
	 */
	void addRequest(Request request) {
		requestList << request
	}
    /**
     * Send the request(s)
     */
    void send() {
		Thread.start {
            getResponse()
		}
    }

	void getResponse(){
        Socket s
		Response response
        if(this.dstAddress != null) {
            // Create the socket connection to the MultiThreadedSocketServer port
            try {
                s = new Socket()
				Log.v("Connecting to: "+dstAddress.hostAddress+":"+dstPort)
				s.connect(new InetSocketAddress(dstAddress, dstPort), timeout)
				Log.i("Connected. ["+dstAddress.hostAddress+"]")
            } catch (UnknownHostException uhe) {
                // Server Host unreachable
                Log.e( "Unknown Host :" + dstAddress.hostAddress, uhe)
                s = null
            } catch (IOException ioe) {
                // Cannot connect to port on given server host
                Log.e( "Cant connect to server: " + dstAddress.hostAddress + ":" + dstPort + ". Make sure it is running.", ioe)
                s = null
            }

            if (s != null) {
				BufferedReader dataIn
				PrintWriter dataOut

				try {
					// Create the streams to send and receive information
					dataIn = new BufferedReader(new InputStreamReader(s.getInputStream(), Charset.forName("UTF8")))
					dataOut = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), Charset.forName("UTF8")))
					Request request
					while(request = requestList.poll()) {
                        // Since this is the client, we will initiate the talking.
                        // Send a string data and flush
                        Log.i("Message to Server [" + dstAddress.hostName + "] >>>>> " + request.message)
                        dataOut.println(request.message)
                        dataOut.flush()
                        Log.v("Waiting for response... [" + dstAddress.hostName + "]")
                        // Receive the reply.
                        String sResp
                        while(sResp = dataIn.readLine()) {
                            Log.i("Message from Server [" + dstAddress.hostName + "] <<<<< " + sResp)
                            if (sResp) {
                                response = new Response(TCPStatus.SENT, sResp, request)
                            } else {
                                Log.e("Response was NULL. TCPStatus is unknown")
                                response = new Response(TCPStatus.NO_RESPONSE, "", request)
                            }
                            try {
                                request.onResponse.call(response)
                            } catch (IOException e) {
                                Log.e("There was an error while processing the response", e)
                            }
                        }
					}
                    // Clean up
                    try
                    {
                        // Close the input and output streams
                        if(dataIn != null) {
                            dataIn.close()
                        }
                        if(dataOut != null) {
                            dataOut.close()
                        }
                        s.close()
                        Log.v("Connection closed successfully [" + dstAddress.hostAddress + "]")
                    }
                    catch(IOException ioe)
                    {
                        Log.e("Error while closing connection", ioe)
                    }
				} catch (IOException ioe) {
					Log.e("Exception during communication. Server probably closed connection: ", ioe)
				}
			} else {
				Log.e("Socket was empty")
			}
        } else {
            Log.e( "Missing remote Host and port")
        }
	}

	/**
	 * Request to process
     *
	 */
	static class Request {
		private final String message
        private final ClientCallback onResponse

		Request(String message, ClientCallback callback = {}) {
			this.message = message
            this.onResponse = callback
		}
		///////////////// GET //////////////////////
		String getMessage() {
			return this.message
		}
	}

	/**
	 * Response Class
	 * result : after processing message, this object may be created
	 */
	static class Response {
		private final String message
		final TCPStatus status
		final Request request

		Response(TCPStatus status, String message, Request request) {
			this.message = message
			this.request = request
			this.status = status
		}
		
		/**
		 * Verifies that the message was sent correctly
		 * @return 
		 */
		boolean isSent() {
			return this.status == TCPStatus.SENT
		}

		@Override
		String toString() {
			return this.message
		}

		Object parse(Parser parser) {
			return parser(this.message)
		}
	}
}
