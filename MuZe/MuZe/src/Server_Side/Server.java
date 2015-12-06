/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server_Side;

/* ------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------- */


import RTSPtest.MP3Object;
import RTSPtest.RTPpacket;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.Timer;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;

public class Server extends JFrame implements ActionListener {
    
  //list of senders for multiple clients
  private static ArrayList<Socket> connectedClients;
  private static ArrayList<OutputStream> outputstreams;
  private static ServerSocket listenSocket;
  private static AcceptingThread at;

  //RTP variables:
  //----------------
  DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
  DatagramPacket senddp; //UDP packet containing the video frames

  InetAddress ClientIPAddr; //Client IP address
  int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

  //GUI:
  //----------------
  JLabel label;

  //Video variables:
  //----------------
  int imagenb = 0; //image nb of the image currently transmitted
  //VideoStream video; //VideoStream object used to access video frames
  //MusicStream music;    //music stream object for music (WIP)
  static int MPA_TYPE = 14; //RTP payload type for MPEG audio
  
  private static ArrayList<String> videoqueue;         //arraylist to hold list of songs to play
  private static int queueindex = 0;
  static MP3Object songfile;
  
  static int FRAME_PERIOD = 10; //Frame period of the video to stream, in ms
  static int VIDEO_LENGTH = 500; //length of the video in frames

  Timer timer; //timer used to send the images at the video frame rate
  byte[] buf; //buffer used to store the images to send to the client 

  //RTSP variables
  //----------------
  //rtsp states
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  //rtsp message types
  final static int SETUP = 3;
  final static int PLAY = 4;
  final static int PAUSE = 5;
  final static int TEARDOWN = 6;
  final static int STOP = 7;
  static int state; //RTSP Server state == INIT or READY or PLAY
  Socket RTSPsocket; //socket used to send/receive RTSP messages
  //input and output stream filters
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String VideoFileName; //video file requested from the client
  static String SongFileName;
  static int RTSP_ID = 123456; //ID of the RTSP session
  int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
  
  final static String CRLF = "\r\n";
  
  //broadcaster to update clients on current playlist
  static PlaylistBroadcaster pb;

  static boolean currentlystreaming = true;
  //--------------------------------
  //Constructor
  //--------------------------------
  public Server(){

    //init Frame
    super("Server");

    //init Timer
    timer = new Timer(FRAME_PERIOD, this);
    timer.setInitialDelay(0);
    timer.setCoalesce(true);

    currentlystreaming = false;
    //allocate memory for the sending buffer
    buf = new byte[15000]; 

    //initialize the arraylist of senders
    
    connectedClients = new ArrayList();
    videoqueue = new ArrayList();
    
    at = new AcceptingThread();
    
     
    //Handler to close the main window
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        //stop the timer and exit
        timer.stop();
        System.exit(0);
      }});

    //GUI:
    label = new JLabel("Send frame #        ", JLabel.CENTER);
    getContentPane().add(label, BorderLayout.CENTER);
  }
          
  //------------------------------------
  //main
  //------------------------------------
  public static void main(String argv[]) throws Exception
  {
    //create a Server object
    Server theServer = new Server();

    //show GUI:
    theServer.pack();
    theServer.setVisible(true);

    //get RTSP socket port from the command line
    int RTSPport = 5544;
   
    //Initiate TCP connection with the client for the RTSP session
    listenSocket = new ServerSocket(RTSPport);
    theServer.RTSPsocket = listenSocket.accept();
    

    //Get Client IP address
    theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();

    connectedClients.add((theServer.RTSPsocket));
    
    //start up a thread to continually accept connections
    at.start();
    //Initiate RTSPstate
    state = INIT;
    
    //JUST FOR TESTING: add something to the queue alread
    videoqueue.add("test.mp3");
    videoqueue.add("test2.mp3");
    
    
    songfile = new MP3Object(videoqueue.get(queueindex));
    
    StreamSender sender = new StreamSender(connectedClients);
    //Set input and output stream filters:
    
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theServer.RTSPsocket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.RTSPsocket.getOutputStream()) );

    //Wait for the SETUP message from the client
    int request_type;
    boolean done = false;
    while(!done)
      {
        request_type = theServer.parse_RTSP_request(); //blocking
        
        if (request_type == SETUP)
          {
            done = true;

            //update RTSP state
            state = READY;
            System.out.println("New RTSP state: READY");
   
            //Send response
            theServer.send_RTSP_response();
   
            //init the VideoStream object:
            //theServer.video = new VideoStream(VideoFileName);
            //theServer.songfile = new MP3Object(VideoFileName);
            
            
            //init RTP socket
            theServer.RTPsocket = new DatagramSocket();
          }
      }

     //loop to handle RTSP requests
    while(true)
      {
        //parse the request
        request_type = theServer.parse_RTSP_request(); //blocking
            
        if ((request_type == PLAY) && (state == READY || state == PLAYING))
          {
            //send back response
            theServer.send_RTSP_response();
            sender = new StreamSender(connectedClients);





            sender.setSong(songfile);
            theServer.send_MP3_Tags();
            sender.start();

            state = PLAYING;
            System.out.println("New RTSP state: PLAYING");
            queueindex++;
            songfile = new MP3Object(videoqueue.get(queueindex % videoqueue.size()));
                
                
            
          }
        else if ((request_type == PAUSE) && (state == PLAYING))
          {
            //send back response
            theServer.send_RTSP_response();
            //stop timer
            theServer.timer.stop();
            //update state
            state = READY;
            System.out.println("New RTSP state: READY");
          }
        else if (request_type == TEARDOWN)
          {
            //send back response
            theServer.send_RTSP_response();
            //stop timer
            theServer.timer.stop();
            //close sockets
            theServer.RTSPsocket.close();
            theServer.RTPsocket.close();

            System.exit(0);
          }
        else if ((request_type == STOP) && (state == PLAYING || state == PAUSE))
        {
            sender.kill();
            sender.join();
            theServer.send_RTSP_response();
            
            state = READY;
        }
       
        
      }
  }


  //------------------------
  //Handler for timer
  //------------------------
  public void actionPerformed(ActionEvent e) {

    //if the current image nb is less than the length of the video
    if (imagenb < songfile.getSize())
      {
        //update current imagenb
        
       
        try {
          //get next frame to send from the video, as well as its size
          //byte [] temp = songfile.getFrame(imagenb);
          //int image_length = video.getnextframe(temp);

          //Builds an RTPpacket object containing the frame
          RTPpacket rtp_packet = new RTPpacket(MPA_TYPE, imagenb, imagenb*FRAME_PERIOD, songfile.getFrame(imagenb), songfile.getFrame(imagenb).length);
          
          //get to total length of the full rtp packet to send
          int packet_length = rtp_packet.getlength();

          //retrieve the packet bitstream and store it in an array of bytes
          byte[] packet_bits = new byte[packet_length];
          rtp_packet.getpacket(packet_bits);

          //send the packet as a DatagramPacket over the UDP socket 
          senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
          RTPsocket.send(senddp);

          //System.out.println("Send frame #"+imagenb);
          //print the header bitstream
          rtp_packet.printheader();

          //update GUI
          label.setText("Send frame #" + imagenb);
          imagenb++;
        }
        catch(Exception ex)
          {
            System.out.println("Exception caught 1: "+ex);
            System.exit(0);
          }
      }
    else
      {
        //if we have reached the end of the video file, stop the timer
        timer.stop();
        state = READY;
      }
  }

  //------------------------------------
  //Parse RTSP Request
  //------------------------------------
  private int parse_RTSP_request()
  {
    int request_type = -1;
    try{
      //parse request line and extract the request_type:
      String RequestLine = RTSPBufferedReader.readLine();
      //System.out.println("RTSP Server - Received from Client:");
      System.out.println(RequestLine);

      StringTokenizer tokens = new StringTokenizer(RequestLine);
      String request_type_string = tokens.nextToken();

      //convert to request_type structure:
      if ((new String(request_type_string)).compareTo("SETUP") == 0)
        request_type = SETUP;
      else if ((new String(request_type_string)).compareTo("PLAY") == 0)
      {
        request_type = PLAY;
        currentlystreaming = true;
      }
      else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
        request_type = PAUSE;
      else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
        request_type = TEARDOWN;
      else if ((new String(request_type_string)).compareTo("STOP") == 0)
          request_type = STOP;
      if (request_type == SETUP)
        {
          //extract VideoFileName from RequestLine
          //VideoFileName = tokens.nextToken();
          videoqueue.add(tokens.nextToken());
        }

      //parse the SeqNumLine and extract CSeq field
      String SeqNumLine = RTSPBufferedReader.readLine();
      System.out.println(SeqNumLine);
      tokens = new StringTokenizer(SeqNumLine);
      tokens.nextToken();
      RTSPSeqNb = Integer.parseInt(tokens.nextToken());
        
      //get LastLine
      String LastLine = RTSPBufferedReader.readLine();
      System.out.println(LastLine);

      if (request_type == SETUP)
        {
          //extract RTP_dest_port from LastLine
          tokens = new StringTokenizer(LastLine);
          for (int i=0; i<3; i++)
            tokens.nextToken(); //skip unused stuff
          RTP_dest_port = Integer.parseInt(tokens.nextToken());
        }
      //else LastLine will be the SessionId line ... do not check for now.
    }
    catch(Exception ex)
      {
        System.out.println("Exception caught 2: "+ex);
        System.exit(0);
      }
    return(request_type);
  }

  //------------------------------------
  //Send RTSP Response
  //------------------------------------
  private void send_RTSP_response()
  {
    try{
      RTSPBufferedWriter.write("RTSP/1.0 200 OK"+CRLF);
      RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
      RTSPBufferedWriter.write("Session: "+RTSP_ID+CRLF);
      RTSPBufferedWriter.flush();
      //System.out.println("RTSP Server - Sent response to Client.");
    }
    catch(Exception ex)
      {
        System.out.println("Exception caught 3: "+ex);
        System.exit(0);
      }
  }
  /*
  * This method will take the current item in the playlist, use the jid3lib in order to extract tags
    and send the resulting strings through the buffered writer streams
  */
 private void send_MP3_Tags() throws IOException, TagException
 {
     MP3File tagfile = new MP3File(videoqueue.get(queueindex));
     ID3v1 id3v1 = tagfile.getID3v1Tag();
     
     RTSPBufferedWriter.write(id3v1.getArtist() + CRLF);
     RTSPBufferedWriter.write(id3v1.getSongTitle() + CRLF);
     RTSPBufferedWriter.write(id3v1.getAlbum() + CRLF);
     RTSPBufferedWriter.flush();
 }
  
  
  /**
 * This is the thread that will manage multiple connections, will endlessly listen
 * In order to facilitate multiple client connections
 * this main server's only purpose is to continually listen for new connections and make them appropriately
 * @author devon
 */
    public class AcceptingThread implements Runnable
    {
        private Thread t;
        private String threadname;
       
        @Override
        public void run() 
        {
           
        
            while(true)
            {
                try {
                    Socket clientsocket = listenSocket.accept();
                    connectedClients.add(clientsocket);
                    
                
                } catch (IOException ex) {
                    Logger.getLogger(AcceptingThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    try {
                        listenSocket.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
           
        }
        
        public void start()
        {
            if (t == null)
            {
                threadname = "connection listening thread";
                t = new Thread(this, threadname);
                t.start();
                
            }     
        }
    }
}
