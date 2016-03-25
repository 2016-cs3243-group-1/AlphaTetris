
public class PlayerSkeleton {
    private static double[] DEFAULT_WEIGHTS = {
            -0.510066, // Aggregate column heights
            -0.184483, // Bumpiness
            0, // Max height
            -0.6, // Num of holes created
            0.760666 // Num of completed rows
    };

    private static double[] weights;

    double land_height;
    double row_clear;
    double row_breaks;
    double column_breaks;
    double hole_idx;
    double depth_idx;
    double pile_height;

    public PlayerSkeleton() {
        land_height = -1.8500868;
        row_clear = 1.760666;
        row_breaks = -1.1653277;
        column_breaks = -4.1061407;
        hole_idx = -0.6;
        depth_idx = -2.4075407;
        pile_height = -1.0;
    }

    // Modified main method
    public static void main(String[] args) {
        // Init weights
//        if (args.length != DEFAULT_WEIGHTS.length) {
//            weights = DEFAULT_WEIGHTS;
//        } else {
//            weights = new double[DEFAULT_WEIGHTS.length];
//            for (int i = 0; i < DEFAULT_WEIGHTS.length; i++) {
//                try {
//                    weights[i] = Double.parseDouble(args[i]);
//                } catch (NumberFormatException e) {
//                    weights = DEFAULT_WEIGHTS;
//                    break;
//                }
//            }
//        }

        PlayerSkeleton p = new PlayerSkeleton();
        State s = new State();
        new TFrame(s);

        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
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

    //implement this function to have a working system
    // modified evaluation function
    public int pickMove(State s, int[][] legalMoves) {
        double[] evaluate_moves = new double[legalMoves.length];
        int move = 0;


        for (int i = 0; i < evaluate_moves.length; i++) {
            evaluate_moves[i] = evaluateMoves(s, legalMoves[i]);

            if ((i > 0) && (evaluate_moves[i] > evaluate_moves[move]))
                move = i;
        }

        return move;

//        int move = 0;
//        double max = -Double.MAX_VALUE;
//        for (int i = 0; i < legalMoves.length; i++) {
//            try {
//                int[] results = scoreMove(s, legalMoves[i][State.ORIENT], legalMoves[i][State.SLOT]);
//                double score = 0;
//                for (int j = 0; j < weights.length; j++) {
//                    score += results[j] * weights[j];
//                }
//                if (score > max) {
//                    max = score;
//                    move = i;
//                }
//            } catch (Exception e) {
//                continue;
//            }
//        }
//        return move;
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
        int height = top[slot] - pBottom[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for (int c = 1; c < pWidth[nextPiece][orient]; c++) {
            height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
        }

        if (height + pHeight[nextPiece][orient] >= State.ROWS) {
            throw new Exception();
        }

        //for each column in the piece - fill in the appropriate blocks
        for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
            //from bottom to top of brick
            for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
                field[h][i + slot] = turn;
            }
        }

        //adjust top
        for (int c = 0; c < pWidth[nextPiece][orient]; c++) {
            top[slot + c] = height + pTop[nextPiece][orient][c];
        }

        results[0] = 0;
        results[2] = 0;
        // Sum heights and find max height
        for (int c = 0; c < State.COLS; c++) {
            results[0] += top[c];
            results[2] = top[c] > results[2] ? top[c] : results[2];
        }
        results[1] = 0;
        // Find bumpiness
        for (int c = 0; c < State.COLS - 1; c++) {
            results[1] += Math.abs(top[c] - top[c + 1]);
        }
        results[3] = 0;
        // Find new holes
        for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
            //from bottom to top of brick
            int colHoles = 0;
            for (int h = height + pBottom[nextPiece][orient][i] - 1; h >= 0; h--) {
                if (field[h][i + slot] != 0) break;
                colHoles++;
            }
            results[3] += colHoles;
        }

        results[4] = 0;
        //check for full rows - starting at the top
        for (int r = height + pHeight[nextPiece][orient] - 1; r >= height; r--) {
            //check all columns in the row
            boolean full = true;
            for (int c = 0; c < State.COLS; c++) {
                if (field[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            //count possibly cleared rows
            if (full) {
                results[4]++;
            }
        }

        //for each column in the piece - empty the appropriate blocks
        for (int i = 0; i < pWidth[nextPiece][orient]; i++) {
            //from bottom to top of brick
            for (int h = height + pBottom[nextPiece][orient][i]; h < height + pTop[nextPiece][orient][i]; h++) {
                field[h][i + slot] = 0;
            }
        }

//        return computeLandingHeight(pHeight[nextPiece][orient], height) * land_height
//                + results[4] * row_clear + getRowBreaks(field)
//                * results[3] + getWellHole(field)
//                * getMax(top) * pile_height;

        return results;
    }

    public double evaluateMoves(State s, int[] move) {
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

        for (int i = 0; i < field.length; i++)
            System.arraycopy(field[i], 0, field_temp[i], 0, field[0].length);

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

                        while (top[c] >= 1 && field[top[c] - 1][c] == 0)
                            top[c]--;
                    }
                }
            }
        } else
            return -9999;

        return computeLandingHeight(pHeight[next_piece][orient], height) * land_height
                + rows_cleared * row_clear + getRowBreaks(field)
                * row_breaks + getColumnBreaks(field)
                * column_breaks + getNumberOfHoles(top, field)
                * hole_idx + getWellHole(field)
                * depth_idx + getMax(top) * pile_height;
    }

    private int getMax(int[] top) {
        int max = top[0];

        for (int t : top) {
            max = Math.max(max, t);
        }

        return max;
    }

    public int computeLandingHeight(int pHeight, int height) {
        int land_height;

        land_height = height + ((pHeight - 1) / 2);

        return land_height;
    }

    public int getNumberOfHoles(int[] top, int[][] board) {
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
    public int getRowBreaks(int[][] board) {
        int row_transition = 0;
        int previous_state = 1;

        for (int row = 0; row < State.ROWS - 1; row++) {
            for (int col = 0; col < State.COLS; col++) {

                if ((board[row][col] != 0) != (previous_state != 0)) {
                    row_transition++;
                }

                previous_state = board[row][col];
            }

            if (board[row][State.COLS - 1] == 0)
                row_transition++;

            previous_state = 1;
        }

        return row_transition;
    }

    // Get the number of transitions between a filled and empty cell in a column
    public int getColumnBreaks(int[][] board) {
        int column_transition = 0;
        int previous_state = 1;

        for (int col = 0; col < State.COLS; col++) {
            for (int row = 0; row < State.ROWS - 1; row++) {
                if ((board[row][col] != 0) != (previous_state != 0)) {
                    column_transition++;
                }

                if (board[State.ROWS - 1][col] == 0)
                    column_transition++;

                previous_state = board[row][col];
            }

            previous_state = 1;
        }

        return column_transition;
    }

    // Get a 'well' hole with filled cell on the left and right
    public int getWellHole(int[][] board) {
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

    // Find max height of the column
//    public int checkMaxHeight(State state) {
//        int max_height = 0;
//        int[] top = state.getTop();
//
//        for (int i = 0; i < State.COLS; i++) {
//            // Check if the current height is the largest
//            if (top[i] > max_height)
//                max_height = top[i];
//        }
//
//        return max_height;
//    }
//
//    public int holes(State state) {
//        int[] top = state.getTop();
//        int[][] field = state.getField();
//        int num_holes = 0;
//
//        for (int i = 0; i < State.COLS; i++) {
//            for (int j = 0; j < top[i]; j++) {
//                // Check if field is 0 == hole
//                if (field[j][i] == 0)
//                    num_holes++;
//            }
//        }
//
//        return num_holes;
//    }

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
