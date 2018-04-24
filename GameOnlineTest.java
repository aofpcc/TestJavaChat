import java.net.*;
import java.util.*;
public class GameOnlineTest { // Server
  public static void main(String... args) {
    String prompt = "Console::";
    Scanner scan = new Scanner(System.in);
    GameOnline.setBroadCast();
    GameOnline.Host host = GameOnline.getHost("Aof");
    host.catchClient();
    host.findClient();
    while(true) {
      System.out.print( prompt + " >");
      String str = scan.nextLine().trim();
      switch(str) {
        case "listAll" :
          host.showCurrentUser();
          break;
        case "prompt" :
          prompt = scan.nextLine(); // ""
          break;
        default: ;
      }
    }
  }
}