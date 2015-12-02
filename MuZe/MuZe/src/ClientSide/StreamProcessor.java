/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client_Side;


import RTSPtest.Mp3Player;
import RTSPtest.RTPpacket;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author devon
 */
public class StreamProcessor implements Runnable
{
    //thread variables
    Thread t;
    String threadname = "stream processor";
    //variables used for storing temporary files
    private final String Directory_Archive = "./temp";
    private final String Temp_mp3_archive = "music.mp3";
    

    //variables used for streaming
    private final DatagramSocket RTPsocket;                 //socket to receive files
    private DatagramPacket rcvdp;                           //datagram packet from UDP connection
    private final byte[] buf;                               //buffer of data used when receiving packets
    private FileOutputStream fileoutput;
    
    
    Mp3Player player;                                       //the actual object to play the mp3 files, uses external library
    private static long actualsize;                         //checks how much has been read so far
    public StreamProcessor(DatagramSocket s)
    {
        RTPsocket = s;
        buf = new byte[15000];                              //15kb is the buffer size
        actualsize = 0;
        
        try {
            fileoutput = new FileOutputStream(Directory_Archive + Temp_mp3_archive);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StreamProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    @Override
    public void run() 
    {
        try {
            Thread.sleep(200);                                                  //need to give server time to begin sending
        } catch (InterruptedException ex) {
            Logger.getLogger(StreamProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        do
        {
            try {
                rcvdp = new DatagramPacket(buf, buf.length);
                
                
                RTPsocket.receive(rcvdp);                                                   //receive the DP from the socket:
                
                
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());   //create an RTPpacket object from the DP
                
                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
                
                
                rtp_packet.printheader();                                                   //print header bitstream:
                
                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);
                
                
                
                
                actualsize += payload.length;
                fileoutput.write(payload);
                fileoutput.flush();
                
                
                
                if((player == null && actualsize >=   15000) ||( (player != null) && (player.isComplete() && actualsize > 15000)))
                {
                    
                    player = new Mp3Player(Directory_Archive +Temp_mp3_archive);
                    player.play();
                    
                    

                }   } catch (IOException ex) {
                Logger.getLogger(StreamProcessor.class.getName()).log(Level.SEVERE, null, ex);
                
            }
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(StreamProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        }while(player == null || !player.isComplete());                 //loop while the player hasn't been initialized or completed streaming yet
    }
    
    public void start()
    {
        if (t == null)
        {
            t = new Thread(this, threadname);
            t.start();
        }
    }
    
    public void pause()
    {
        if (player != null)
        {
            player.pauseToggle();
        }
    }
}
