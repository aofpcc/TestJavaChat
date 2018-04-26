import java.net.*;
import java.util.*;
public class GameOnlineTest { // Server
  public static void main(String... args) {
    String prompt = "Console::";
    Scanner scan = new Scanner(System.in);
    GameOnline.setBroadCast();
    GameOnline.Host host = GameOnline.getHost("Aof");
    host.catchClient();
    host.setClientable(new Clientable() {
      public void getInput(Pack pack) {
        System.out.println(pack);
      }
      public void getInput(String str) {
        System.out.println(str);
      }
      public void update(Pack pack) {
        System.out.println("Update");
      }
    });
    host.findClient();
    while(true) {
      System.out.print( prompt + " >");
      String str = scan.nextLine().trim();
      String temp;
      switch(str) {
        case "listAll" :
          host.showCurrentUser();
          break;
        case "prompt" :
          prompt = scan.nextLine(); // ""
          break;
        case "getAll":
          Set<GameOnline.Client> clients = host.clientSet();
          for(GameOnline.Client c: clients) {
            System.out.println( c.getName() + " : " + c.getIP() );
          }
        default: ;
      }
    }
  }
}