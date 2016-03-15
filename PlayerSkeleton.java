
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
		int[][][] pBottom = s.getpBottom();
		int[][][] pTop = s.getpTop();
		int[][] pHeight = s.getpHeight();
		int[][] pWidth = s.getpWidth();

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

	// Modified main method
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
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// Prints the number of rows cleared and the turn number
		System.out.println(s.getRowsCleared() + " " + s.getTurnNumber());
		System.exit(0);
	}
	
	// Default main method
	/*
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}*/
}
