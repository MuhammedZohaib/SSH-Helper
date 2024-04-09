package client;

import java.io.*;
import java.net.Socket;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

public class SockBaseClient {
    public static void main(String[] args) throws Exception {
        String host = null;
        int port = -1;

        if (argsCheck(args)) {
            host = args[0];
            port = getPort(args);
        }

        boolean displayMenu = true;
        boolean hasDisconnect = false;
        boolean exitGame = false;

        try (Socket serverSock = new Socket(host, port); OutputStream out = serverSock.getOutputStream(); InputStream in = serverSock.getInputStream()) {
            System.out.println("Please provide your name for the server.");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String strToSend = stdin.readLine();

            Request request = null;
            Response response = null;

            if (serverSock.isConnected()) {
                request = buildNameReq(strToSend);
                request.writeDelimitedTo(out);
                response = getResponse(in);
            } else {
                serverSock.close();
            }

            if (response != null) {
                if (response.getResponseType() == Response.ResponseType.WELCOME) {
                    System.out.println(response.getMessage());
                }
            }
            while (!hasDisconnect) {
                int selection = -99;
                if (displayMenu) {
                    while (selection == -99) {
                        System.out.println("* \nWhat would you like to do? \n 1 - leaderboard \n 2 - play game \n 3 - quit");
                        String menuSelection = stdin.readLine();
                        try {
                            int parseSelection = Integer.parseInt(menuSelection);
                            if (checkBounds(parseSelection)) {
                                selection = parseSelection;

                                switch (selection) {
                                    case 1:
                                        request = buildLeaderReq();
                                        break;
                                    case 2:
                                        request = buildGameReq();
                                        displayMenu = false;
                                        break;
                                    case 3:
                                        request = buildQuitReq();
                                        hasDisconnect = true;
                                        exitGame = true;
                                        break;

                                    default:
                                        throw new IllegalStateException("Unexpected value: " + selection);
                                }
                            }
                        } catch (IOException | NumberFormatException e) {
                            System.out.println("Enter a valid number");
                        }
                    }
                }
                if (request != null && response != null && serverSock.isBound()) {
                    request.writeDelimitedTo(out);
                    response = getResponse(in);
                } else {
                    serverSock.close();
                }
                if (response != null) {
                    switch (response.getResponseType()) {
                        case LEADERBOARD:
                            for (Leader lead : response.getLeaderboardList()) {
                                System.out.println(lead.getName() + ":" + lead.getWins());
                            }
                            break;
                        case TASK:
                            System.out.println(response.getPhrase());
                            System.out.println("Enter your Guess (single letter): ");
                            String guess = stdin.readLine().trim();

                            while (guess.length() != 1 || !Character.isLetter(guess.charAt(0))) {
                                System.out.println("Invalid guess. Please enter a single letter.");
                                System.out.println("Enter your Guess (single letter): ");
                                guess = stdin.readLine().trim();
                            }

                            if (isExit(guess)) {
                                request = buildQuitReq();
                                hasDisconnect = true;
                                exitGame = true;
                            } else {
                                request = buildTaskReq(guess);
                            }

                            if (response.getEval()) {
                                System.out.println(response.getPhrase());
                            } else {
                                System.out.println(response.getTask());
                            }
                            break;

                        case WON:
                            System.out.println(response.getMessage());
                            System.out.println(response.getPhrase());
                            displayMenu = true;
                            break;
                        case ERROR, BYE:
                            System.out.println(response.getMessage());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + response.getResponseType());
                    }

                }
            }
        }
    }

    private static boolean argsCheck(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        return true;
    }

    private static int getPort(String[] args) {
        int port = -1;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }
        return port;
    }

    private static Request buildNameReq(String name) {
        return Request.newBuilder().setOperationType(Request.OperationType.NAME).setName(name).build();
    }

    private synchronized static Response getResponse(InputStream in) throws IOException {
        return Response.parseDelimitedFrom(in);
    }

    private static boolean checkBounds(int i) throws IOException {
        return i > 0 && i < 4;
    }


    private static Request buildLeaderReq() {
        Request op;
        op = Request.newBuilder().setOperationType(Request.OperationType.LEADERBOARD).build();
        return op;
    }


    private static Request buildGameReq() {
        Request op;
        op = Request.newBuilder().setOperationType(Request.OperationType.START).build();
        return op;
    }


    private static Request buildQuitReq() {
        Request op;
        op = Request.newBuilder().setOperationType(Request.OperationType.QUIT).build();
        return op;
    }


    private static boolean isExit(String input) {
        return input.equalsIgnoreCase("exit");
    }


    private static Request buildTaskReq(String guess) {
        Request op;
        op = Request.newBuilder()
                .setOperationType(Request.OperationType.GUESS)
                .setGuess(guess)
                .build();
        return op;
    }
}
