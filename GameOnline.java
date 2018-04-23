import java.net.*;
import java.io.*;
import java.util.*;
import static java.lang.System.out;

public class GameOnline {
  private static Host host;
  private static Client client;
  
  private static int portChat = 9876; // port chatting
  private static int portRoom = 9875; // connect to call api
  private static int portInit = 9874; // finding server port
  
  private static List<String> listBroadCast;
  
  static class Host {
    public List<Socket> sockets;
    private Set<String> listIP;
    private FindClient fc;
    private String roomName;
    public Host(String rn ) {
      roomName = rn;
    }
    public void findClient() {
      if( fc!= null ) {
        fc.interrupt();
      }
      fc = new FindClient(roomName);
      fc.start();
    }
    public void stopFindClient() {
      if( fc == null ) return;
      fc.interrupt();
    }
    static class FindClient extends Thread {
      private DatagramSocket socket;
      private List<String> listBroadCast;
      private String roomName;
      private int portInit;
      public FindClient(String roomName) {
        portInit = GameOnline.portInit;
        this.roomName = roomName;
        listBroadCast = GameOnline.listBroadCast;
      }
      public void run() {
        while( true ) {
          try {
            for(String ip: listBroadCast) {
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
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, portInit);
        
        socket.send(packet);
        socket.close();
      }
    }
  }
  
  static class Client {
    private String ip;
    private Socket host;
    private String name;
    private Set<String> hostList;
    private FindServer fs;
    public Client() {
      hostList = new HashSet<String>();
    }
    public void setName(String name) {
      this.name = name;
    }
    public void setHost(Socket host) {
      this.host = host;
    }
    public void findServer() {
      if( fs != null ) fs.interrupt();
      fs = new FindServer(this);
      fs.start();
    }
    static class FindServer extends Thread {
      private DatagramSocket socket;
      private Client client;
      private int portFinding;
      private int portRoom;
      public FindServer(Client client) {
        portRoom = GameOnline.portRoom;
        portFinding = GameOnline.portInit;
        this.client = client;
      }
      @Override
      public void run() {
        try {
          socket = new DatagramSocket( portFinding );
          while( true ) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket( data, data.length );
            Socket s = null;
            socket.receive( packet );
            
            int size = client.hostList.size();
            client.hostList.add( packet.getAddress().getHostAddress() );
            if( client.hostList.size() == size ) continue;
            
            String d = new String( packet.getData() );
            
            if( d.indexOf("Server") == -1 ) {
              continue;
            }
            String[] temp = d.split(":"); // Server:Name
            String n = temp[1]; // n = name
            System.out.println(n);
            //String a = packet.getAddress().getHostAddress();
            //chat.chatable.addHost( n, a);
            
            //s = new Socket( a, Chat.portC );
            //chat.addUser(s);
          }
        }catch (Exception e){
          e.printStackTrace();
        }
      }  
    }
  }
  
  public static Host getHost(String roomName) {
    if(host!=null) return host;
    return host = new Host(roomName);
  }
  
  public static Client getClient() {
    if(client!=null) return client;
    return client = new Client();
  }
  
  public static void setBroadCast() {
    listBroadCast = new ArrayList<String>();
    HashSet<InetAddress> set = findBroadCast();
    for(InetAddress ia : set) {
      listBroadCast.add(ia.getHostAddress());
    }
  }
  
  public static HashSet<InetAddress> findBroadCast() {
    HashSet<InetAddress> listOfBroadcasts = new HashSet<InetAddress>();
    Enumeration list;
    try {
      list = NetworkInterface.getNetworkInterfaces();
      while(list.hasMoreElements()) {
        NetworkInterface iface = (NetworkInterface) list.nextElement();
        if(iface == null) continue;
        if(!iface.isLoopback() && iface.isUp()) {
          Iterator it = iface.getInterfaceAddresses().iterator();
          while (it.hasNext()) {
            InterfaceAddress address = (InterfaceAddress) it.next();
            if(address == null) continue;
            InetAddress broadcast = address.getBroadcast();
            if(broadcast != null) 
            {
              String broadCast = broadcast.toString().replaceAll("/","");
              //System.out.println("Found broadcast: " + broadCast);
              listOfBroadcasts.add(broadcast);
            }
          }
        }
      }
    } catch (SocketException ex) {
      System.err.println("Error while getting network interfaces");
      ex.printStackTrace();
    }
    return listOfBroadcasts;
  }
  
}