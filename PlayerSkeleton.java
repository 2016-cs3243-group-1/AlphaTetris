import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;

public class PlayerSkeleton {

    public static double[] DEFAULT_WEIGHTS = {
            -0.11889029514812746,   // land height
            0.17617469347875242,    // row clear
            -0.07049774628211491,   // row breaks
            -0.2284616211183528,    // column breaks
            -0.2624911582307603,    // hole_idx
            -0.09943344141832017,   // depth_idx
            -0.04405104432357203    // pile_height
    };

    public static int WEIGHTS_LENGTH = DEFAULT_WEIGHTS.length;

    //implement this function to have a working system
    public static int pickMove(State s, int[][] legalMoves, double[] weights) {
        double[] evaluate_moves = new double[legalMoves.length];
        int move = 0;

        for (int i = 0; i < evaluate_moves.length; i++) {
            evaluate_moves[i] = evaluateMoves(s, legalMoves[i], weights);

            if ((i > 0) && (evaluate_moves[i] > evaluate_moves[move]))
                move = i;
        }
        return move;
    }

    public static double evaluateMoves(State s, int[] move, double[] weights) {

        int completed = -1;
        int orient = move[State.ORIENT];
        int slot = move[State.SLOT];
        int next_piece = s.getNextPiece();
        int turn = s.getTurnNumber() + 1;
        int rows_cleared = 0;

        int[] top = s.getTop();
        int[] temp_top = new int[top.length];
        int[][] field = s.getField();
        int[][] field_temp = new int[field.length][field[0].length];
        int[][] pWidth = State.getpWidth();
        int[][] pHeight = State.getpHeight();
        int[][][] pTop = State.getpTop();
        int[][][] pBottom = State.getpBottom();

        System.arraycopy(top, 0, temp_top, 0, top.length);

        for (int i = 0; i < field.length; i++) {
            System.arraycopy(field[i], 0, field_temp[i], 0, field[0].length);
        }

        top = temp_top;
        field = field_temp;

        // height if the first column makes contact
        int height = top[slot] - pBottom[next_piece][orient][0];

        // for each column beyond the first in the piece
        for (int c = 1; c < pWidth[next_piece][orient]; c++) {
            height = Math.max(height, top[slot + c] - pBottom[next_piece][orient][c]);
        }

        // check if it hits the top
        // game ends
        if (height + pHeight[next_piece][orient] < State.ROWS) {
            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[next_piece][orient]; i++) {
                // from bottom to top of brick
                for (int h = height + pBottom[next_piece][orient][i]; h < height + pTop[next_piece][orient][i]; h++) {
                    field[h][i + slot] = turn;
                }
            }

            // adjust top
            for (int c = 0; c < pWidth[next_piece][orient]; c++) {
                top[slot + c] = height + pTop[next_piece][orient][c];
            }

            // check for full rows - starting at the top
            for (int r = height + pHeight[next_piece][orient] - 1; r >= height; r--) {
                // check all columns in the row
                boolean full = true;

                for (int c = 0; c < State.COLS; c++) {
                    if (field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }

                //if the row was full
                //remove and slide down
                if (full) {
                    rows_cleared++;
                    completed++;

                    //for each column
                    for (int c = 0; c < State.COLS; c++) {
                        //slide down all bricks
                        for (int i = r; i < top[c]; i++) {
                            field[i][c] = field[i + 1][c];
                        }

                        // lower the top
                        top[c]--;

                        while (top[c] >= 1 && field[top[c] - 1][c] == 0) {
                            top[c]--;
                        }
                    }
                }
            }
        } else {
            return -9999;  
        }

        return computeLandingHeight(pHeight[next_piece][orient], height) * weights[0]
                + rows_cleared * weights[1] + getRowBreaks(field)
                * weights[2] + getColumnBreaks(field)
                * weights[3] + getNumberOfHoles(top, field)
                * weights[4] + getWellHole(field)
                * weights[5] + getMax(top) * weights[6];
    }

    private static int getMax(int[] top) {
        int max = top[0];

        for (int t : top) {
            max = Math.max(max, t);
        }

        return max;
    }

    public static int computeLandingHeight(int pHeight, int height) {
        int land_height;
        land_height = height + ((pHeight - 1) / 2);
        return land_height;
    }

    public static int getNumberOfHoles(int[] top, int[][] board) {
        int holes = 0;
        for (int j = 0; j < State.COLS; j++) {
            for (int i = 0; i < top[j] - 1; i++) {
                if (board[i][j] == 0)
                    holes++;
            }
        }
        return holes;
    }

    // Get the number of transitions between a filled and empty cell in a row
    public static int getRowBreaks(int[][] board) {
        int row_transition = 0;
        int previous_state = 1;
        for (int row = 0; row < State.ROWS - 1; row++) {
            for (int col = 0; col < State.COLS; col++) {
                if ((board[row][col] != 0) != (previous_state != 0)) {
                    row_transition++;
                }
                previous_state = board[row][col];
            }
            if (board[row][State.COLS - 1] == 0) {
                row_transition++;
            }
            previous_state = 1;
        }
        return row_transition;
    }

    // Get the number of transitions between a filled and empty cell in a column
    public static int getColumnBreaks(int[][] board) {
        int column_transition = 0;
        int previous_state = 1;
        for (int col = 0; col < State.COLS; col++) {
            for (int row = 0; row < State.ROWS - 1; row++) {
                if ((board[row][col] != 0) != (previous_state != 0)) {
                    column_transition++;
                }
                if (board[State.ROWS - 1][col] == 0) {
                    column_transition++;
                }
                previous_state = board[row][col];
            }
            previous_state = 1;
        }
        return column_transition;
    }

    // Get a 'well' hole with filled cell on the left and right
    public static int getWellHole(int[][] board) {
        int depth = 0;

        for (int c = 1; c < State.COLS - 1; c++) {
            for (int r = State.ROWS - 2; r >= 0; r--) {
                if (board[r][c] == 0 && board[r][c - 1] != 0 && board[r][c + 1] != 0) {
                    depth++;

                    for (int i = r - 1; i >= 0; i--) {
                        if (board[i][c] == 0) {
                            depth++;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        for (int r = State.ROWS - 2; r >= 0; r--) {
            if (board[r][0] == 0 && board[r][1] != 0) {
                depth++;

                for (int i = r - 1; i >= 0; i--) {
                    if (board[i][0] == 0) {
                        depth++;
                    } else {
                        break;
                    }
                }
            }
        }
        for (int r = State.ROWS - 2; r >= 0; r--) {
            if (board[r][State.COLS - 1] == 0 && board[r][State.COLS - 2] != 0) {
                depth++;

                for (int c = r - 1; c >= 0; c--) {
                    if (board[c][State.COLS - 1] == 0) {
                        depth++;
                    } else {
                        break;
                    }
                }
            }
        }
        return depth;
    }

    public static int[] run(double[] weights, boolean showResults, int maxTurns) {
        State s = new State();
        while(!s.hasLost() && (maxTurns == 0 || maxTurns > s.getTurnNumber())) {
            s.makeMove(PlayerSkeleton.pickMove(s,s.legalMoves(), weights));
        }
        // Prints the number of rows cleared and the turn number
        if (showResults) {
            System.out.println(s.getRowsCleared() + " " + s.getTurnNumber());
        }
        return new int [] { s.getRowsCleared(), s.getTurnNumber() };
    }

    public static void run() {
        run(PlayerSkeleton.DEFAULT_WEIGHTS, true, 0);
    }

    // ==============================================
    // Modified main method
    // ==============================================
    public static void main(String[] args) {
        // java PlayerSkeleton optimize <NUMBER OF GENERATIONS> <STARTING MAX TURNS>
        if(args.length >= 2 && args[0].equals("optimize")) {
            // Run genetic algorithm
            try {
                GeneticAlgorithm ga = new GeneticAlgorithm();
                int generations = Integer.parseInt(args[1]);
                int maxTurns = Integer.parseInt(args[2]);
                ga.optimizeWeights(generations, maxTurns);
                System.out.println("end");
                System.exit(0);
            } catch (NumberFormatException e) {
                System.out.println("argument has to be an integer!");
            }
        } else {
            // run game
            PlayerSkeleton.run();
        }
    }
}

class Agent implements Comparable<Agent> {

    private double[] weights;
    private int rowsCompleted;
    private int turnsPlayed;
    private int numGames;

    public Agent(double[] populationWeights) {
        this.weights = populationWeights;
        reset();
    }

    public void reset() {
        this.rowsCompleted = 0;
        this.turnsPlayed = 0;
        this.numGames = 0;
    }

    public double[] getWeights() {
        return this.weights;
    }

    public int getRowsCompleted() {
        return this.rowsCompleted;
    }

    public int getTurnsPlayed() {
        return this.turnsPlayed;
    }

    public int gamesPlayed() {
        return this.numGames;
    }

    public void updateScore(int rowsCompleted, int turnsPlayed) {
        this.rowsCompleted += rowsCompleted;
        this.turnsPlayed += turnsPlayed;
        numGames ++;
    }

    public String getResultsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Average Rows Completed: ");
        sb.append(rowsCompleted / numGames);
        sb.append(", Average Turns Played: ");
        sb.append(turnsPlayed / numGames);
        sb.append(", Num Games Played: ");
        sb.append(numGames);
        return sb.toString();
    }

    public void setWeights(double[] newWeights) {
        this.weights = newWeights;
    }

    public void mutateOneWeight(int index, double modifier) {
        this.weights[index] *= modifier;
        this.weights = normaliseWeights(this.weights);
    }

    private double[] normaliseWeights(double[] weights) {
        double sumWeights = 0;
        for (int i = 0; i < weights.length; i++) {
            sumWeights += Math.abs(weights[i]);
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (weights[i] / sumWeights);
        }
        return weights;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(getResultsString());
        s.append("\n");
        for (double d : weights) {
            s.append(d);
            s.append(", ");
        }
        s.append("\n");
        return s.toString();
    }

    @Override
    // Sorted in increasing order number of rows cleared
    // Number of turns survived is used as a tiebreaker
    public int compareTo(Agent o) {
        if (o.getRowsCompleted() > this.getRowsCompleted()) {
            return -1;
        } else if (o.getRowsCompleted() < this.getRowsCompleted()) {
            return 1;
        } else {
            if (o.getTurnsPlayed() > this.getTurnsPlayed()) {
                return -1;
            } else if (o.getTurnsPlayed() < this.getTurnsPlayed()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

class GameTask implements Callable<int[]> {

    int index;
    double[] weights;
    int maxTurns;

    public GameTask(int index, Agent agent, int maxTurns) {
        this.index = index;
        this.weights = agent.getWeights();
        this.maxTurns = maxTurns;
    }

    @Override
    public int[] call() throws Exception {
        int[] results = PlayerSkeleton.run(weights, false, maxTurns);
        return new int[] { index, results[0], results[1] };
    }
}

class GeneticAlgorithm {

    // ==============================================
    // Default values
    // ==============================================
    private int WORKERS_POOL =  Runtime.getRuntime().availableProcessors(); // threading stuff
    private int POPULATION_SIZE = 100; // number of agents
    private int GAMES = 20; // number of games each agent plays
    private int MAX_TURNS = 1000;
    private double DEEPEN = 0.1; // if average rows cleared is within % of top score
    private double SELECTION = 0.1; // for tournament selection
    private double CULLING = 0.3;
    private double MUTATION_RATE = 0.05;
    private double MUTATION_DELTA = 0.2;
    private int NUM_WEIGHTS = PlayerSkeleton.WEIGHTS_LENGTH;

    private ArrayList<Agent> population;
    private long generationTotalRowsCleared;
    ExecutorService pool;
    CompletionService<int[]> completionService;

    private static Logger logger;

    // ==============================================
    // Constructor
    // ==============================================
    public GeneticAlgorithm() {
        population = seedPopulation();
        pool = Executors.newFixedThreadPool(WORKERS_POOL);
        completionService = new ExecutorCompletionService<int[]>(pool);
        logger = Logger.getLogger("GeneticAlgorithm");
        logger.setUseParentHandlers(false);

        FileHandler fh;

        try {
            fh = new FileHandler("output.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==============================================
    // Driver methods
    // ==============================================
    public void runGames() {
        int totalGames = GAMES * population.size();
        int gamesCompleted = 0;
        for (int i = 0; i < GAMES; i++) {
            for (int j = 0; j < population.size(); j++) {
                Agent agent = population.get(j);
                agent.reset();
                completionService.submit(new GameTask(j, population.get(j), MAX_TURNS));
            }
        }
        long startTime = System.currentTimeMillis();
        long stopTime;
        long averageTimePerGame;
        float timeRemaining;
        for (int i = 0; i < (GAMES * population.size()); i++){
            try {
                int[] result = completionService.take().get();
                population.get(result[0]).updateScore(result[1], result[2]);
                generationTotalRowsCleared += result[1];
                gamesCompleted ++;
                stopTime = System.currentTimeMillis();
                averageTimePerGame = (stopTime - startTime) / gamesCompleted;
                timeRemaining = (float) ((averageTimePerGame * (totalGames - gamesCompleted)) / 1000) / 60;
//                System.out.printf("Currently processing %s of %s games. Estimated Time Remaining: %.2f Minutes.\r",
//                        gamesCompleted, totalGames, timeRemaining);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void report(int gen) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nGeneration ");
        sb.append(Integer.toString(gen));
        sb.append(", Search Depth: ");
        sb.append(MAX_TURNS);
        sb.append(" turns\nTop 10 Results:\n");
        for (int i = 1; i <= 5; i++) {
            sb.append(population.get(population.size() - i));
        }
        sb.append("\nTop 4 runner ups\n");
        for (int j = POPULATION_SIZE-2; j > POPULATION_SIZE-6; j--) {
            sb.append(population.get(j).getResultsString());
            sb.append("\n");
        }
        sb.append("Population Average Rows Completed: ");
        sb.append(generationTotalRowsCleared / (GAMES * population.size()));
        sb.append(", Total Games Played: ");
        sb.append(GAMES * population.size());
        sb.append("\n");
        logger.info(sb.toString());
    }

    public void deepenSearch() {
        float averageRowsCleared = generationTotalRowsCleared / (GAMES * population.size());
        float topAverageRowsCleared = population.get(population.size() - 1).getRowsCompleted() / GAMES;
        if (((topAverageRowsCleared - averageRowsCleared) / topAverageRowsCleared) <  DEEPEN) {
            MAX_TURNS *= 2;
        }
    }

    // The point of entry for GeneticAlgorithm
    public void optimizeWeights(int generations, int maxTurns) {
        MAX_TURNS = maxTurns;
        logger.info("Running genetic algorithm with " + generations + " generations and " + maxTurns + " starting max turns");
        for (int i = 1; i <= generations; i++) {
            generationTotalRowsCleared = 0;
            runGames();
            Collections.sort(population);
            report(i);
            deepenSearch();
            nextGeneration();
        }
        pool.shutdown();
    }

    public ArrayList<Agent> seedPopulation() {
        ArrayList<Agent> agentPopulation = new ArrayList<Agent>(POPULATION_SIZE);
        double[][] presetPopulationWeights = { // Defines the weights of agents in the initial population
//                {-0.10461710553782902, 0.1636787909859012, -0.07762865196546648, -0.24290354224274174, -0.23672018475759599, -0.10996400562919922, -0.06448771888126632},
//                {-0.11741727538659695, 0.18517925976192556, -0.06514716576913131, -0.225153790321674, -0.2796538696383896, -0.09259548149778675, -0.03485315762449593},
//                {-0.11883381275308547, 0.18032482985192158, -0.0683131042859156, -0.22771843084331625, -0.255099842371379, -0.10194603505980349, -0.04776394483457872},
//                {-0.11466481619868854, 0.17906903795497178, -0.07335909488648606, -0.2289938236986478, -0.26241137434208617, -0.09796617248777267, -0.043535680431346965},
//                {-0.1199958257271216, 0.1593630545379815, -0.07328409786606721, -0.2339853931325515, -0.275737750051736, -0.0998922389491572, -0.03774163973538498}
        };
        int numRepetitions = 10; // Number of agents for each set of pre-defined weights

        for (double[] weights : presetPopulationWeights) {
            for (int i = 0; i < numRepetitions; i++) {
                agentPopulation.add(new Agent(weights));
            }
        }

        for (int i = 0; i < POPULATION_SIZE - numRepetitions * presetPopulationWeights.length; i++) {
            agentPopulation.add(new Agent(generateWeights()));
        }

        assert agentPopulation.size() == POPULATION_SIZE;
        return agentPopulation;
    }

    public double[] generateWeights() {
        double[] populationWeights = new double[NUM_WEIGHTS];

        // Modified so that the weight generation is a smarter
        // e.g. aggregate column heights should never be +ve
        populationWeights[0] = randRange(-1, 0);
        populationWeights[1] = randRange(0, 1);
        populationWeights[2] = randRange(-1, 0);
        populationWeights[3] = randRange(-1, 0);
        populationWeights[4] = randRange(-1, 0);
        populationWeights[5] = randRange(-1, 0);
        populationWeights[6] = randRange(-1, 0);

        //		for (int i = 0; i < NUM_WEIGHTS; i++) {
        //			populationWeights[i] = randRange(-1, 1);
        //		}

        return normaliseWeights(populationWeights);
    }

    public double randRange(double lower, double upper) {
        Random random = new Random();
        return random.nextDouble() * (upper - lower) + lower;
    }

    public double[] normaliseWeights(double[] weights) {
        double sumWeights = 0;
        for (int i = 0; i < weights.length; i++) {
            sumWeights += Math.abs(weights[i]);
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (weights[i] / sumWeights);
        }
        return weights;
    }

    // ====================================================
    // Biology related functions (aka eugenics huehuehue) (>huehuehue i will end you)
    // ====================================================
    public void nextGeneration() {
        // Select the last 10 percentile and replace them
        int numCulled = (int) (CULLING * POPULATION_SIZE);
        for (int i = 0; i < numCulled; i++) {
            try {
                // Replace the the ith agent with a new agent
                population.set(i, createOffspring());
            } catch (Exception e) {
                e.printStackTrace(); // by right this shouldn't happen, but just to be safe
            }
        }
    }

    public Agent createOffspring() {
        Random random = new Random();
        ArrayList<Agent> parents = selectParents(population);
        Agent offspring = crossover(parents);
        if (random.nextDouble() < MUTATION_RATE) {
            mutate(offspring);
        }
        return offspring;
    }

    // Returns top 2 agents from randomly selected pool (tournament selection)
    public ArrayList<Agent> selectParents(ArrayList<Agent> agentPopulation) {
        ArrayList<Agent> parents = new ArrayList<Agent>();
        Random random = new Random();
        ArrayList<Agent> randomAgents = new ArrayList<Agent>();
        for (int i = 0; i < (int) (POPULATION_SIZE * SELECTION); i++) {
            randomAgents.add(population.get(random.nextInt(POPULATION_SIZE)));
        }
        Collections.sort(randomAgents);

        // Get the last 2 indexes from randomIndexes
        Agent p1 = randomAgents.get(randomAgents.size()-1);
        Agent p2 = randomAgents.get(randomAgents.size()-2);

        parents.add(p1);
        parents.add(p2);
        return parents;
    }

    // Number of rows cleared is a proxy for fitness
    public Agent crossover(ArrayList<Agent> parents) {
        Agent p1 = parents.get(0);
        Agent p2 = parents.get(1);

        double fitness1 = p1.getTurnsPlayed();
        double fitness2 = p2.getTurnsPlayed();

        double inheritFromP1 = fitness1 / (fitness1 + fitness2);
        double inheritFromP2 = fitness2 / (fitness1 + fitness2);

        double[] offspringWeights = new double[NUM_WEIGHTS];
        for (int i = 0; i < NUM_WEIGHTS; i++) {
            offspringWeights[i] = (inheritFromP1 * p1.getWeights()[i]) + (inheritFromP2 * p2.getWeights()[i]);
        }
        return new Agent(normaliseWeights(offspringWeights));
    }

    public void mutate(Agent offspring) {
        Random random = new Random();
        int weightIndex = random.nextInt(offspring.getWeights().length);
        double mutationModifier = 1 + randRange(MUTATION_DELTA, -MUTATION_DELTA);
        offspring.mutateOneWeight(weightIndex, mutationModifier);
    }

    // ==============================================
    // Debug methods
    // ==============================================
    public void printAgents(ArrayList<Agent> agents) {
        for (Agent a : agents) {
            System.out.print(a);
            System.out.println();
        }
    }

    public void printResults(ArrayList<ArrayList<double[]>> ranks) {
        for (ArrayList<double[]> indiv : ranks) {
            for (double[] e : indiv) {
                printArray(e);
            }
            System.out.println();
        }
    }

    public void printArray(double[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
    }
}


