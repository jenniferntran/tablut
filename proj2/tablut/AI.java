package tablut;

import static java.lang.Math.*;

/** A Player that automatically generates moves.
 *  @author Jennifer Tran
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        Move m = findMove();
        _controller.reportMove(m);
        return m.toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        findMove(b, maxDepth(b), true, 1, -INFTY, INFTY);
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (depth == 0 || board.winner() != null) {
            return staticScore(board);
        }
        int bestSoFar = staticScore(board);
        if (sense == 1) {
            sense *= -1;
            bestSoFar = -INFTY;
            for (Move m: board.legalMoves(board.turn())) {
                board.makeMove(m);
                int resp = findMove(board, depth - 1, false, sense,
                        alpha, beta);
                board.undo();
                if (resp >= bestSoFar) {
                    if (saveMove) {
                        _lastFoundMove = m;
                    }
                    bestSoFar = resp;
                    alpha = max(alpha, resp);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        } else if (sense == -1) {
            sense *= -1;
            bestSoFar = INFTY;
            for (Move m: board.legalMoves(board.turn())) {
                board.makeMove(m);
                int resp = findMove(board, depth - 1, false, sense,
                        alpha, beta);
                board.undo();
                if (resp <= bestSoFar) {
                    if (saveMove) {
                        _lastFoundMove = m;
                    }
                    bestSoFar = resp;
                    beta = min(beta, resp);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        }
        return bestSoFar;
    }

    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        return 4;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int score = INFTY;
        if (board.winner() == Piece.WHITE) {
            return WINNING_VALUE;
        } else if (board.winner() == Piece.BLACK) {
            return -WINNING_VALUE;
        } else if (board.kingPosition() == null) {
            return -WINNING_VALUE;
        } else if (board.kingPosition().isEdge()) {
            return WINNING_VALUE;
        } else if (board.countSide(Piece.BLACK)
                > board.countSide(Piece.WHITE)) {
            return -WINNING_VALUE;
        } else {
            return score;
        }
    }

}
