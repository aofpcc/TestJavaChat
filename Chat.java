import java.net.*;
import java.io.*;
import java.util.*;
import static java.lang.System.out;

public class Chat {
  private Scanner scan;
  private static String name;
  private Role role;
  private static final int portB = 6969;
  private static final int portC = 6900;
  
  private List<Socket> sockets;
  
  private Set<String> listIP;
  
  private WaitServer waitServer;
  private WaitClient waitClient;
  private FindClient findClient;
  
  private enum Role {
    HOST, CLIENT
  }
  
  public Chat() {
    sockets = new ArrayList<Socket>();
    scan = new Scanner(System.in);
    System.out.print( "Enter you name: " );
    name = scan.nextLine();
    listIP = new HashSet<String>();
  }
  
  public void setting() {
    waitServer = new WaitServer(this);
    waitClient = new WaitClient(this);
    findClient = new FindClient(this);
  }
  
  public void findBroadCast() {
    HashSet<InetAddress> listOfBroadcasts = new HashSet<InetAddress>();
    Enumeration list;
    try {
      list = NetworkInterface.getNetworkInterfaces();
      
      while(list.hasMoreElements()) {
        NetworkInterface iface = (NetworkInterface) list.nextElement();
        
        if(iface == null) continue;
        
        if(!iface.isLoopback() && iface.isUp()) {
          //System.out.println("Found non-loopback, up interface:" + iface);
          
          Iterator it = iface.getInterfaceAddresses().iterator();
          while (it.hasNext()) {
            InterfaceAddress address = (InterfaceAddress) it.next();
            //System.out.println("Found address: " + address);
            if(address == null) continue;
            InetAddress broadcast = address.getBroadcast();
            if(broadcast != null) 
            {
              String broadCast = broadcast.toString().replaceAll("/","");
              listIP.add( broadCast);
              System.out.println("Found broadcast: " + broadCast);
              //listOfBroadcasts.add(broadcast);
            }
          }
        }
      }
    } catch (SocketException ex) {
      System.err.println("Error while getting network interfaces");
      ex.printStackTrace();
    }
    
    // return listOfBroadcasts;
  }
  
  public void setRole() {
    findBroadCast();
    setting();
    System.out.println( "Select the following role -> " );
    System.out.println( "\t1.Host" );
    System.out.println( "\t2.Client" );
    System.out.print("You choose: ");
    
    String r = scan.nextLine();
    int n = 0;
    if( isNumber(r) ) n = Integer.parseInt( r );
    if( n != 1 && n !=2 ) {
      System.out.println( "Your mother fucker!!!! Type it correctly" );
      return;
    }
    if( n == 1 ) {
      role = Role.HOST;
    }else if( n == 2) {
      role = Role.CLIENT;
    }
    // Do some thing with role 
    switch( n ) {
      case 1: 
        findClient.start();
        waitClient.start();
        break;
      case 2: 
        waitServer.start();
        break;
      default:;
    }
  }
  public boolean isNumber(String str) {
    try {
      Integer.parseInt( str);
    }catch( Exception e) {
      return false;
    }
    return true;
  }
  
  static class WaitServer extends Thread {
    private DatagramSocket socket;
    private Chat chat;
    public WaitServer(Chat chat) {
      this.chat = chat;
    }
    @Override
    public void run() {
      try {
        socket = new DatagramSocket( Chat.portB );
      }catch (Exception e){
        e.printStackTrace();
      }
      while( true ) {
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket( data, data.length );
        Socket s = null;
        try{
          socket.receive( packet );
          
          int size = chat.listIP.size();
          //chat.listIP.add( packet.getAddress().getHostAddress() );
          //if( chat.listIP.size() == size ) continue;
          
          String d = new String( packet.getData() );
          //System.out.println( d );
          
          if( !d.equals("I'm Server") ) {
            continue;
          }
          
          String a = packet.getAddress().getHostAddress();
          System.out.println( a );
          s = new Socket( a, Chat.portC );
          chat.sockets.add( s );
          
        }catch(Exception e) {
          e.printStackTrace();
        }finally{
        }
        // do some thing with ip that not in the list and findServer
        try {
          PrintWriter OUT = new PrintWriter( s.getOutputStream() );
          OUT.println( name );
          OUT.flush();
        }catch( Exception e) {
          e.printStackTrace();
        }
      }
    }  
  }
  
  static class WaitClient extends Thread {
    private ServerSocket server;
    private Chat chat;
    public WaitClient(Chat chat) {
      //System.out.println("WaitClient");
      try{
        server = new ServerSocket( Chat.portC );
      }catch(Exception e) {
        e.printStackTrace();
      }
      //System.out.println("Close WaitClient");
    }
    @Override
    public void run() {
      System.out.println( "Waiting for clients...." );
      while( true ) {
        try {
          Socket sock = server.accept();
          chat.addUser( sock );
          
          System.out.println("Client connected from: " + sock.getLocalAddress().getHostName());
          // create thread for accept chat
          
        }catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public void addUser( Socket socket ) throws Exception {
    Scanner scan = new Scanner(socket.getInputStream());
    String username = scan.nextLine();
    // add username
    sockets.add( socket );
    for(Socket s: sockets ) {
      PrintWriter pr = new PrintWriter( s.getOutputStream() );
      pr.println( username + " has joint.");
      pr.flush();
    }
  }
  
  static class FindClient extends Thread {
    private DatagramSocket socket;
    private Chat chat;
    private long lastTimeSend;
    public FindClient(Chat chat) {
      this.chat = chat;
      lastTimeSend = System.nanoTime();
    }
    public void run() {
      while( true ) {
        try {
          for(String ip: chat.listIP) {
            //System.out.println( ip );
            boardcastMessage("I'm Server", InetAddress.getByName(ip));
          }
          Thread.sleep( 1000 );
        }catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
    public void boardcastMessage(String msg, InetAddress address) throws Exception{
      socket = new DatagramSocket();
      socket.setBroadcast(true);
      byte[] buffer = msg.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Chat.portB);
      
      socket.send(packet);
      //System.out.println( msg );
      socket.close();
    }
  }
  
  public static void main(String[] args) {
    Chat chat = new Chat();
    chat.setRole();
  }
}