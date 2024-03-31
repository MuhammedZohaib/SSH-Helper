import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.Scanner;

class SockClient {
  static Socket sock = null;
  static String host = "localhost";
  static int port = 8888;
  static OutputStream out;
  static ObjectOutputStream os;
  static DataInputStream in;

  public static void main (String args[]) {
    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      connect(host, port);
      System.out.println("Client connected to server.");
      boolean requesting = true;

      while (requesting) {
        System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - addmany, 4 - roller, 5 - inventory (0 to quit)");
        Scanner scanner = new Scanner(System.in);
        int choice = Integer.parseInt(scanner.nextLine());

        JSONObject json = new JSONObject();

        switch(choice) {
          case 0:
            System.out.println("Choose quit. Thank you for using our services. Goodbye!");
            requesting = false;
            break;
          case 1:
            System.out.println("Choose echo, which String do you want to send?");
            String message = scanner.nextLine();
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            int num1 = Integer.parseInt(scanner.nextLine());
            System.out.println("Enter second number:");
            int num2 = Integer.parseInt(scanner.nextLine());
            json.put("type", "add");
            json.put("num1", num1);
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose addmany, enter as many numbers as you like, when done choose 0:");
            JSONArray array = new JSONArray();
            int num;
            do {
              num = Integer.parseInt(scanner.nextLine());
              array.put(num);
              System.out.println("Got your " + num);
            } while (num != 0);
            json.put("type", "addmany");
            json.put("nums", array);
            break;
          case 4:
            System.out.println("Choose roller, enter number of dice:");
            int dieCount = Integer.parseInt(scanner.nextLine());
            System.out.println("Enter number of faces per die:");
            int faces = Integer.parseInt(scanner.nextLine());
            json.put("type", "roller");
            json.put("dieCount", dieCount);
            json.put("faces", faces);
            break;
          case 5:
            System.out.println("Choose inventory, enter task (add, view, buy):");
            String task = scanner.nextLine();
            json.put("type", "inventory");
            json.put("task", task);
            if (task.equals("add")) {
              System.out.println("Enter product name:");
              String productName = scanner.nextLine();
              System.out.println("Enter quantity:");
              int quantity = Integer.parseInt(scanner.nextLine());
              json.put("productName", productName);
              json.put("quantity", quantity);
            } else if (task.equals("buy")) {
              System.out.println("Enter product name:");
              String productName = scanner.nextLine();
              System.out.println("Enter quantity to buy:");
              int quantity = Integer.parseInt(scanner.nextLine());
              json.put("productName", productName);
              json.put("quantity", quantity);
            }
            break;
        }

        if (!requesting) {
          continue;
        }

        os.writeObject(json.toString());
        os.flush();

        String response = in.readUTF();
        JSONObject res = new JSONObject(response);

        System.out.println("Got response: " + res);
        if (res.getBoolean("ok")) {
          if (res.has("type")) {
            if (res.getString("type").equals("echo")) {
              System.out.println(res.getString("echo"));
            } else if (res.getString("type").equals("roller")) {
              System.out.println("Rolled dice: " + res.getJSONObject("result"));
            } else if (res.getString("type").equals("inventory")) {
              JSONArray inventory = res.getJSONArray("inventory");
              System.out.println("Inventory:");
              for (int i = 0; i < inventory.length(); i++) {
                JSONObject item = inventory.getJSONObject(i);
                System.out.println(item.getString("product") + ": " + item.getInt("quantity"));
              }
            } else {
              System.out.println("Result: " + res.getInt("result"));
            }
          }
        } else {
          System.out.println("Error: " + res.getString("message"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void connect(String host, int port) throws IOException {
    sock = new Socket(host, port);
    out = sock.getOutputStream();
    os = new ObjectOutputStream(out);
    in = new DataInputStream(sock.getInputStream());
  }
}
