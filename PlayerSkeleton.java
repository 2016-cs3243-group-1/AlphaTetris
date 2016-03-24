import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PlayerSkeleton {
	private static double[] DEFAULT_WEIGHTS = {
		-0.510066, // Aggregate column heights
		-0.184483, // Bumpiness
		0, // Max height
		-0.6, // Num of holes created
		0.760666 // Num of completed rows
	};
	private static double[] weights;

	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
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

	public int[] scoreMove(State s, int orient, int slot) throws Exception {
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

	// ==========================================================
	// Methods that GeneticAlgorithm calls from PlayerSkeleton
	// ==========================================================
	public double[] runAverage(double[] args, int numGames) {
		double[] results = new double[2];

		for (int i = 0; i < numGames; i++) {
			double[] result = run(args);
			results[0] += result[0];
			results[1] += result[1];
		}
		results[0] = results[0] / numGames;
		results[1] = results[1] / numGames;

		return results;
	}

	public double[] run(double[] args) {
		double[] results = new double[2];

		// Init weights
		if (args.length != DEFAULT_WEIGHTS.length) {
			weights = DEFAULT_WEIGHTS;
		} else {
			weights = new double[DEFAULT_WEIGHTS.length];
			for(int i = 0; i < DEFAULT_WEIGHTS.length; i++) {
				try {
					weights[i] = args[i];
				} catch (NumberFormatException e) {
					weights = DEFAULT_WEIGHTS;
					break;
				}
			}
		}

		State s = new State();
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
		}

		// Returns the number of rows cleared and the turn number in a tuple
		results[0] = s.getRowsCleared();
		results[1] = s.getTurnNumber();

		return results;
	}

	// ==============================================
	// Modified main method
	// ==============================================
	public static void main(String[] args) {
		// Init weights
		if (args.length != DEFAULT_WEIGHTS.length) {
			weights = DEFAULT_WEIGHTS;
		} else {
			weights = new double[DEFAULT_WEIGHTS.length];
			for(int i = 0; i < DEFAULT_WEIGHTS.length; i++) {
				try {
					weights[i] = Double.parseDouble(args[i]);
				} catch (NumberFormatException e) {
					weights = DEFAULT_WEIGHTS;
					break;
				}
			}
		}

		State s = new State();
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
		}

		// Prints the number of rows cleared and the turn number
		System.out.println(s.getRowsCleared() + " " + s.getTurnNumber());

		// DEBUG AREA
		GeneticAlgorithm ga = new GeneticAlgorithm();
//		for (double weight : ga.generateWeights()) {
//			System.out.println(weight);
//		}
		ga.optimizeWeights(1);
		System.out.println("end");
		System.exit(0);
	}
}

class Agent implements Comparable<Agent> {
	private double[] weights;
	private double[] results;
	private int numGames;

	public Agent(double[] populationWeights, int numGames) {
		this.weights = populationWeights;
		this.numGames = numGames;
	}

	public void runSimulation() {
		PlayerSkeleton ps = new PlayerSkeleton();
		this.results = ps.runAverage(this.weights, numGames);
	}

	public double[] getWeights() {
		return this.weights;
	}

	public double[] getResults() {
		return this.results;
	}

	public void setWeights(double[] newWeights) {
		this.weights = newWeights;
	}

	public void mutateOneWeight(int index, double modifier) {
		this.weights[index] *= modifier;
		normaliseWeights(this.weights);
	}

	private void normaliseWeights(double[] weights) {
		// Modify the array in place
		double sumWeights = 0;
		for (int i = 0; i < weights.length; i++) {
			sumWeights += Math.abs(weights[i]);
		}
		if (sumWeights > 0) {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = (weights[i] / sumWeights);
			}
		} else {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 0.5;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (double d : results) {
			s.append(d + " ");
		}
		for (double d : weights) {
			s.append(d + " ");
		}
		return s.toString();
	}

	@Override
	// Sorted in increasing order number of rows cleared
	// Number of turns survived is used as a tiebreaker
	public int compareTo(Agent o) {
		if (o.getResults()[0] > this.getResults()[0]) {
			return -1;
		} else if (o.getResults()[0] < this.getResults()[0]) {
			return 1;
		} else {
			if (o.getResults()[1] > this.getResults()[1]) {
				return -1;
			} else if (o.getResults()[1] < this.getResults()[1]) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}

class GeneticAlgorithm {

	// ==============================================
	// Default values
	// ==============================================
	private int WORKERS_POOL = 8; // threading stuff
	private int POPULATION_SIZE = 500; // number of agents
	private int GAMES = 10; // number of games each agent plays
	private double SELECTION = 0.1;
	private double CULLING = 0.3;
	private double MUTATION_RATE = 0.05;
	private double MUTATION_DELTA = 0.2;

	private int NUM_WEIGHTS = 5;

	private static Logger LOGGER = Logger.getLogger("tetris");

	ArrayList<double[]> population = seedPopulation();

	// ==============================================
	// Constructor
	// ==============================================
	public GeneticAlgorithm() {
		BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(GAMES);
		// Concurrency not yet implemented
		// spawnWorkers();
		ArrayList<double[]> population = seedPopulation();
	}

	// ==============================================
	// Driver methods
	// ==============================================
	public void optimizeWeights(int generations) {
		// Concurrency not yet implemented
		for (int i = 0; i < generations; i++) {
			int totalRowsCleared = 0;

//			ArrayList<ArrayList<double[]>> ranks = new ArrayList<ArrayList<double[]>>();
//			for (int j = 0; j < POPULATION_SIZE; j++) {
//				ArrayList<double[]> rank = new ArrayList<double[]>();
//				PlayerSkeleton simulation = new PlayerSkeleton();
//
//				double[] simulationResults = simulation.runAverage(population.get(j), GAMES);
//
//				rank.add(simulationResults);
//				rank.add(population.get(j));
//				ranks.add(rank);
//			}

			ArrayList<Agent> agentPopulation = new ArrayList<Agent>();
			for (int j = 0; j < POPULATION_SIZE; j++) {
				Agent agent = new Agent(population.get(j), GAMES);
				agent.runSimulation();
				agentPopulation.add(agent);
			}
			Collections.sort(agentPopulation);
			printAgents(agentPopulation);
		}
	}

	public void spawnWorkers() {
		for (int i = 0; i < WORKERS_POOL; i++) {
			System.out.println("no idea what to do here :/");
		}
	}

	public ArrayList<double[]> seedPopulation() {
		ArrayList<double[]> populations = new ArrayList<double[]>();
		for (int i = 0; i < POPULATION_SIZE; i++) {
			populations.add(generateWeights());
		}
		return populations;
	}

	public double[] generateWeights() {
		int RANGE_MIN = -1;
		int RANGE_MAX = 1;
		Random random = new Random();
		double[] populationWeights = new double[NUM_WEIGHTS];
		for (int i = 0; i < NUM_WEIGHTS; i++) {
			populationWeights[i] = RANGE_MIN + (RANGE_MAX - RANGE_MIN) * random.nextDouble();
		}
		return normaliseWeights(populationWeights);
	}

	public double[] normaliseWeights(double[] weights) {
		// Modify the array in place then return it
		double sumWeights = 0;
		for (int i = 0; i < weights.length; i++) {
			sumWeights += Math.abs(weights[i]);
		}
		if (sumWeights > 0) {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = (weights[i] / sumWeights);
			}
		} else {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 0.5;
			}
		}
		return weights;
	}

	// ====================================================
	// Biology related functions (aka eugenics huehuehue)
	// ====================================================
	public void nextGeneration(ArrayList<ArrayList<double[]>> ranks) {
		// Select the last 10 percentile and replace them
		int numCulled = (int) CULLING * POPULATION_SIZE;
		for (int i = 0; i < numCulled; i++) {
			try {
				ranks.set(i, createOffspring());
			} catch (Exception e) {
				e.printStackTrace(); // by right this shouldn't happen, but just to be safe
			}
		}
	}

	public ArrayList<double[]> createOffspring() {
//		parents = selectParents();

		return null;
	}

	public void selectParents() {

	}

	public void crossover() {

	}

	public void mutate() {
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

	public void report() {
		LOGGER.info("stuff");
	}
}


