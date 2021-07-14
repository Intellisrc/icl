package com.intellisrc.net

import com.inspeedia.common.core.Config
import com.inspeedia.common.core.Log
import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.*

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
    Inet4Address ip
    int port
    String user
    String pass
    String path
    String cwd = "/"
    protected final FTPClient client

    FtpClient(Inet4Address ip, int port = 21, String user, String pass, String path) {
        this.ip = ip
        this.port = port
        this.user = user
        this.pass = pass
        this.path = path.replaceAll(/\/$/, '') // Remove trailing slash if present
        client = new FTPClient()
    }
    /**
     * Connect to FTP Server
     * @return
     */
    boolean connect() {
        boolean connected = false
        try {
            Log.i("Connecting to server : %s", ip.hostAddress)
            client.setConnectTimeout(10 * 1000)
            if(port) {
                Log.i("Setting port: %d", port)
                client.setDefaultPort(port)
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
            Log.w("Server [ %s ] closed connection : ", ip.hostAddress, e.message)
        } catch(IOException e) {
            Log.w("Unable to connect to server [ %s ] : ", ip.hostAddress, e.message)
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
