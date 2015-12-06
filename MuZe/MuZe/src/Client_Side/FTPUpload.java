package Client_Side;

import Exceptions.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
 
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

///////////////////////////////////////////////////////////////////////////////
//
//  Author:         Mason Toy
//  Class name:     FTPUpload
//  Description:    
//      This class is used to handle the task of uploading a file.
//      
///////////////////////////////////////////////////////////////////////////////
public class FTPUpload extends SwingWorker<Void, Void> {
    
    private static final int    BUFFER_SIZE = 4096;
    
    private String host;
    private int port;
    private String username;
    private String password;
    
    private String uploadPath;
    
    DownloadGUI dlGui;
    FTPUtil ftp;
    
    FTPUpload(String h, int p, String u, String pass, String up,
                        DownloadGUI gui){
        
        host = h;
        port = p;
        username = u;
        password = pass;
        uploadPath = up;
        dlGui = gui;
        
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        
        ftp = new FTPUtil(host, port,username,password);
        
            try(InputStream input = new FileInputStream(new File(uploadPath))){
            this.ftp.getClient().storeFile("_", input);
        }
        
            
       return null;
       
    }
    
    
    
}
