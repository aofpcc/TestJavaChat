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
        private FindClient fc;
        private CatchClient cc;
        private String roomName;
        private Set<Client> clientSet;
        private Clientable cn;
        public Host(String rn) {
            roomName = rn;
            clientSet = new TreeSet<Client>();
        }

        public void testChat() {
            Scanner scan = new Scanner(System.in);
            while(true) {
                String str = scan.nextLine();
                broadCast(str); 
            }
        }

        public Set<Client> clientSet() {
            return clientSet;
        }

        public void broadCast(String str) {
            try{ 
                for(Client client : clientSet) {
                    PrintWriter pr = new PrintWriter(client.getSocket().getOutputStream());
                    pr.println(str);
                    pr.flush();
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        public void showCurrentUser() {
            System.out.println("Client List");
            for(Client c: clientSet ) {
                System.out.println(c.getName() + " : " + c.getIP());
            }
        }

        public void catchClient() {
            if( cc != null ) cc.interrupt();
            cc = new CatchClient(this);
            cc.start();
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

        public void setClientable(Clientable cn ) {
            this.cn = cn;
        }

        public void addUser(Socket socket) {
            for( Client c : clientSet) {
                if( c.getIP().equals(socket.getInetAddress().getHostAddress() ) ) return;
            }
            try {
                Scanner scan = new Scanner(socket.getInputStream());
                String name = "";
                if( scan.hasNext() ) {
                    name = scan.nextLine();
                    System.out.println("Name : " + name);
                    Client temp = new Client(name);
                    temp.setIP(socket.getInetAddress().getHostAddress());
                    temp.setSocket(socket);
                    clientSet.add( temp );

                    ConnectedClient conn = new ConnectedClient(temp, socket, this);
                    conn.start();
                }
            }catch( Exception e ) {
                e.printStackTrace();
            }
        }

        static class ConnectedClient extends Thread {
            private Client client;
            private Socket socket;
            private Host host;
            public ConnectedClient(Client client, Socket socket, Host host) {
                this.client = client;
                this.socket = socket;
                this.host = host;
            }

            @Override
            public void run() {
                try{
                    Scanner scan = new Scanner( socket.getInputStream() );
                    while(true) {
                        if(scan.hasNext() ) {
                            //System.out.println(scan.nextLine());
                            Pack pack = new Pack(scan.nextLine());
                            if(host.cn != null) {
                                host.cn.getInput(pack);
                            }
                            switch(pack.getType() ) {
                                case MESSAGE:
                                host.broadCast( "Broadcast:" +client.getName() + "," + pack.getData());
                                break;
                                case UPDATE:
                                System.out.println( "Update:" + client.getName() + "," + pack.getData());
                                host.broadCast( "Broadcast:"+ ":" + client.getName() + "," + pack.getData());
                                break;
                                default:;
                            }
                        }
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        static class CatchClient extends Thread {
            private ServerSocket server;
            private Host host;
            public CatchClient(Host host) {
                try{
                    this.host = host;
                    server = new ServerSocket(GameOnline.portRoom);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void run() {
                System.out.println( "Waiting for clients...." );
                while( true ) {
                    try {
                        Socket sock = server.accept();
                        host.addUser( sock );
                        System.out.println("Client connected from: " + sock.getInetAddress().getHostName());
                        // create thread for accept chat
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
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

    static class Client implements Comparable<Client>{
        private String ip;
        private Socket socket;
        private String name;
        private Set<String> hostList;
        private Set<HostChoice> hostChoice;
        private FindServer fs;
        private ConnectedServer cs;
        private Clientable cn;
        private PrintWriter pr;
        private static long lastTime;
        public Client(String name) {
            this.name = name;
            hostList = new TreeSet<String>();
            hostChoice  = new TreeSet<HostChoice>();
        }
        static class HostChoice implements Comparable<HostChoice>{
          private String ip;
          private String name;
          public HostChoice(String ip, String name) {
             this.ip = ip;
             this.name = name;
          }
          public int compareTo(HostChoice hc) {
             return ip.compareTo(hc.ip);
          } 
        }
        public void setIP(String ip) {
            this.ip = ip;
        }

        public String getIP() {
            return ip;
        }

        public String getName() {
            return name;
        }

        public void setClientable(Clientable cn){
            this.cn = cn;
        }

        public void setHost(Socket host) {
            this.socket = host;
            try {
                pr = new PrintWriter(host.getOutputStream());
            } catch(Exception e ) {
                e.printStackTrace();
            }
            cs = new ConnectedServer(host, this);
            cs.start();
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public Socket getSocket() {
            return socket;
        }

        public void findServer() {
            if( fs != null ) fs.interrupt();
            fs = new FindServer(this);
            fs.start();
        }

        public void showHost() {
            System.out.println("Host List : ");
            int count = 0;
            for(String host: hostList) {
                System.out.println( (++count) + " : " + host);
            }
        }

        public void selectHost(String host) {
            if( socket != null) return;
            try {
                Socket s = new Socket(host, GameOnline.portRoom);
                setHost(s);
                PrintWriter pr = new PrintWriter(socket.getOutputStream());
                pr.println(name);
                pr.flush();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        public int compareTo(Client c) {
            return this.getIP().compareTo(c.getIP());
        }

        public void testChat() {
            Scanner scan = new Scanner(System.in);
            while(true) {
                String data = scan.nextLine(); // message
                if( data.indexOf("sysupdate") != -1 ) sendToHost(new Pack(Pack.Type.UPDATE, data.split("sysupdate")[1].trim()));
                else sendToHost(new Pack(Pack.Type.MESSAGE, data));
            }
        }

        public void sendToHost(Pack pack) {
            try {
                //System.out.println("Sent Time : " + System.nanoTime());
                lastTime = System.nanoTime();
                pr.println(pack);
                pr.flush();
            } catch( Exception e) {
                e.printStackTrace();
            }
        }
        static class ConnectedServer extends Thread {
            private Socket socket;
            private Client client;
            public ConnectedServer (Socket socket, Client client) {
                this.socket = socket;
                this.client = client;
            }

            @Override
            public void run() {
                try {
                    Scanner scan = new Scanner(socket.getInputStream());
                    while(true) {
                        if( scan.hasNext() ) {
                            String str = scan.nextLine() ;
                            System.out.println(str);
                            client.cn.getInput(new Pack(str));
                        }
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
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
                        String ip = packet.getAddress().getHostAddress();
                        client.hostList.add( ip );
                        if( client.hostList.size() == size ) continue;

                        String d = new String( packet.getData() );

                        if( d.indexOf("Server") == -1 ) {
                            continue;
                        }
                        String[] temp = d.split(":"); // Server:Name
                        String n = temp[1]; // n = name
                        client.hostChoice.add(new HostChoice(ip, n));
                        client.cn.getInput("Host:" + ip + ":" + n);
                        //System.out.println(n);

                        //System.out.println( "Connecting to " + ip );

                        //client.selectHost(ip);    <---------- Use this
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

    public static Client getClient(String name) {
        if(client!=null) return client;
        return client = new Client(name);
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

interface Clientable {
    public void getInput(Pack pack);

    public void getInput(String str);

    public void update(Pack pack);
}

class Pack {
    static enum Type {
        MESSAGE, // message
        UPDATE,
        BROADCAST
    }
    private String data;
    private Type type;
    public Pack(String str) {
        String[] arr = str.split(":");
        switch( arr[0].trim().toUpperCase() ) {
            case "MESSAGE" : type = Type.MESSAGE; break;
            case "UPDATE" : type = Type.UPDATE; break;
            default : type = Type.BROADCAST; break;
        }
        this.data = arr[1];
    }

    public Pack(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public String toString() {
        if( type == null ) return "";
        return type + ":" + data;
    }
}
