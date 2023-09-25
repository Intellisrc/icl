//file:noinspection GrFinalVariableAccess
package com.intellisrc.net

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.*
import org.apache.commons.net.util.TrustManagerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Files

/**
 * FTP client class (wrapper around apache commons FTPClient)
 * @since 2020/10/20.
 */
@CompileStatic
class FtpClient {
    static boolean active = Config.getBool("ftp.active") //By default will be "passive"
    final String hostname
    final InetAddress ip
    final int port
    final String user
    final String pass
    String path
    String cwd = "/"
    final boolean secure
    boolean verifyHost = false // Only if encrypted is true, will check certificate against host name
    protected final FTPClient client
    /**
     * Constructor
     * @param ip    : Server IP address
     * @param port  : Server port
     * @param user  : username
     * @param pass  : password
     * @param path  : path to change upon connection
     * @param secure : Use FTPS
     * @param hostToVerify : Hostname to verify (it is not needed if encrypted is false)
     */
    FtpClient(InetAddress ip, int port = 21, String user, String pass, String path, boolean secure = false, String hostToVerify = "") {
        this.ip = ip
        this.port = port
        this.user = user
        this.pass = pass
        this.path = path.replaceAll(/\/$/, '') // Remove trailing slash if present
        this.secure = secure
        this.hostname = hostToVerify
        this.verifyHost = ! hostToVerify.empty
        client = secure ? new FTPSClient() : new FTPClient()
    }
    /**
     * Constructor using hostname instead of IP
     * @param hostname  : Server hostname
     * ...
     * @param verifyHost : Verify hostname (boolean) against certificate
     */
    FtpClient(String hostname, int port = 21, String user, String pass, String path, boolean secure = false, boolean verifyHost = false) {
        this(InetAddress.getByName(hostname), port, user, pass, path, secure, verifyHost ? hostname : "")
    }

    /**
     * Connect to FTP Server
     * @return
     */
    boolean connect() {
        boolean connected = false
        try {
            Log.i("Connecting to server : %s", ip.hostAddress)
            client.setConnectTimeout(Millis.SECOND_10)
            if(port) {
                Log.i("Setting port: %d", port)
                client.setDefaultPort(port)
            }
            if(secure) {
                FTPSClient ftps = (client as FTPSClient)
                Log.v("Enabled cipher-suites: ")
                ftps.enabledCipherSuites.each {
                    Log.v("cipher-suite: %s", it)
                }
                Log.v("Enabled protocols: ")
                ftps.enabledProtocols.each {
                    Log.v("protocol: %s", it)
                }
                ftps.endpointCheckingEnabled = verifyHost
                ftps.trustManager = verifyHost ? TrustManagerUtils.validateServerCertificateTrustManager : TrustManagerUtils.acceptAllTrustManager
                Log.i("Connecting using secure socket (host verification: %s)", verifyHost ? "Yes [${hostname}]".toString() : "No")
            } else {
                Log.i("Using non-encrypted communication")
            }
            client.connect(ip)
            if (FTPReply.isPositiveCompletion(client.replyCode)) {
                Log.i("Logging in...")
                boolean login = client.login(user, pass)
                if (login) {
                    connected = true
                    if (active) {
                        client.enterLocalActiveMode()
                    } else {
                        client.enterLocalPassiveMode()
                    }
                    Log.i("Connection was successful : %s", active ? "ACTIVE" : "PASSIVE")
                    cd(path)
                } else {
                    Log.w("Unable to login to server [ %s ]", ip.hostAddress)
                }
            } else {
                Log.w("Unable to connect to server [ %s ] : Received error code: %d", ip.hostAddress, client.replyCode)
            }
        } catch(FTPConnectionClosedException e) {
            Log.w("Server [ %s ] closed connection : ", ip.hostAddress, e)
        } catch(IOException e) {
            Log.w("Unable to connect to server [ %s ] : ", ip.hostAddress, e)
        }
        return connected
    }
    /**
     * Change current working directory
     * @param directory
     */
    boolean cd(String directory) {
        boolean changed = false
        // Convert relative to absolute:
        String fullPath = directory
        if(! directory.startsWith("/")) {
            fullPath = cwd.endsWith("/") ? cwd + directory : cwd + "/" + directory
        }
        if(client.changeWorkingDirectory(fullPath)) {
            cwd = fullPath
            Log.v("Directory is now: %s", cwd)
            changed = true
        } else {
            Log.w("Unable to change directory to: %s", directory)
        }
        return changed
    }

    /**
     * Go to parent directory
     */
    void cdToParent() {
        client.changeToParentDirectory()
    }

    /**
     * Get files at path
     * @param filePath
     * @return
     */
    List<String> listFiles() {
        if(!client.connected) { connect() } //Reconnect
        Log.v("Listing files in %s", cwd)
        List<FTPFile> files = client.listFiles(cwd).toList()
        Log.v("Found: %d files", files.size())
        return files.collect { it.name }
    }
    /**
     * Get a File from FTP
     * @return
     */
    File getFile(String file) {
        File out = null
        try {
            if (!client.connected) {
                connect()
            } //Reconnect
            if (client.connected) {
                client.setFileType(FTP.BINARY_FILE_TYPE)
                if (active) {
                    client.enterLocalActiveMode()
                } else {
                    client.enterLocalPassiveMode()
                }
                out = Files.createTempFile("ftp-", file).toFile()
                OutputStream output = new FileOutputStream(out.absolutePath)
                if (!client.retrieveFile(file, output)) {
                    Log.w("Unable to retrieve file: %s", file)
                }
                output.close()
            } else {
                Log.w("Unable to reconnect")
            }
        } catch(Exception e) {
            Log.e("Error while trying to retrieve file: ", e)
        }
        return out
    }

    /**
     * Get an Image from FTP
     * @param filePath
     * @return
     */
    BufferedImage getImage(String filePath) {
        BufferedImage image = null
        try {
            if (!client.connected) {
                connect()
            } //Reconnect
            if (client.connected) {
                client.setFileType(FTP.BINARY_FILE_TYPE)
                if (active) {
                    client.enterLocalActiveMode()
                } else {
                    client.enterLocalPassiveMode()
                }
                InputStream input = client.retrieveFileStream(filePath)
                image = ImageIO.read(input)
                client.completePendingCommand()
                input.close()
            } else {
                Log.w("Unable to reconnect")
            }
        } catch(Exception e) {
            Log.e("Error while trying to retrieve image: ", e)
        }
        return image
    }

    /**
     * Upload a file
     * @param file
     * @return
     */
    boolean uploadFile(final File file) {
        boolean uploaded = false
        try {
            InputStream input = new FileInputStream(file)
            client.storeFile(file.name, input)
            uploaded = true
        } catch(Exception e) {
            Log.w("Unable to upload file [%s] : %s", file.name, e)
        }
        return uploaded
    }

    /**
     * Delete a file in remote server
     * @param path
     * @return
     */
    boolean delete(String path) {
        return client.deleteFile(path)
    }

    /**
     * Noop
     */
    void noop() {
        try {
            if (client.connected) {
                client.noop() //Check that the connection is fine
            }
        } catch(Exception ignore) {}
    }

    /**
     * Abort transfer
     * @return
     */
    boolean abort() {
        client.abort()
    }

    /**
     * Disconnects
     */
    void disconnect() {
        Log.i("Disconnecting...")
        try {
            client.logout()
            if (client.connected) {
                client.disconnect()
            }
        } catch(Exception ignore) {}
    }
}
