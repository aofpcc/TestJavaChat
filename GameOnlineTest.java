import java.net.*;
import java.util.*;
public class GameOnlineTest {
  public static void main(String... args) {
    GameOnline.setBroadCast();
    GameOnline.Host host = GameOnline.getHost("Aof");
    host.catchClient();
    host.findClient();
    
    GameOnline.Client client = GameOnline.getClient("Bunya555");
    client.findServer();
    client.testChat();
  }
}