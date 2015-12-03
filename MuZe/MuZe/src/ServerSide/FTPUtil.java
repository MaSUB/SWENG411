package ServerSide;

import Exceptions.*;
import java.io.IOException;
import java.io.InputStream;
 
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

///////////////////////////////////////////////////////////////////////////////
//
//  Author:         Mason Toy
//  Description:    
//      This class is used to connect the client to the FTP server. This class
//      handles, uploading, downloading, logging in to the FTP server,
//      connecting to the FTP server and correctly disconnecting as well.
//      
///////////////////////////////////////////////////////////////////////////////
public class FTPUtil {
 
    //  These attributes are used to connect the client to the server.
    //  host represents the host Server's IP/Web address, the portNum is the
    //  port number, username and password are used to log into the server.
    //  
    private String host;
    private int portNum;
    private String username;
    private String password;
 
    
    //  The FTPClient class is used from the apache.commons.net libray's this
    //  makes connecting to a server and creating a client FTP very easy to 
    //  understand and straight forward.
    //
    private FTPClient client = new FTPClient();
    private int reply;
    private InputStream inStream;
 
    
    //  When creating a new instance of the FTPUtil, it needs to be passed the
    //  host ip/webaddress, port#, username and password. This is needed to
    //  connect to the server.
    //  
    public FTPUtil(String h, int p, String u, String pass) {
        
        this.host = h;
        this.portNum = p;
        this.username = u;
        this.password = pass;
        
    }
 
    //  connect:
    //      This method is used to connect the client/host machine to the FTP
    //      Server. The host, portNum, username, and password are needed in
    //      order for this to function without exception.
    //  
    public void connect() throws FTPException {
        
        try {
            
            client.connect(host, portNum);
            reply = client.getReplyCode();
            
            if (!FTPReply.isPositiveCompletion(reply)) {
                
                throw new FTPException("serve couldn't connect.");
                
            }
 
            boolean logIn;
            logIn = client.login(username, password);
            
            if (!logIn) {
                
                client.disconnect();
                throw new FTPException("Unable to login.");
                
            }
 
            client.enterLocalPassiveMode();
 
        } catch (IOException ex) {
            
            throw new FTPException("Error during IO: " + ex.getMessage());
            
        }
    }
 
    
    //
    //
    //
    //
    public long getFileSize(String filePath) throws FTPException {
        
        try {
            
            FTPFile file;
            file = client.mlistFile(filePath);
            
            if (file == null) {
                
                throw new FTPException("404 file not found.");
                
            }
            return file.getSize();
        } catch (IOException ex) {
            throw new FTPException("Unable to calculate file size: " 
                    + ex.getMessage());
        }
    }
 
    /**
     * Start downloading a file from the server
     *
     * @param downloadPath
     *            Full path of the file on the server
     * @throws FTPException
     *             if client-server communication error occurred
     */
    public void downloadFile(String downloadPath) throws FTPException {
        try {
 
            boolean success = client.setFileType(FTP.BINARY_FILE_TYPE);
            if (!success) {
                throw new FTPException("Could not set binary file type.");
            }
 
            inStream = client.retrieveFileStream(downloadPath);
 
            if (inStream == null) {
                
                throw new FTPException("Unable to open inStream. File may not "
                                        + "be available.");
                
            }
            
        } catch (IOException ex) {
            
            throw new FTPException("Error downloading file: " + ex.getMessage());
            
        }
    }
 
    //  
    //  
    //  
    //
    public void finish() throws IOException {
        inStream.close();
        client.completePendingCommand();
    }
 
    /**
     * Log out and disconnect from the server
     */
    public void disconnect() throws FTPException {
        if (client.isConnected()) {
            try {
                if (!client.logout()) {
                    throw new FTPException("Could not log out from the server");
                }
                client.disconnect();
            } catch (IOException ex) {
                throw new FTPException("Error disconnect from the server: "
                        + ex.getMessage());
            }
        }
    }
    
    //  Simple getter for the InputStream.
    //
    public InputStream getInStream() {
        
        return inStream;
        
    }
}

