/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.* ;
import java.net.* ;
import java.util.* ;
/**
 *
 * @author Mason
 */
final class RTSPRequest implements Runnable {

    final static String CRLF = "\r\n";
    Socket socket;
    
    RTSPRequest(Socket socket) throws Exception {
        
        this.socket = socket;
        
    }
    
    @Override
    public void run() {
        
        try{
            
            processRequest();
            
        }
        catch(Exception e){
            
            System.out.println(e);
            
        }
        
        
    }
    
    private void processRequest() throws Exception {
        
        InputStream inStream;
        DataOutputStream outStream;
        BufferedReader bread;
        
        inStream = new DataInputStream(socket.getInputStream());
        outStream = new DataOutputStream(socket.getOutputStream());
        
        
        bread = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        String requestLine = bread.readLine();
        
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();
        String fileName = tokens.nextToken();
        fileName = "." + fileName;
        
        FileInputStream fis = null;
        boolean fileExists = true;
        
        try{
          
            fis = new FileInputStream(fileName);
            
        } 
        catch(FileNotFoundException e) {
            
            fileExists = false;
            
        }
        
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        if (fileExists) {
            statusLine = "RTSP/1.0 200 OK"; 
            contentTypeLine = "Content-type: " +
            contentType(fileName) + CRLF;
        } else {
            statusLine = "RTSP/1.0 404\n";
            contentTypeLine = "Not Found\n";
            entityBody = "<HTML>\n" +
            "\t<HEAD>\n\t\t<TITLE>Not Found</TITLE>\n\t</HEAD>" +
            "\n\t<BODY>404 File Not Found</BODY>\n</HTML>";
        }


        outStream.writeBytes(statusLine);
        outStream.writeBytes(contentTypeLine);
        
        outStream.writeBytes(CRLF);
        String headerLine = null;
        
        if(fileExists){
            
            sendBytes(fis,outStream);
            fis.close();
            
        }
        
        else{
            
            outStream.writeBytes(entityBody);
            
        }
        while((headerLine = bread.readLine()).length() != 0) {
            
            System.out.println(headerLine);
            
        }
       
        
        //
        //  Close all of the streams
        //  then close the socket
        //
        inStream.close();
        outStream.close();
        bread.close();
        socket.close();
        
    }
    
    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;
        
        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".mp3")){
                
            return "audio/mpeg";
            
        }
        if(fileName.endsWith(".jpg")){
            
            return "image/jpeg";
                
        }
        
        return "application/octet-stream";
        
    }
    
}
