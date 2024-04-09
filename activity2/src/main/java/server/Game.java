package server;

import java.util.*; 
import java.io.*;

/**
 * Class: Game 
 * Description: Game class that can load a phrase
 * Class can be used to hold the persistent state for a game for different threads
 * synchronization is not taken care of .
 * You can change this Class in any way you like or decide to not use it at all
 * I used this class in my SockBaseServer to create a new game and keep track of the current image evenon differnt threads. 
 * My threads each get a reference to this Game
 */

public class Game {
    private int length; // length of phrase
    private char[] originalPhrase; // the original phrase
    private char[] hiddenPhrase; // the hidden phrase
    private List<String> phrases = new ArrayList<String>(); // list of phrases
    private String currentTask;
    private boolean won  = false;


    public Game(){
        currentTask = "";
        length = 0;
        loadPhrases("phrases.txt");
    }

    /**
     * Method loads in a new phrase from the specified file and creates the hidden phrase for it.
     * @return Nothing.
     */
    public  void newGame(){
        currentTask = "Guess a letter";
        getRandomPhrase();
    }

    public void loadPhrases(String filename){
        try{
            File file = new File( Game.class.getResource("/"+filename).getFile().replace("%20", " "));
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                phrases.add(line);
                System.out.println("Added Phrase: " + line);
            }
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("File load error"); // extremely simple error handling, you can do better if you like. 
        }
    }

    // Simple method to load a random phrase, but might load the same phrase twice
    private  void getRandomPhrase(){
        String phrase = "";
        try{
            // loads one random phrase from list
            Random rand = new Random(); 
            int randInt = rand.nextInt(phrases.size());

            phrase = phrases.get(randInt);
            length = phrase.length();

            System.out.println("Starting new game with phrase: " + phrase);

            originalPhrase = new char[length];
            hiddenPhrase = new char[length];

            for (int i = 0; i < length; i++) {
                char curr = phrase.charAt(i);
                originalPhrase[i] = curr;
                if (curr == ' ') {
                    hiddenPhrase[i] = curr;
                } else {
                    hiddenPhrase[i] = '_';
                } 
            }
        }
        catch (Exception e){
            System.out.println("Error generating random phrase"); // extremely simple error handling, you can do better if you like. 
            System.exit(0);
        }

    }

    public void markGuess(char guess) {
        for (int i = 0; i < length; i++) {
            if (originalPhrase[i] == guess) {
                hiddenPhrase[i] = guess;
            }
        }
    }

    public String getPhrase(){
        return String.valueOf(hiddenPhrase);
    }

    public String getTask(){
        return currentTask;
    }

    public void setWon() {
        this.won = true;
    }
}
