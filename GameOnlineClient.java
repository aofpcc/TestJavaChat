public class GameOnlineClient {
  public static void main(String... args) {
    GameOnline.Client client = GameOnline.getClient("What!!");
    client.setClientable(new Clientable() {
      public void getInput(Pack pack) {
        System.out.println(pack);
      }
      public void getInput(String str) {
        System.out.println(str);
      }
      public void update(Pack pack) {
        System.out.println("Do something");
      }
    });
    client.findServer();
    client.testChat();
  }
}