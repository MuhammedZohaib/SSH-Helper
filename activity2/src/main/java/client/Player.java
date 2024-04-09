package client;

public class Player implements Comparable<Player> {

    private final int wins;
    private final String name;

    public Player(String name, int wins){
      this.wins = wins;
      this.name = name;
    }

    public int getWins(){
      return wins;
    }

    // override equals and hashCode
    @Override
    public int compareTo(Player player) {
        return player.getWins() - this.wins;
    }

    @Override
       public String toString() {
            return ("\n" +this.wins + ": " + this.name);
       }
}