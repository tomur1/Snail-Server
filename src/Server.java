
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Server {

    //ids start from 1 because 0 is reserved for communication for everyone
    int IDs = 1;
    ArrayList<Integer> idList = new ArrayList<>();

    //output streams to unity game clients
    AtomicReference<ArrayList<OutputStream>> outs = new AtomicReference<>();
    AtomicBoolean canStart = new AtomicBoolean(false);

    void start() {
        System.out.println("Starting the server...");
        outs.set(new ArrayList<>());
        try (var listener = new ServerSocket(59898)) {
            var pool = Executors.newFixedThreadPool(20);
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (canStart.get()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (outs.get().size() == 1) {
                                System.out.println("Starting game");
                                String ids = "";
                                for (Integer ID :
                                        idList) {
                                    ids += ID;
                                    ids += ",";
                                }
                                tellEveryone(0 + ",NEWGAME," + idList.size() + "," + ids);
                                //listener for information from game to players
                                return;
                            }
                        }
                    }
                }
            });

            while (true) {
                Socket client = listener.accept();
                outs.get().add(client.getOutputStream());
                pool.execute(new ClientHandling(client));
                //this line assigns id
                int newId = IDs++;
                tellEveryone((newId) + "");
                idList.add(newId);
                canStart.set(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void tellEveryone(String message) {
        for (OutputStream out :
                outs.get()) {
            PrintWriter pw = new PrintWriter(out);
            pw.write(message + "\n");
            pw.flush();
            System.out.println(message);
        }
    }

    private class ClientHandling implements Runnable {
        private Socket socket;

        ClientHandling(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket);
            try {
                var in = new Scanner(socket.getInputStream());
                while (in.hasNextLine()) {
                    performMove(in.nextLine());
            }
            } catch (Exception e) {
                System.out.println("Error:" + socket);
            } finally {
                try {
                    outs.get().remove(socket.getOutputStream());
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Closed: " + socket);
            }
        }

        private void performMove(String ID) {
            tellEveryone(0 + "," + "MOVE," + ID);
        }

    }
}
