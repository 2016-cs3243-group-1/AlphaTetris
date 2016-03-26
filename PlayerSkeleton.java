import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerSkeleton {
    
	private static double[] DEFAULT_WEIGHTS = {
		-0.510066, // Aggregate column heights
		-0.184483, // Bumpiness
		0, // Max height
		-0.6, // Num of holes created
		0.760666 // Num of completed rows
	};

	//implement this function to have a working system
	public static int pickMove(State s, int[][] legalMoves, double[] weights) {
		int move = 0;
		double max = -Double.MAX_VALUE;
		for(int i = 0; i < legalMoves.length; i++) {
			try {
				int[] results = scoreMove(s, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT]);
				double score = 0;
				for(int j = 0; j < weights.length; j++) {
					score += results[j] * weights[j];
				}
				if (score > max) {
					max = score;
					move = i;
				}
			} catch (Exception e) {
				continue;
			}
		}
		return move;
	}

	public static int[] scoreMove(State s, int orient, int slot) throws Exception {
		int[] results = new int[5];

		int[][] field = s.getField();
		int nextPiece = s.getNextPiece();
		int turn = s.getTurnNumber() + 1;
		int[] top = new int[s.getTop().length];
		System.arraycopy(s.getTop(), 0, top, 0, s.getTop().length);
		int[][][] pBottom = State.getpBottom();
		int[][][] pTop = State.getpTop();
		int[][] pHeight = State.getpHeight();
		int[][] pWidth = State.getpWidth();

		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}

		if(height+pHeight[nextPiece][orient] >= State.ROWS) {
			throw new Exception();
		}

		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}

		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}

		results[0] = 0;
		results[2] = 0;
		// Sum heights and find max height
		for(int c = 0; c < State.COLS; c++) {
			results[0] += top[c];
			results[2] = top[c] > results[2] ? top[c] : results[2];
		}
		results[1] = 0;
		// Find bumpiness
		for(int c = 0; c < State.COLS - 1; c++) {
			results[1] += Math.abs(top[c] - top[c+1]);
		}
		results[3] = 0;
		// Find new holes
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			//from bottom to top of brick
			int colHoles = 0;
			for(int h = height+pBottom[nextPiece][orient][i] - 1; h >= 0; h--) {
				if (field[h][i+slot] != 0) break;
				colHoles++;
			}
			results[3] += colHoles;
		}

		results[4] = 0;
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < State.COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//count possibly cleared rows
			if(full) {
				results[4]++;
			}
		}

		//for each column in the piece - empty the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = 0;
			}
		}
		return results;
	}
	
	public static int[] run(double[] weights, boolean showResults) {
        State s = new State();
        while(!s.hasLost()) {
            s.makeMove(PlayerSkeleton.pickMove(s,s.legalMoves(), weights));
        }
        // Prints the number of rows cleared and the turn number
        if (showResults) {
            System.out.println(s.getRowsCleared() + " " + s.getTurnNumber());
        }
        return new int [] { s.getRowsCleared(), s.getTurnNumber() };
	}
	
	public static void run() {
	    run(PlayerSkeleton.DEFAULT_WEIGHTS, true);
	}

	// ==============================================
	// Modified main method
	// ==============================================
	public static void main(String[] args) {
		if(args.length >= 2 && args[0].equals("optimize")) {
            // Run genetic algorithm
		    try {
                GeneticAlgorithm ga = new GeneticAlgorithm();
                int generations = Integer.parseInt(args[1]);
                ga.optimizeWeights(generations);
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

	public double getRowsCompleted() {
		return this.rowsCompleted;
	}
	
	public double getTurnsPlayed() {
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
		s.append(" ");
		for (double d : weights) {
			s.append(d);
			s.append(" ");
		}
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
    
    public GameTask(int index, Agent agent) {
        this.index = index;
        this.weights = agent.getWeights();
        agent.reset();
    }

    @Override
    public int[] call() throws Exception {
        int[] results = PlayerSkeleton.run(weights, false);
        return new int[] { index, results[0], results[1] };
    }
}

class GeneticAlgorithm {

	// ==============================================
	// Default values
	// ==============================================
	private int WORKERS_POOL =  Runtime.getRuntime().availableProcessors(); // threading stuff
	private int POPULATION_SIZE = 500; // number of agents
	private int GAMES = 20; // number of games each agent plays
	private double SELECTION = 0.1; // for tournament selection
	private double CULLING = 0.3;
	private double MUTATION_RATE = 0.05;
	private double MUTATION_DELTA = 0.2;
	private int NUM_WEIGHTS = 5;
	
	private ArrayList<Agent> population;
	private int generationTotalRowsCleared;
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
	}
	
    // ==============================================
    // Driver methods
    // ==============================================
	public void runGames() {
	    for (int i = 0; i < GAMES; i++) {
	        for (int j = 0; j < population.size(); j++) {
	            completionService.submit(new GameTask(j, population.get(j)));
	        }
	    }
	    for (int i = 0; i < (GAMES * population.size()); i++){
	        try {
                int[] result = completionService.take().get();
                population.get(result[0]).updateScore(result[1], result[2]);
                generationTotalRowsCleared += result[1];
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
    	}
	}

	public void report(int gen) {
	    StringBuilder sb = new StringBuilder();
	    //printAgents(population);
        sb.append("Generation ");
        sb.append(Integer.toString(gen));
        sb.append("\n");
        sb.append("Top 5 results\n");
        for (int j = POPULATION_SIZE-1; j > POPULATION_SIZE-6; j--) {
            sb.append(population.get(j).getResultsString());
            sb.append("\n");
        }
        sb.append("Population Average Rows Completed: ");
        sb.append(generationTotalRowsCleared / (GAMES * population.size()));
        sb.append(", Total Games Played: ");
        sb.append(GAMES * population.size());
        logger.log(Level.INFO, sb.toString());
	}
	
	// The point of entry for GeneticAlgorithm
	public void optimizeWeights(int generations) {
		for (int i = 0; i < generations; i++) {
		    generationTotalRowsCleared = 0;
		    runGames();
			Collections.sort(population);
			report(i);
			nextGeneration();
		}
		pool.shutdown();
	}

	public ArrayList<Agent> seedPopulation() {
	    ArrayList<Agent> agentPopulation = new ArrayList<Agent>();
		for (int i = 0; i < POPULATION_SIZE; i++) {
		    agentPopulation.add(new Agent(generateWeights()));
		}
		return agentPopulation;
	}

	public double[] generateWeights() {
		double[] populationWeights = new double[NUM_WEIGHTS];

		// Modified so that the weight generation is a smarter
		// e.g. aggregate column heights should never be +ve
		populationWeights[0] = randRange(-1, 0);
		populationWeights[1] = randRange(-1, 0);
		populationWeights[2] = randRange(-1, 0);
		populationWeights[3] = randRange(-1, 0);
		populationWeights[4] = randRange(0, 1);

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
	// TODO: randomly selected pool may contain duplicates, implement proper subset selection
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


