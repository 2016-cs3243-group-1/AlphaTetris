import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Game {

    public int populationIdx;
    public double[] weights;

    public Game(int populationIdx, double[] weights){
        this.populationIdx = populationIdx;
        this.weights = weights;
    }
}

class GameWorker extends Thread {

    private final BlockingQueue<Game> queue;
    private GameMaster master;

    public GameWorker(BlockingQueue<Game> queue, GameMaster master){
        this.queue = queue;
        this.master = master;
        setDaemon(true);
    }

    public void run() {
        try {
            while(!isInterrupted()) {
                Game game = queue.take();

                // code to run game here
                System.out.printf("Running game for population idx %s\n", game.populationIdx);

                // return the results
                master.callback(game.populationIdx, 0, 0);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class GameMaster {

    private static int workersCount = 4;
    private GameWorker[] workers = new GameWorker[workersCount];
    private final BlockingQueue<Game> queue;

    public GameMaster() {
        this.queue = new LinkedBlockingQueue<Game>();
        spawnWorkers();
    }

    private void spawnWorkers() {
        for (int i = 0; i < workersCount; i++) {
            workers[i] = new GameWorker(queue, this);
            workers[i].start();
        }
    }

    public synchronized void callback(int populationIdx, int rowsCleared, int turnsPlayed) {
        // callback method that the workers call to return results
        // synchronized is so that everything in this method is considered an atomic process
        // prevents race conditions
        System.out.printf("Received results for population index %s\n", populationIdx);
    }

    private void queueGames() {
        // add games by population idx and weights to the queue using the game object
        System.out.println("Queueing games.");
        for (int i = 0; i < 10; i++) {
            double[] weights = {0.1, 0.2, 0.3};
            Game game = new Game(i, weights);
            queue.add(game);
        }
    }

    public void waitForWorkers() throws InterruptedException {
        // java queues dont have .join so this is a shitty hack
        // not ideal, but since each generation takes a considerable
        // amount of time to process, a maximum of 1 second delay
        // is insignificant. Can lower delay for increased cpu overhead.
        // maybe we can figure out how to do this more elegantly later
        while (!queue.isEmpty()) {
            Thread.sleep(1000);
        }
    }

    public void optimizeWeights(int generations) {
        try {
            for (int i = 0; i < generations; i++) {
                System.out.printf("Starting Generation %s.\n", i);
                queueGames();
                waitForWorkers();
                System.out.println("Processing results and making new generation");
                // rank results
                // generate next generation etc
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

public class ConcurrencyDemo {

    public static void main(String[] args){
        GameMaster gm = new GameMaster();
        gm.optimizeWeights(10);
    }
}