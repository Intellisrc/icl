package jp.sharelock.net

import jp.sharelock.etc.Log

import java.nio.charset.Charset

@groovy.transform.CompileStatic
/**
 * TCP Client
 */
class TCPClient extends Thread {
	interface ClientCallback {
		void call(Response response)
	}
    interface Parser {
        Object call(String msg)
    }
    protected InetAddress dstAddress
    protected int dstPort
    protected Request request
	protected ClientCallback callback
	int timeout = 20000

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
	void sendRequest(Request request, ClientCallback callback) {
		this.request = request
		this.callback = callback
		if(isAlive()) {
			run()
		} else {
			start()
		}
	}
	@Override
    void run() {
		Response response = getResponse()
		if(callback) {
			callback.call(response)
		}
    }
	/**
	 * Sends a Message Synchronously
	 * @param request
	 * @return 
	 */
	Response sendRequest(Request request) {
		this.request = request
		return getResponse()
	}

	Response getResponse(){
        Socket s
		Response response
        if(this.dstAddress != null) {
            // Create the socket connection to the MultiThreadedSocketServer port
            try {
                s = new Socket()
				Log.v("Connecting to: "+dstAddress.getHostAddress()+":"+dstPort)
				s.connect(new InetSocketAddress(dstAddress, dstPort), timeout)
				Log.v("Connected. ["+dstAddress.getHostAddress()+"]")
            } catch (UnknownHostException uhe) {
                // Server Host unreachable
                Log.e( "Unknown Host :" + dstAddress.getHostAddress(), uhe)
                s = null
            } catch (IOException ioe) {
                // Cannot connect to port on given server host
                Log.e( "Cant connect to server: " + dstAddress.getHostAddress() + ":" + dstPort + ". Make sure it is running.", ioe)
                s = null
            }

            if (s == null) {
				response = new Response(TCPStatus.ERROR, "", request)
				return response
			}

            BufferedReader dataIn
            PrintWriter dataOut

            try {
                // Create the streams to send and receive information
				dataIn = new BufferedReader(new InputStreamReader(s.getInputStream(),Charset.forName("UTF8")))
                dataOut = new PrintWriter(new OutputStreamWriter(s.getOutputStream(),Charset.forName("UTF8")))

                // Since this is the client, we will initiate the talking.
                // Send a string data and flush
				Log.i( "Message to Server [" + dstAddress.getHostAddress() + "] >>>>> " + request.getMessage())
                dataOut.println(request.getMessage())
                dataOut.flush()
				Log.v("Waiting for response... [" + dstAddress.getHostAddress() + "]")
                // Receive the reply.
				String s_resp = dataIn.readLine()
				Log.i( "Message from Server [" + dstAddress.getHostAddress() + "] <<<<< " + s_resp)
				if(s_resp == null) {
					Log.e( "Response was NULL. TCPStatus is unknown")
					response = new Response(TCPStatus.NO_RESPONSE, "", request)
				} else {
					response = new Response(TCPStatus.SENT, s_resp, request)
				}
                // For subsequent commands, use: dataOut.printl(...); dataOut.flush()
                try {
                    // Close the input and output streams
                    dataOut.close()
					dataIn.close()
                    // Close the socket before quitting
                    s.close()
					Log.v( "Connection closed successfully [" + dstAddress.getHostAddress() + "]")
                } catch (IOException e) {
                    Log.e( "Unable to close communication: ", e)
                }
            } catch (IOException ioe) {
                Log.e( "Exception during communication. Server probably closed connection: ",ioe)
				response = new Response(TCPStatus.NO_CONN, "", request)
            }
        } else {
            Log.e( "Missing remote Host and port")
			response = new Response(TCPStatus.ERROR, "", request)
        }
		return response
	}

	/**
	 * Request to process
     *
	 */
	static class Request {
		private final String message
		private final Object[] params
		
		Request(String message) {
			this.message = message
			this.params = null
		}
		Request(String message, Object... params) {
			this.message = message
			this.params = params
		}
		///////////////// GET //////////////////////
		String getMessage() {
			return this.message
		}
		Object getParam(int i) {
			Object o = null
			if(this.params != null) {
				if(this.params.length - 1 >= i) {
					o = this.params[i]
				}
			}
			return o
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
