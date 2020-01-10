package tablut;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import static tablut.Piece.BLACK;
import static tablut.Piece.EMPTY;
import static tablut.Piece.KING;
import static tablut.Piece.WHITE;
import static tablut.Square.SQUARE_LIST;
import static tablut.Square.sq;

/** The state of a Tablut Game.
 *  @author Jennifer Tran
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        init();
        _winner = model._winner;
        _turn = model._turn;
        _moveCount = model._moveCount;
        sqStack = model.sqStack;
        csqStack = model.csqStack;
        pStack = model.pStack;
        sStack = model.sStack;
        board = model.board;
        _repeated = model._repeated;
        lim = model.lim;
    }

    /** Clears the board to the initial position. */
    void init() {
        board = new Piece[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        for (Square sq: INITIAL_ATTACKERS) {
            board[sq.col()][sq.row()] = BLACK;
        }
        for (Square sq: INITIAL_DEFENDERS) {
            board[sq.col()][sq.row()] = WHITE;
        }
        board[THRONE.col()][THRONE.row()] = KING;
        _moveCount = 0;
        _turn = BLACK.side();
        pStack = new Stack<>();
        sqStack = new Stack<>();
        csqStack = new Stack<>();
        sStack = new Stack<>();
        _winner = null;
    }

    /** Set the move limit to LIM, N.  It is an error if 2*LIM <=
     * moveCount(). */
    void setMoveLimit(int n) {
        lim = n;
        if (moveCount() >= 2 * lim) {
            throw new IllegalArgumentException("Moves are over the limit!");
        }
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (sStack.contains(encodedBoard())) {
            _repeated = true;
            _winner = _turn;
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (int col = 0; col < SIZE; col++) {
            for (int row = 0; row < SIZE; row++) {
                if (board[col][row] == KING) {
                    return sq(col, row);
                }
            }
        }
        return null;
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return board[col][row];
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        board[s.col()][s.row()] = p;
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        put(p, s);
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        int dir = from.direction(to);
        if (get(from) == EMPTY) {
            return false;
        }
        while (from.isRookMove(to)) {
            from = from.rookMove(dir, 1);
            if (from == null || get(from) != EMPTY) {
                return false;
            }
        }
        return true;
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if (from == to || !isLegal(from)) {
            return false;
        }
        if (!get(from).equals(KING) && to.equals(THRONE)) {
            return false;
        }
        return isUnblockedMove(from, to);
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        assert isLegal(from, to);
        if (!hasMove(_turn)) {
            if (_turn == WHITE.side()) {
                _winner = BLACK.side();
            } else {
                _winner = WHITE.side();
            }
        }
        sStack.push(encodedBoard());
        _moveCount += 1;
        HashSet<Square> pieces = pieceLocations(_turn);
        board[to.col()][to.row()] = board[from.col()][from.row()];
        put(EMPTY, from);
        for (int d = 0; d < 4; d++) {
            Square rookSquare = to.rookMove(d, 2);
            if (rookSquare != null) {
                pStack.push(get(to.between(rookSquare)));
                csqStack.push(to.between(rookSquare));
                if (pieces.contains(rookSquare) || rookSquare.equals(THRONE)) {
                    capture(to, rookSquare);
                }
            }
        }
        sqStack.push(from);
        sqStack.push(to);
        if (kingPosition() != null && kingPosition().isEdge()) {
            _winner = WHITE;
        }
        if (_turn == WHITE.side()) {
            _turn = BLACK.side();
        } else {
            _turn = WHITE.side();
        }
        checkRepeated();
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        if (move.toString().equals("h4-5")) {
            int x = 1;
        }
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square sq1 = sq0.between(sq2);
        if (get(sq1) == KING) {
            if (captureKing(sq0, sq2)) {
                _winner = BLACK;
                put(EMPTY, sq1);
            }
        } else if (captureCheck(sq0, sq2)) {
            put(EMPTY, sq1);
        }
    }

    /** Returns TRUE if Piece between SQ0 and SQ2 satisfies conditions
     * to be captured. */
    private boolean captureCheck(Square sq0, Square sq2) {
        Piece p0 = get(sq0);
        Piece p2 = get(sq2);
        Square sq1 = sq0.between(sq2);
        if (get(sq1).equals(EMPTY) || get(sq1).side() == p0.side()) {
            return false;
        } else if (sq2.equals(THRONE)) {
            return checkHostileThrone(sq0, sq2);
        }
        return sq0 != sq2
                && p0.side() == p2.side()
                && (sq0.col() == sq2.col() || sq0.row() == sq2.row());
    }

    /** Returns TRUE if conditions to capture a King between SQ0 and SQ2
     * are satisfied. */
    private boolean captureKing(Square sq0, Square sq2) {
        Square sq1 = sq0.between(sq2);
        if (checkThrone()) {
            if (get(sq1).equals(KING)) {
                for (int d = 0; d < 4; d++) {
                    Square check = sq1.rookMove(d, 1);
                    if (!get(check).equals(BLACK) && !check.equals(THRONE)) {
                        return false;
                    }
                }
            }
        }
        return captureCheck(sq0, sq2);
    }

    /** Returns TRUE if the throne is hostile between SQ0 and SQ2. */
    private boolean checkHostileThrone(Square sq0, Square sq2) {
        Square sq1 = sq0.between(sq2);
        if (sq2 == THRONE) {
            if (get(sq1).equals(EMPTY)) {
                return false;
            } else if (!kingPosition().equals(THRONE) || checkBlack()) {
                return true;
            }
        }
        return false;
    }

    /** Returns TRUE if there are at least 3 thrones that contain BLACK
     * pieces. */
    private boolean checkBlack() {
        Square[] thrones = new Square[]{NTHRONE, WTHRONE, ETHRONE, STHRONE};
        int check = 0;
        for (Square s: thrones) {
            if (get(s).equals(BLACK)) {
                check += 1;
            }
        }
        return check >= 3;
    }

    /** Returns TRUE if the King is positioned on any throne. */
    private boolean checkThrone() {
        return kingPosition() == THRONE
                || kingPosition() == NTHRONE || kingPosition() == WTHRONE
                || kingPosition() == STHRONE || kingPosition() == ETHRONE;
    }

    /** Undo one move. Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
            _moveCount -= 1;
            _turn = _turn.opponent().side();
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        _repeated = false;
        Square to = sqStack.pop();
        Square from = sqStack.pop();
        for (int d = 3; d >= 0; d--) {
            Square sq = to.rookMove(d, 2);
            if (sq != null) {
                Piece pCapture = pStack.pop();
                Square sqCapture = csqStack.pop();
                put(pCapture, sqCapture);
            }
        }
        sStack.pop();
        board[from.col()][from.row()] = board[to.col()][to.row()];
        put(EMPTY, to);
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        sqStack = new Stack<>();
        pStack = new Stack<>();
        csqStack = new Stack<>();
        sStack = new Stack<>();
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        List<Move> moves = new ArrayList<>();
        for (Square p: pieceLocations(side)) {
            for (int d = 0; d < 4; d++) {
                for (Move m: Move.ROOK_MOVES[p.index()][d]) {
                    if (isLegal(m)) {
                        moves.add(m);
                    }
                }
            }
        }
        return moves;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        return !legalMoves(side).isEmpty();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    private HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> plocations = new HashSet<>();
        for (int col = 0; col < SIZE; col++) {
            for (int row = 0; row < SIZE; row++) {
                if (get(col, row).side() == side.side()) {
                    plocations.add(sq(col, row));
                }
            }
        }
        return plocations;
    }

    /** Return INT of positions on a SIDE. */
    int countSide(Piece side) {
        return pieceLocations(side).size();
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;
    /** A board represented as a 2D array of pieces. */
    private Piece[][] board;
    /** Limit of move count. */
    private int lim;
    /** A stack of squares. */
    private Stack<Square> sqStack;
    /** A stack of pieces for captures. */
    private Stack<Piece> pStack;
    /** A stack of squares for captures. */
    private Stack<Square> csqStack;
    /** A stack of strings for board. */
    private Stack<String> sStack;

}
