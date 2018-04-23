import java.net.*;
import java.io.*;
import java.util.*;
import static java.lang.System.out;
/**
 * @author Aof
 */
public class Chat {
  private List<User> users;
  static class User {
    private String ip; // ip is a primary key
    private String name;
    public User(String name, String ip) {
      this.ip = ip.trim();
      this.name = name;
    }
    
    public String getName() {
      return name;
    }
    public String getIP() {
      return ip;
    }
  }
  private Scanner scan;
  private static String roomName = "default room";
  private Role role;
  public static final int portB = 6969;
  public static final int portC = 6900;
  
  private static Chat inc;
  
  public List<Socket> sockets;
  private Set<String> listIP;
  
  private WaitServer waitServer;
  private WaitClient waitClient;
  private FindClient findClient;
  
  private Chatable chatable;
  
  private MainGame mainGame;
  
  private Chat() {
    sockets = new ArrayList<Socket>();
    //scan = new Scanner(System.in);
    //System.out.print( "Enter you name: " );
    //name = scan.nextLine();
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
          chat.listIP.add( packet.getAddress().getHostAddress() );
          if( chat.listIP.size() == size ) continue;
          
          String d = new String( packet.getData() );
          //System.out.println( d );
          
          if( d.indexOf("Server") == -1 ) {
            continue;
          }
          String[] temp = d.split(":"); // 
          String n = temp[1];
          
          String a = packet.getAddress().getHostAddress();
          chat.chatable.addHost( n, a);
          
          //s = new Socket( a, Chat.portC );
          //chat.addUser(s);
        }catch(Exception e) {
          e.printStackTrace();
        }finally{
        }
        // do some thing with ip that not in the list and findServer
      }
    }  
  }
  
  static class WaitClient extends Thread {
    private ServerSocket server;
    private Chat chat;
    public WaitClient(Chat chat) {
      this.chat = chat;
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
          System.out.println("Client connected from: " + sock.getInetAddress().getHostName());
          // create thread for accept chat
        }catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  static class ChatReturn extends Thread {
    private Socket socket;
    private Scanner scan;
    private Chat chat;
    public ChatReturn(Socket socket, Chat chat) {
      this.socket = socket;
      this.chat = chat;
    }
    
    public void run() {
      while(true) {
        try{
          scan = new Scanner(socket.getInputStream());
          String str = "";
          if( scan.hasNext() ) str = scan.nextLine();
          if( str.equals("") ) continue;
          System.out.println(str);
          String[] s = str.split(":");
          
          if( chat.role == Role.HOST ) { 
            String ans = chat.callApi(s, socket);
            chat.f(ans, socket);
          }else {
            //System.out.println("Get API");
            chat.getApi(s, socket);
          }
        }catch(Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public void getApi(String[] str, Socket socket) {
    String status, ip;
    String[] s;
    int x,y;
    switch(str[0].trim()) {
      case "CurUser" : 
        int size = Integer.parseInt(str[1].trim());
        List<User> temp = new ArrayList<User>();
        for(int i = 0; i < 2*size; i+=2) {
          String username = str[i+2];
          ip = str[i+3];
          temp.add(new User(username, ip));
        }
        this.users = temp;
        chatable.setListUser(users);
        break;
      case "UpdateIP":
        status = str[1].trim();
        s = status.split(",");
        ip = s[0].trim();
        x = Integer.parseInt(s[1].trim());
        y = Integer.parseInt(s[2].trim());
        mainGame.update( ip, x, y);
        break;
      case "StartGame":
        chatable.startGame();
        break;
      default: ;
    }
  }
  
  public String callApi(String[] str, Socket socket) {
    String status, ip;
    String[] s;
    int x,y;
    switch(str[0].trim()) {
      case "CurUser" : 
        StringBuilder sb = new StringBuilder(str[0].trim() + ":");
        sb.append("" + users.size() + ":");
      for( User u : users) {
        sb.append("" + u.name + ":" + u.ip + ":");
      }
      return sb.toString();
      case "Update":
        status = str[1].trim();
        s = status.split(",");
        ip = socket.getInetAddress().getHostName();
        x = Integer.parseInt(s[0].trim());
        y = Integer.parseInt(s[1].trim());
        mainGame.update( ip, x, y);
      default: 
        return String.join("", str);
    }
  }
  
  public String callApi(String[] str) {
    String status, ip;
    String[] s;
    int x,y;
    switch(str[0].trim()) {
      case "CurUser" : 
        StringBuilder sb = new StringBuilder(str[0].trim() + ":");
        sb.append("" + users.size() + ":");
      for( User u : users) {
        sb.append("" + u.name + ":" + u.ip + ":");
      }
      return sb.toString();
      default: 
        return String.join("", str);
    }
  }
  
  public void f(String str) { // BroadCast
    
    try{
      for( Socket sock: sockets) {
        PrintWriter pr = new PrintWriter( sock.getOutputStream());
        pr.println(str);
        pr.flush();
      }
    }catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public void f(String str, Socket sock) { // BroadCast
    try{
      
      PrintWriter pr = new PrintWriter( sock.getOutputStream());
      pr.println(str);
      pr.flush();
      
    }catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public void addUser( Socket socket ) throws Exception {
    Scanner scan = new Scanner(socket.getInputStream());
    String username = scan.nextLine();
    // add username
    sockets.add( socket );
    System.out.println( username + " :::: " + socket.getInetAddress().getHostName() );
    chatable.addPlayer( username, socket.getInetAddress().getHostName());
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
            boardcastMessage("Server:" + roomName, InetAddress.getByName(ip));
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
  
  public void host() {
    role = Role.HOST;
    findBroadCast();
    setting();
    findClient.start();
    waitClient.start();
  }
  
  public void client() {
    findBroadCast();
    setting();
    waitServer.start();
  }
  
  public void end() {
    if( findClient != null ) findClient.interrupt();
    if( waitClient != null ) waitClient.interrupt();
    if( waitServer != null ) waitServer.interrupt();
  }
  
  public static Chat getInc() {
    if( inc == null ) inc = new Chat();
    return inc;
  }
  
  public void set(Role role) {
    if( role == Role.HOST && ( findClient == null || !findClient.isAlive()) ) {
      end();
      host();
    }else if(role == Role.CLIENT && ( waitServer == null || !waitServer.isAlive())) {
      end();
      client();
    }
  }
  
  public static void setRoomName(String rn) {
    roomName = rn;
  }
  
  public void setDest(Chatable c) { // Set Destination
    chatable = c;
  }
  
  public void setUserList(List<User> users ) {
    this.users = users;
  }
  
  public List<User> getUser() {
    return users;
  }
  
  public void setUser(List<User> user) {
    this.users = user;
  }
  
  public void setMainGame(MainGame mainGame) {
    this.mainGame = mainGame;
  }
  
  public Role role() {
    return role;
  }
}

interface Chatable {
  
}

enum Role{
  HOST, CLIENT
}