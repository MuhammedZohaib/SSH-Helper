package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

public class SockBaseServer implements Runnable{
    static String logFilename = "logs.txt";
    static String leaderBoardFilename = "leader_log.txt";
    static Response.Builder leaderBoard = Response.newBuilder().setResponseType(Response.ResponseType.LEADERBOARD);
    Game game;
    Socket clientSocket;
    InputStream in = null;
    OutputStream out = null;
    String name = "";


    public SockBaseServer(Socket sock, Game game) {
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e) {
            System.out.println("Error in constructor: " + e);
        }
    }

    public static void main(String[] args) {
        Game game = new Game();

        initLeaderFile();
        initLeaderboard();
        int port = 9099;

        if (argsCheck(args)) {
            port = getPort(args);
        }
        ServerSocket serv = connectServerSocket(port);

        while (true) {
            try {
                Socket clientSocket = serv.accept();
                Runnable serverRunnable = new SockBaseServer(clientSocket, game);
                Thread serverThread = new Thread(serverRunnable);
                serverThread.start();
            } catch (IOException e) {
                System.out.println("Exception in main method: "+e.getMessage());
            }
        }

    }

    private static void initLeaderFile() {
        try {
            File leaderFile = new File(leaderBoardFilename);
            if (leaderFile.createNewFile()) {
                System.out.println("File created: " + leaderFile.getName());
            }
        } catch (IOException e) {
            System.out.println("An error occurred in initLeaderFile Method: "+e.getMessage());
        }
    }

    private static void initLeaderboard() {
        String line;
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(leaderBoardFilename)); BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty()) {
                    String[] lineArray = line.split(",");
                    String element1 = lineArray[0];
                    String element2 = lineArray[1];

                    int wins = 0;
                    try {
                        wins = Integer.parseInt(element2);
                    } catch (NumberFormatException e) {
                        System.out.println("Inconsistent Data Found in Leaderboard File"+e.getMessage());
                    }
                    addEntry(element1, wins);
                }
            }
        } catch (IOException e) {
            System.out.println("IO Exception in initLeaderboard method: "+e.getMessage());
        }
    }

    private static boolean argsCheck(String[] args) {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }
        return true;
    }

    private static int getPort(String[] args) {
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }
        return port;
    }

    private static ServerSocket connectServerSocket(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println("Connection Exception: "+e.getMessage());
            System.exit(2);
        }
        return serverSocket;
    }

    private synchronized static void addEntry(String name, int wins) {
        Leader leader = Leader.newBuilder().setName(name).setWins(wins).build();
        leaderBoard.addLeaderboard(leader);
    }

    @Override
    public void run() {
        try {
            boolean quit = false;
            while (!quit) {

                Request request;
                Response response;

                if (clientSocket.isConnected()) {
                    request = Request.parseDelimitedFrom(in);
                } else {
                    break;
                }

                switch (request.getOperationType()) {
                    case NAME:
                        name = request.getName();
                        writeToLog(name, Message.CONNECT);
                        response = buildGreetingRes(name);
                        break;
                    case LEADERBOARD:
                        leaderBoard.getLeaderboardList();
                        response = buildLeaderRes();
                        break;
                    case START:
                        game.newGame();
                        response = buildNewRes();
                        writeToLog(name, Message.START);
                        break;
                    case GUESS:
                        String guessedLetter = request.getGuess().toUpperCase();
                        if (guessedLetter.length() != 1 || !Character.isLetter(guessedLetter.charAt(0))) {
                            response = buildErrorRes();
                        } else {
                            char guessChar = guessedLetter.charAt(0);
                            game.markGuess(guessChar);
                            response = buildTaskRes(game.getPhrase().contains("_"));
                        }
                        break;
                    case QUIT:
                        response = buildByeRes(name);
                        quit = true;
                        writeToLog(name, Message.WIN);
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + request);
                }

                if (response.getResponseType().equals(Response.ResponseType.WON)) {
                    game.setWon();
                }
                response.writeDelimitedTo(out);
            }

        } catch (Exception ex) {
            System.out.println("Client " + name + " connection has been terminated.");
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.out.println("Client connection has been closed");
            }
        }
    }

    private synchronized static void writeToLog(String name, Message message) {
        try {
            Logs.Builder logs = readLogFile(logFilename);
            Date date = java.util.Calendar.getInstance().getTime();
            logs.addLog(date + ": " + name + " - " + message);
            System.out.println(date + ": " + name + " - " + message);
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();
            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    private synchronized Response buildGreetingRes(String name) {
        return Response.newBuilder().setResponseType(Response.ResponseType.WELCOME).setMessage("Hello " + name + " and welcome.").build();
    }

    private synchronized Response buildLeaderRes() {
        return leaderBoard.build();
    }

    private synchronized Response buildNewRes() {
        return Response.newBuilder().setResponseType(Response.ResponseType.TASK).setPhrase(game.getPhrase()).build();
    }

    public synchronized static void writeToLeaderLog(String name) {
        try {
            BufferedReader file = new BufferedReader(new FileReader(leaderBoardFilename));
            StringBuilder inputBuffer = new StringBuilder();
            String line;

            boolean found = false;
            while ((line = file.readLine()) != null) {
                if (line.contains(name)) {
                    String[] contents = line.split(",");
                    int currWins = Integer.parseInt(contents[1]);
                    currWins += 1;
                    line = contents[0] + "," + currWins;
                    found = true;
                }
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }

            if (!found) {
                while ((line = file.readLine()) != null) {
                    inputBuffer.append(line);
                    inputBuffer.append('\n');
                }
                inputBuffer.append(name).append(",1");
            }
            file.close();

            FileOutputStream fileOut = new FileOutputStream(leaderBoardFilename);
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.close();

        } catch (Exception e) {
            System.out.println("Problem reading file.");
        }

    }


    private synchronized Response buildWonRes() {
        return Response.newBuilder().setResponseType(Response.ResponseType.WON).setPhrase(game.getPhrase()).setMessage("Congrats!!! You have guessed the Phrase").build();
    }

    private synchronized Response buildTaskRes(boolean hasHit) {
        if (hasHit) {
            return Response.newBuilder()
                    .setResponseType(Response.ResponseType.TASK)
                    .setPhrase(game.getPhrase())
                    .setEval(true)
                    .setTask("\nCorrect Guess")
                    .build();
        } else {
            boolean gameWon = !game.getPhrase().contains("_");
            if (gameWon) {
                writeToLeaderLog(name);
                return buildWonRes();
            } else {
                return Response.newBuilder()
                        .setResponseType(Response.ResponseType.TASK)
                        .setPhrase(game.getPhrase())
                        .setEval(false)
                        .setTask("\nIncorrect Guess")
                        .build();
            }
        }
    }


    private synchronized Response buildErrorRes() {
        return Response.newBuilder().setResponseType(Response.ResponseType.ERROR).setMessage("Invalid guess. Please enter a single letter.").build();
    }

    private synchronized Response buildByeRes(String name) {
        return Response.newBuilder().setResponseType(Response.ResponseType.BYE).setMessage("Goodbye " + name).build();
    }

    public static Logs.Builder readLogFile(String fileName) throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            return logs.mergeFrom(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            System.out.println(fileName + ": File not found.  Creating a new file.");
            return logs;
        }
    }
}

