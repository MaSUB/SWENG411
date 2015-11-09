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
public final class Server {

    public static void main(String argv[]) throws Exception {
	
        //  Port number that is being listend to for requests.
        int port = 554;
        ServerSocket socket;
        Socket client;
        
        
        //  Trys to set the socket on the server
        //  if the socket cant be started, it will throw an IO exception.
        //  
        client = new Socket();
        socket = new ServerSocket(port);
        System.out.println("The server is listening on socket #:\t" + port + "\n");
        
       
        
        while(true){
            
            client = socket.accept();
            
            if(socket.isBound()) {
                
                RTSPRequest request = new RTSPRequest(client);
                Thread thread = new Thread(request);
                thread.start();
                
            }
            
        }
        
        
    }

}
