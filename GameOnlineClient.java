public class GameOnlineClient {
  public static void main(String... args) {
    GameOnline.Client client = GameOnline.getClient("Bunya555");
    client.findServer();
    client.testChat();
  }
}