package jp.sharelock.net

import jp.sharelock.etc.Log

import java.nio.charset.Charset

@groovy.transform.CompileStatic
/**
 * TCP Server
 * It is abstract because execCommand must be implemented
 */
class TCPServer {
	interface ServerCallback {
        String exec(String clientCommand)
    }

    /**
     * If port can not be used...
     */
    static class InvalidPortException extends Exception {
        InvalidPortException(String e) {
            super(e)
        }
    }
    /**
     * Set this flag to true to exit the server
     */
	private boolean exit = false
	/**
	 * Creates a new TCPServer with port and a return callback to process String response
	 * @param port
	 * @param callback
	 */
	TCPServer(int port, ServerCallback callback) throws InvalidPortException {
		Thread.start {
			ServerSocket serverSocket
            try {
                serverSocket = new ServerSocket(port)
                exit = true
            } catch (Exception e) {
                Log.e( "Could not create server socket on port: $port ",e)
                throw new InvalidPortException("Could not create server socket on port: $port ")
            }
            while(exit) {
                try
                {
                    // Accept incoming connections.
                    Socket clientSocket = serverSocket.accept()

                    // accept() will block until a client connects to the server.
                    // If execution reaches this point, then it means that a client
                    // socket has been accepted.

                    // For each client, we will start a service thread to
                    // service the client requests. This is to demonstrate a
                    // Multi-Threaded server. Starting a thread also lets our
                    // MultiThreadedSocketServer accept multiple connections simultaneously.

                    // Start a Service thread
                    ClientServiceThread cliThread = new ClientServiceThread(clientSocket, callback)
                    cliThread.start()
                }
                catch(IOException ioe)
                {
                    Log.w( "Exception encountered on accept. Ignoring. ($ioe)")
                }

            }

            try
            {
                serverSocket.close()
                Log.d( "Server Stopped")
            }
            catch(IOException ioe)
            {
                Log.e( "Problem stopping server socket", ioe)
                System.exit(-1)
            }
		}
	}

	void quit() {
		exit = false
		Log.i( "Server will stop listening to new requests")
	}

	/**
	 * For each client, this method will be started
	 */
	class ClientServiceThread extends Thread {
		private Socket clientSocket
		private final ServerCallback execCommand
		/**
		 * Execute some command on the server side and return result
		 * if "exit" is returned, it will quit the server
		 * @param command
		 */

		ClientServiceThread(final Socket s, final ServerCallback callback) {
			clientSocket = s
			execCommand = callback
		}

		@Override
		void run()
		{
			// Obtain the input stream and the output stream for the socket
			// A good practice is to encapsulate them with a BufferedReader
			// and a PrintWriter as shown below.
            BufferedReader dataIn = new BufferedReader(new InputStreamReader(clientSocket.inputStream, Charset.forName("UTF8")))
            PrintWriter dataOut = new PrintWriter(new OutputStreamWriter(clientSocket.outputStream, Charset.forName("UTF8")))

			// Print out details of this connection
			//Log.d( "Accepted Client Address - " + myClientSocket.getInetAddress().getHostName()); <-- this line caused 5sec delay in Windows
			Log.d( "Accepted Client")

			try
			{
                // read incoming stream
				String clientCommand
                String reply
                while(clientCommand = dataIn.readLine()) {
					Log.i("Message from client <<<<< [" + clientCommand + "]")
					reply = execCommand.exec(clientCommand)
                    if(!reply.isEmpty()) {
                        Log.i( "Message to client >>>>> [" + reply +"]")
                        dataOut.println(reply+"\n")
						dataOut.flush()
                    }
                }
            }
			catch(IOException e)
			{
				Log.e("Error in TCP connection",e)
			}
            // Clean up
            try
            {
				if(dataIn != null) {
					dataIn.close()
				}
				if(dataOut != null) {
					dataOut.close()
				}
				clientSocket.close()
				Log.d( "... Thread ended")
			}
            catch(IOException ioe)
            {
				Log.e("Error while closing connection", ioe)
            }
		}
	}

}
