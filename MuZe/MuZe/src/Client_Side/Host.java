/*
 * This client is the one designed to work with threading and stream processors
 *
 */
package Client_Side;

import RTSPtest.Mp3Player;
import RTSPtest.RTPpacket;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.Timer;
import javazoom.jl.decoder.JavaLayerException;

public class Host{

  //GUI
  //----
  JFrame f = new JFrame("Client");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Teardown");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  static JTextArea textfield = new JTextArea();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;

  //Thread that will take care of updating playlist
  static PlaylistManager pmanager;

  //RTP variables:
  //----------------
  DatagramPacket rcvdp; //UDP packet received from the server
  static DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
  static int RTP_RCV_PORT = 5544; //port where the client will receive the RTP packets
  
  Timer timer; //timer used to receive data from the UDP socket
  byte[] buf; //buffer used to store data received from the server 
 
  //RTSP variables
  //----------------
  //rtsp states 
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  final static int PAUSED = 3;
  static int state; //RTSP state == INIT or READY or PLAYING
  Socket RTSPsocket; //socket used to send/receive RTSP messages
  //input and output stream filters
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String VideoFileName; //video file to request to the server
  static String SongFileName;   //song file to request to the server
  static int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
  static int RTSPid = 0; //ID of the RTSP session (given by the RTSP Server)

  
  private Mp3Player player;
  private static final String DIRECTORY_ARCHIVE_TEMPORARY = "./temp/";
  private static final String ARCHIVE_MP3_TEMPORARY = "music.mp3";
  final static String CRLF = "\r\n";
  private static long tamanhoactual;
  
  static StreamProcessorHost sp;
  
  private static int filecount = 0;
  
  
  
  
  public static FileOutputStream fileoutput;

  //Video constants:
  //------------------
  static int MJPEG_TYPE = 14; //RTP payload type for MP3  audio
 
  //--------------------------
  //Constructor
  //--------------------------
  public Host() {

    //build GUI
    //--------------------------
 
    //Frame
    f.addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
         System.exit(0);
       }
    });

    //Buttons
    buttonPanel.setLayout(new GridLayout(1,0));
    buttonPanel.add(setupButton);
    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(tearButton);
    
    setupButton.addActionListener(new setupButtonListener());
    playButton.addActionListener(new playButtonListener());
    pauseButton.addActionListener(new pauseButtonListener());
    tearButton.addActionListener(new tearButtonListener());

   
    //Image display label
    iconLabel.setIcon(null);
    
    //frame layout
    mainPanel.setLayout(null);
    mainPanel.add(iconLabel);
    mainPanel.add(buttonPanel);
    iconLabel.setBounds(0,0,380,280);
    buttonPanel.setBounds(0,280,380,50);
    
    
    

    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
    f.getContentPane().add(textfield, BorderLayout.NORTH);
    textfield.setEditable(false);
    
   
    f.setSize(new Dimension(390,370));
    f.setVisible(true);

    //init timer
    //--------------------------
    timer = new Timer(5, new timerListener());
    timer.setInitialDelay(0);
    timer.setCoalesce(true);

    //allocate enough memory for the buffer used to receive data from the server
    buf = new byte[15000];    
  }

  //------------------------------------
  //main
  //------------------------------------
  public static void main(String argv[]) throws Exception
  {
    //Create a Client object
    Host theClient = new Host();
    
    //get server RTSP port and IP address from the command line
    //------------------
    int RTSP_server_port = 5544;
    String ServerHost = "localhost";
    InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);

    //get video filename to request:
    VideoFileName = "test.mp3";
    

    //Establish a TCP connection with the server to exchange RTSP messages
    //------------------
    theClient.RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);
    
    //set up playlist manager with socket
    //pmanager = new PlaylistManager(textfield);
    
    //Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()) );

    //init RTSP state:
    state = INIT;
    
    File tempdirectory = new File(DIRECTORY_ARCHIVE_TEMPORARY);
    
    if (!tempdirectory.exists())
    {
        tempdirectory.mkdir();
        tempdirectory = null;
    }
    fileoutput = new FileOutputStream(DIRECTORY_ARCHIVE_TEMPORARY + Integer.toString(filecount) + ARCHIVE_MP3_TEMPORARY);
    
    //send setup to the server
    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                  
    //set TimeOut value of the socket to 5msec.
    RTPsocket.setSoTimeout(5);
    
    RTSPSeqNb = 1;
              
      //Send SETUP message to the server
      send_RTSP_request("SETUP");

      //Wait for the response
      if (parse_server_response() != 200) {
          System.out.println("Invalid Server Response");
      } else {
          //change RTSP state and print new state
          state = READY;
          tamanhoactual = 0;
          //System.out.println("New RTSP state: ....");
      }
      //else if state != INIT then do nothing
      //pmanager.start();
      if (state == READY) 
        {
          try {
              //increase RTSP sequence number
              RTSPSeqNb = RTSPSeqNb++;
              
              
              //Send PLAY message to the server
              send_RTSP_request("PLAY");
              
              //Wait for the response
              if (parse_server_response() != 200)
                  System.out.println("Invalid Server Response");
              else
              {
                  //change RTSP state and print out new state
                  state=PLAYING;
                  
                  sp = new StreamProcessorHost(RTPsocket);
                  sp.start();
                  sp.join();
                  System.out.println("Playback finished");
                  state = READY;
                 
              }
          }
         
          
          catch (Exception ex) {
              //Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      
      //play it again sam
      
       if (state == READY) 
        {
          try {
              //increase RTSP sequence number
              RTSPSeqNb = RTSPSeqNb++;
              
              
              //Send PLAY message to the server
              send_RTSP_request("PLAY");
              
              //Wait for the response
              if (parse_server_response() != 200)
                  System.out.println("Invalid Server Response");
              else
              {
                  //change RTSP state and print out new state
                  state=PLAYING;
                  
                  sp = new StreamProcessorHost(RTPsocket);
                  sp.start();
                  sp.join();
                  System.out.println("Playback finished");
                  state = READY;
                 
              }
          }
         
          
          catch (Exception ex) {
              //Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        
  }


  //------------------------------------
  //Handler for buttons
  //------------------------------------

  //.............
  //TO COMPLETE
  //.............

  //Handler for Setup button
  //-----------------------
  class setupButtonListener implements ActionListener{
    public void actionPerformed(ActionEvent e){

      //System.out.println("Setup Button pressed !");      

      if (state == INIT) 
        {
          try{
              //Init non-blocking RTPsocket that will be used to receive data
              try{
                  //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                  RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                  
                  //set TimeOut value of the socket to 5msec.
                  RTPsocket.setSoTimeout(5);
                  
              }
              catch (SocketException se)
              {
                  System.out.println("Socket exception: "+se);
                  System.exit(0);
              }
              
              //init RTSP sequence number
              RTSPSeqNb = 1;
              
              //Send SETUP message to the server
              send_RTSP_request("SETUP");
              
              //Wait for the response
              if (parse_server_response() != 200)
                  System.out.println("Invalid Server Response");
              else
              {
                  //change RTSP state and print new state
                  state = READY;
                  tamanhoactual = 0;
                  //System.out.println("New RTSP state: ....");
              }
          }//else if state != INIT then do nothing

          catch (Exception ex)
            {
              Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
  }
  
  //Handler for Play button
  //-----------------------
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      //System.out.println("Play Button pressed !"); 

      if (state == READY) 
        {
          try {
              //increase RTSP sequence number
              RTSPSeqNb = RTSPSeqNb++;
              
              
              //Send PLAY message to the server
              send_RTSP_request("PLAY");
              
              //Wait for the response
              if (parse_server_response() != 200)
                  System.out.println("Invalid Server Response");
              else
              {
                  //change RTSP state and print out new state
                  state=PLAYING;
                  
                  sp = new StreamProcessorHost(RTPsocket);
                  sp.start();
                  // System.out.println("New RTSP state: ...")
                  
                  //start the timer
                  //timer.start();
                  
                  
                  
                  /*if (player != null)
                  {player
                     
                       player.play();
                  }*/
              }
          }
         
          
          catch (Exception ex) {
              Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      else if (state == PAUSED)
      {
          try {
              //increase RTSP sequence number
              RTSPSeqNb = RTSPSeqNb++;
              
              
              //Send PLAY message to the server
              //send_RTSP_request("PLAY");
              
              //Wait for the response
              //if (parse_server_response() != 200)
                  //System.out.println("Invalid Server Response");
              //else
              {
                  //change RTSP state and print out new state
                  state=PLAYING;
                  // System.out.println("New RTSP state: ...")
                  
                  //start the timer
                  //timer.restart();
                  
                  
                  
                  if (player != null)
                  {
                     
                       sp.pause();
                  }
              }
          }
         
          
          catch (Exception ex) {
              Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
      }

    }
  }


  //Handler for Pause button
  //-----------------------
  class pauseButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      //System.out.println("Pause Button pressed !");   

      if (state == PLAYING) 
        {
          try {
              //increase RTSP sequence number
              RTSPSeqNb = RTSPSeqNb++;
              
              //Send PAUSE message to the server
              //send_RTSP_request("PAUSE");
              
              //Wait for the response
              //if (parse_server_response() != 200)
                  //System.out.println("Invalid Server Response");
              //else
              //{
                  //change RTSP state and print out new state
                  state= PAUSED;
                  //System.out.println("New RTSP state: ...");
                  sp.pause();
                  //stop the timer
                  //timer.stop();
                  //player.pause();
              //}
          }
          //else if state != PLAYING then do nothing
          catch (Exception ex) {
              Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
    }
  }

  //Handler for Teardown button
  //-----------------------
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      //System.out.println("Teardown Button pressed !");  

      //increase RTSP sequence number
      RTSPSeqNb = RTSPSeqNb++;
      

      //Send TEARDOWN message to the server
      send_RTSP_request("TEARDOWN");

        try {
            //Wait for the response
            if (parse_server_response() != 200)
                System.out.println("Invalid Server Response");
            else
            {
                //change RTSP state and print out new state
                state=INIT;
                //System.out.println("New RTSP state: ...");
                
                //stop the timer
                timer.stop();
                
                //exit
                System.exit(0);
            }
        } catch (Exception ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  }


  //------------------------------------
  //Handler for timer
  //------------------------------------
  
  class timerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      
      //Construct a DatagramPacket to receive data from the UDP socket
      rcvdp = new DatagramPacket(buf, buf.length);

      try{
        //receive the DP from the socket:
        RTPsocket.receive(rcvdp);
          
        //create an RTPpacket object from the DP
        RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

        //print important header fields of the RTP packet received: 
        //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
        
        //print header bitstream:
        //rtp_packet.printheader();

        //get the payload bitstream from the RTPpacket object
        int payload_length = rtp_packet.getpayload_length();
        byte [] payload = new byte[payload_length];
        rtp_packet.getpayload(payload);

        //get an Image object from the payload bitstream
        //Toolkit toolkit = Toolkit.getDefaultToolkit();
        //Image image = toolkit.createImage(payload, 0, payload_length);
        
        //display the image as an ImageIcon object
        //icon = new ImageIcon(image);
        //iconLabel.setIcon(icon);
        //fileoutput = new FileOutputStream(DIRECTORY_ARCHIVE_TEMPORARY + ARCHIVE_MP3_TEMPORARY);
        
       
        tamanhoactual += payload.length;
        fileoutput.write(payload);
        fileoutput.flush();
        
        
       
        if((player == null && tamanhoactual >=   15000) ||( (player != null) && (player.isComplete() && tamanhoactual > 15000)))  
        {
            
            player = new Mp3Player(DIRECTORY_ARCHIVE_TEMPORARY + Integer.toString(filecount)+ARCHIVE_MP3_TEMPORARY);
            player.play();
            
            //filecount++;
            
            //fileoutput = new FileOutputStream(DIRECTORY_ARCHIVE_TEMPORARY + Integer.toString(filecount)+ARCHIVE_MP3_TEMPORARY);
           
        }
        
        
        /*else if (player != null)
        {
            if(player.isComplete())
            {
                fileoutput = new FileOutputStream(DIRECTORY_ARCHIVE_TEMPORARY + ARCHIVE_MP3_TEMPORARY);
                player = new SoundJLayer(DIRECTORY_ARCHIVE_TEMPORARY + ARCHIVE_MP3_TEMPORARY);
                tamanhoactual = 0;
                player.play();
            }
        }*/
                
      }
      catch (InterruptedIOException iioe){
        //System.out.println("Nothing to read");
      }
      catch (IOException ioe) {
        System.out.println("Exception caught: "+ioe);
      } catch (Exception ex) {
            Logger.getLogger(Host.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  }

  //------------------------------------
  //Parse Server Response
  //------------------------------------
  private static int parse_server_response() throws Exception 
  {
    int reply_code = 0;

    try{
      //parse status line and extract the reply_code:
      String StatusLine = RTSPBufferedReader.readLine();
      //System.out.println("RTSP Client - Received from Server:");
      System.out.println(StatusLine);
    
      StringTokenizer tokens = new StringTokenizer(StatusLine);
      tokens.nextToken(); //skip over the RTSP version
      reply_code = Integer.parseInt(tokens.nextToken());
      
      //if reply code is OK get and print the 2 other lines
      if (reply_code == 200)
        {
          String SeqNumLine = RTSPBufferedReader.readLine();
          System.out.println(SeqNumLine);
          
          String SessionLine = RTSPBufferedReader.readLine();
          System.out.println(SessionLine);
        
          //if state == INIT gets the Session Id from the SessionLine
          tokens = new StringTokenizer(SessionLine);
          tokens.nextToken(); //skip over the Session:
          RTSPid = Integer.parseInt(tokens.nextToken());
        }
      
    }
    catch(Exception ex)
      {
        System.out.println("Exception caught c: "+ex);
        System.exit(0);
      }
    
    return(reply_code);
  }

  //------------------------------------
  //Send RTSP Request
  //------------------------------------

  //.............
  //TO COMPLETE
  //.............
  
  private static void send_RTSP_request(String request_type)
  {
    try{
      //Use the RTSPBufferedWriter to write to the RTSP socket

      //write the request line:
      RTSPBufferedWriter.write(request_type+" "+VideoFileName+" RTSP/1.0"+CRLF);
      System.out.print(request_type+" "+VideoFileName+" RTSP/1.0"+CRLF);
      
      //write the CSeq line: 
      RTSPBufferedWriter.write("CSeq "+ RTSPSeqNb +CRLF);
      System.out.print("CSeq "+ RTSPSeqNb +CRLF);
      
      //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_RCV_PORT
      if(request_type.equals("SETUP"))
      {
          RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= "+ RTP_RCV_PORT + CRLF);
          System.out.print("Transport: RTP/UDP; client_port= "+ RTP_RCV_PORT + CRLF);
      }
      //otherwise, write the Session line from the RTSPid field
      else
      {
          RTSPBufferedWriter.write("Session: "+ RTSPid + CRLF);
          System.out.print("Session: "+ RTSPid + CRLF);
      }
      try
      {
      RTSPBufferedWriter.flush();
      }
      catch(IOException e)
      {
          System.out.println("Exception caught a: "+e.toString());
      }
    }
    catch(Exception ex)
      {
        System.out.println("Exception caught b: "+ex);
        System.exit(0);
      }
  }

}//end of Class Client
