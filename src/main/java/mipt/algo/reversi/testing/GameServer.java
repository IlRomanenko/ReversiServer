package mipt.algo.reversi.testing;

import mipt.algo.reversi.database.SolutionStore;
import mipt.algo.reversi.protocol.StringProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * ReversiServer
 * mipt.algo.reversi.testing
 * <p>
 * Created by ilya on 03.05.17.
 */
public class GameServer {

    private static final Logger log = LoggerFactory.getLogger(GameServer.class);

    private class GameResult {

        private GameStatus status;

        private int firstScore;
        private int secondScore;

        public GameResult(GameStatus status, int looser) {
            this.status = status;
            if (looser == 1) {
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

    private BufferedWriter out;
    private String logPath;

    public GameServer(int port, String token, SolutionStore store, String logPath) {
        this.port = port;
        this.token = token;
        this.solutionStore = store;

        try {
            this.logPath = logPath;
            out = new BufferedWriter(new FileWriter(logPath));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        map = new Integer[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                map[i][j] = 0;
            }
        }
        map[3][3] = 2;
        map[3][4] = 1;
        map[4][3] = 1;
        map[4][4] = 2;

        clients = new GameClient[2];
    }

    public void createListeningServer() {
        acceptConnThread = new Thread(() -> {
            try {
                acceptConnections();
            } catch (IOException e) {
                connectionsAccepted = false;
                log.error(e.getMessage());
            }
        });
        acceptConnThread.start();
    }

    public void testGame(Integer firstId, Integer secondId, Boolean fixedWaiting) {

        try {
            int cnt = 0;
            while (cnt < 10 || !fixedWaiting) {
                Thread.sleep(100);
                cnt++;
                if (connectionsAccepted) {
                    break;
                }
            }
            if (!connectionsAccepted) {
                log.error("Too long waiting, interrupt listen server");
                acceptConnThread.interrupt();
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        GameStatus initResult = initgame(firstId, secondId);
        if (initResult != GameStatus.OK) {
            trySaveResult(firstId, secondId, 0, 0, "Invalid game {" + initResult.toString() + "}");
            return;
        }
        GameResult gameResult = gameloop();
        trySaveResult(firstId, secondId, gameResult.firstScore, gameResult.secondScore, gameResult.toString());
    }

    private void trySaveResult(Integer firstId, Integer secondId, Integer firstScore, Integer secondScore, String result) {
        final int MAGIC_CONST = 1000000000;
        if (solutionStore == null) {
            System.out.println(String.format("%d vs %d -> %s", firstId, secondId - MAGIC_CONST, result));
        } else {
            solutionStore.saveTestResult(firstId, secondId - MAGIC_CONST, firstScore, secondScore, result, logPath);
        }
    }

    private GameStatus initgame(Integer firstId, Integer secondId) {

        log.info("Begin init game");

        ids = new Integer[2];
        for (int i = 0; i < 2; i++) {
            String clientToken = clients[i].readToken();

            if (!token.equals(clientToken)) {
                return GameStatus.INVALID_TOKEN;
            }

            String id = clients[i].readToken();
            try {
                ids[i] = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                return GameStatus.SERVER_ERROR;
            }
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
            log.error("Can't init game, different ids");
            return GameStatus.SERVER_ERROR;
        }

        for (int i = 0; i < 2; i++) {
            if (ids[i].equals(firstId)) {
                clients[i].sendMessage("init black");
            } else {
                clients[i].sendMessage("init white");
            }
        }
        log.info("Send init, game initializing completed");
        return GameStatus.OK;
    }

    private final int[][] turnOffsets = new int[][]{
            {-1, 0},
            {-1, 1},
            {0, 1},
            {1, 1},
            {1, 0},
            {1, -1},
            {0, -1},
            {-1, -1}
    };

    private Integer getReverseLength(int x, int y, int turnOffsetIndex, int player) {
        int[] turnOffset = turnOffsets[turnOffsetIndex];
        int revertLength = 0;
        for (int c = 1; c < 8; c++) {
            int tx = x + turnOffset[0] * c, ty = y + turnOffset[1] * c;

            if (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                if (map[tx][ty] == 0) {
                    break;
                } else if (map[tx][ty] == player) {
                    revertLength = c - 1;
                    break;
                }
            } else {
                break;
            }
        }
        return revertLength;
    }

    private Set<Map.Entry<Integer, Integer>> getValidTurns(int player) {

        Set<Map.Entry<Integer, Integer>> validTurns = new HashSet<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                boolean isValidTurn = false;

                if (map[x][y] != 0) {
                    continue;
                }

                for (int j = 0; j < turnOffsets.length; j++) {
                    if (getReverseLength(x, y, j, player) > 0) {
                        isValidTurn = true;
                        break;
                    }
                }
                if (isValidTurn) {
                    validTurns.add(new AbstractMap.SimpleEntry<>(x, y));
                }
            }
        }

        return validTurns;
    }

    private void processTurn(int x, int y, int player) {
        map[x][y] = player;

        for (int j = 0; j < turnOffsets.length; j++) {
            int revertLength = getReverseLength(x, y, j, player);

            for (int c = 1; c <= revertLength; c++) {
                map[x + turnOffsets[j][0] * c][y + turnOffsets[j][1] * c] = player;
            }

        }
    }

    private void writeToLog(String str) {
        try {
            out.write(str);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private GameResult gameloop() {

        log.info("Begin testing game");
        boolean hasTurn = true;
        Set<Map.Entry<Integer, Integer>> firstValidTurns;
        Set<Map.Entry<Integer, Integer>> secondValidTurns;
        List<Queue<Map.Entry<Integer, Integer>>> playerToSendTurns = new ArrayList<>();

        playerToSendTurns.add(new LinkedList<>());
        playerToSendTurns.add(new LinkedList<>());

        int curPlayer = 1;
        int anotherPlayer;

        do {
            anotherPlayer = 3 - curPlayer;
            firstValidTurns = getValidTurns(curPlayer);
            secondValidTurns = getValidTurns(anotherPlayer);

            if (!firstValidTurns.isEmpty()) {
                while (!playerToSendTurns.get(curPlayer - 1).isEmpty()) {
                    Map.Entry<Integer, Integer> move = playerToSendTurns.get(curPlayer - 1).poll();
                    clients[curPlayer - 1].sendMove(move.getKey(), move.getValue());
                }
                clients[curPlayer - 1].sendTurn();

                Map.Entry<Integer, Integer> turn = clients[curPlayer - 1].readTurn();

                if (!firstValidTurns.contains(turn)) {
                    if (turn == null) {
                        log.error("Timeout exception");
                        return new GameResult(GameStatus.TIMEOUT_ERROR, curPlayer);
                    }

                    writeToLog("Turn " + curPlayer + " " + turn.getKey() + " " + turn.getValue() + "\n");

                    log.error("Invalid turn" + curPlayer);
                    return new GameResult(GameStatus.INVALID_TURN, curPlayer);
                }
                writeToLog("Turn " + curPlayer + " " + turn.getKey() + " " + turn.getValue() + "\n");

                playerToSendTurns.get(anotherPlayer - 1).add(turn);

                processTurn(turn.getKey(), turn.getValue(), curPlayer);

                writeToLog("\n");
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        writeToLog(map[i][j].toString());
                        writeToLog(" ");
                    }
                    writeToLog("\n");
                }
                writeToLog("\n");
            }

            if (firstValidTurns.isEmpty() && secondValidTurns.isEmpty()) {
                hasTurn = false;
            }
            curPlayer = 3 - curPlayer;
        } while (hasTurn);

        int firstType = 0, secondType = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (map[i][j] == 1) {
                    firstType++;
                } else {
                    secondType++;
                }
            }
        }

        if (firstType > secondType) {
            clients[0].sendMessage("win");
            clients[1].sendMessage("lose");
        } else if (firstType < secondType) {
            clients[0].sendMessage("lose");
            clients[1].sendMessage("win");
        } else {
            clients[0].sendMessage("draw");
            clients[1].sendMessage("draw");
        }

        writeToLog("Result : " + firstType + " vs " + secondType + "\n");

        log.info("End testing game");

        return new GameResult(firstType, secondType);
    }

    private void acceptConnections() throws IOException {
        log.info("Begin listen on port " + port);
        acceptSocket = new ServerSocket(port);
        for (int i = 0; i < 2; i++) {
            Socket clientSocket = acceptSocket.accept();
            clients[i] = new GameClient(clientSocket, new StringProtocol(), STRATEGY_TIMEOUT);
        }
        connectionsAccepted = true;
        log.info("All clients have connected");
    }


    public void close() {
        try {

            out.flush();
            out.close();

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
