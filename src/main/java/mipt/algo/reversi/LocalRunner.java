package mipt.algo.reversi;

import mipt.algo.reversi.testing.GameServer;

/**
 * ReversiServer
 * mipt.algo.reversi
 * <p>
 * Created by ilya on 05.05.17.
 */
public class LocalRunner {
    public static void main(String[] args) {
        GameServer server = new GameServer(4242, "", null);
        server.createListeningServer();
        server.testGame(1, 2, false);
    }
}
