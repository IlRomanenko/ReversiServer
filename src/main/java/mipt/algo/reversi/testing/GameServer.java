package mipt.algo.reversi.testing;

import mipt.algo.reversi.database.SolutionStore;
import mipt.algo.reversi.protocol.StringProtocol;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * ReversiServer
 * mipt.algo.reversi.testing
 * <p>
 * Created by ilya on 03.05.17.
 */
public class GameServer {

    private class GameResult {

        private GameStatus status;

        private int firstScore;
        private int secondScore;

        public GameResult(GameStatus status, int winner) {
            this.status = status;
            if (winner == 0) {
                firstScore = 64;
                secondScore = 0;
            } else {
                firstScore = 0;
                secondScore = 64;
            }
        }

        public GameResult(int firstScore, int secondScore) {
            this.status = GameStatus.OK;
            this.firstScore = firstScore;
            this.secondScore = secondScore;
        }

        @Override
        public String toString() {
            if (status != GameStatus.OK) {
                return "Error(" + status.toString() + ") during game -> { 1 : " + firstScore + " , 2 : " + secondScore + " }";
            } else {
                return "Final score -> { 1 : " + firstScore + " , 2 : " + secondScore + " }";
            }
        }
    }

    private enum GameStatus {
        SERVER_ERROR, INVALID_TOKEN,
        TIMEOUT_ERROR, INVALID_TURN,
        OK
    }

    private GameClient clients[];

    private ServerSocket acceptSocket;
    private Integer[][] map;
    private Integer[] ids;

    private volatile Boolean connectionsAccepted = false;
    private static final Integer STRATEGY_TIMEOUT = 3000;
    private int port;
    private String token;
    private Thread acceptConnThread;

    private SolutionStore solutionStore;

    public GameServer(int port, String token, SolutionStore store) {
        this.port = port;
        this.token = token;
        this.solutionStore = store;

        map = new Integer[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                map[i][j] = -1;
            }
        }
        clients = new GameClient[2];
    }

    public void createListeningServer() {
        acceptConnThread = new Thread( () -> {
            try {
                acceptConnections();
            } catch (IOException e) {
                connectionsAccepted = false;
            }
        });
        acceptConnThread.start();
    }

    public void testGame(Integer firstId, Integer secondId) {

        try {
            int cnt = 0;
            while (cnt < 10) {
                Thread.sleep(100);
                cnt++;
                if (connectionsAccepted) {
                    break;
                }
            }
            if (!connectionsAccepted) {
                acceptConnThread.interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        GameStatus initResult = initgame(firstId, secondId);
        if (initResult != GameStatus.OK) {
            solutionStore.saveTestResult(firstId, secondId, "Invalid game {" + initResult.toString() + "}");
            return;
        }
        GameResult gameResult = gameloop();
        solutionStore.saveTestResult(firstId, secondId, gameResult.toString());
    }

    private GameStatus initgame(Integer firstId, Integer secondId) {

        ids = new Integer[2];
        for (int i = 0; i < 2; i++) {
            String clientToken = clients[i].readToken();

            if (!token.equals(clientToken)) {
                return GameStatus.INVALID_TOKEN;
            }

            Map.Entry<Integer, Integer> msg = clients[i].readTurn();
            ids[i] = msg.getKey();
        }

        if (ids[1].equals(firstId)) {
            GameClient var = clients[0];
            clients[0] = clients[1];
            clients[1] = var;

            Integer intVar = ids[0];
            ids[0] = ids[1];
            ids[1] = intVar;
        }

        if (!ids[0].equals(firstId) || !ids[1].equals(secondId)) {
            return GameStatus.SERVER_ERROR;
        }

        for (int i = 0; i < 2; i++) {
            if (ids[i].equals(firstId)) {
                clients[i].sendMessage("white");
            } else {
                clients[i].sendMessage("black");
            }
        }
        return GameStatus.OK;
    }

    private GameStatus validateTurn(Map.Entry<Integer, Integer> turn) {
        if (turn == null) {
            return GameStatus.TIMEOUT_ERROR;
        }
        int x = turn.getKey(), y = turn.getValue();
        if (x < 0 || x >= 8 || y < 0 || y >= 8 || map[x][y] != -1) {
            return GameStatus.INVALID_TURN;
        }
        return GameStatus.OK;
    }

    private GameResult gameloop() {
        Integer[][] offsets = new Integer[][] {
                new Integer[]{-1, 0},
                new Integer[]{-1, 1},
                new Integer[]{0, 1},
                new Integer[]{1, 1},
                new Integer[]{1, 0},
                new Integer[]{1, -1},
                new Integer[]{-1, 0},
                new Integer[]{-1, -1}
        };

        Integer offsetSize = 8;

        for (int i = 0; i < 64; i++) {
            Integer cur = i ^ 2;
            Map.Entry<Integer, Integer> turn = clients[cur].readTurn();

            GameStatus turnStatus = validateTurn(turn);

            if (turnStatus != GameStatus.OK) {
                return new GameResult(turnStatus, cur ^ 1);
            }

            int x = turn.getKey(), y = turn.getValue();

            for (int j = 0; j < offsetSize; j++) {
                boolean canRevert = false;
                int revertLength = 0;

                for (int c = 1; c < 8 ;c++) {
                    int tx = x + offsets[j][0] * c, ty = y + offsets[j][1] * c;
                    if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                        if (map[tx][ty] == -1) {
                            break;
                        } else if (map[tx][ty].equals(cur)) {
                            canRevert = true;
                            revertLength = c;
                        }
                    } else {
                        break;
                    }
                }
                if (canRevert) {
                    for (int c = 1; c < revertLength; c++) {
                        int tx = x + offsets[j][0] * c, ty = y + offsets[j][1] * c;
                        map[tx][ty] = cur;
                    }
                }
            }
        }
        int firstType = 0, secondType = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (map[i][j] == 0) {
                    firstType++;
                } else {
                    secondType++;
                }
            }
        }

        return new GameResult(firstType, secondType);
    }

    private void acceptConnections() throws IOException {
        acceptSocket = new ServerSocket(port);
        for (int i = 0; i < 2; i++) {
            Socket clientSocket = acceptSocket.accept();
            clients[i] = new GameClient(clientSocket, new StringProtocol(), STRATEGY_TIMEOUT);
        }
        connectionsAccepted = true;
    }
}
