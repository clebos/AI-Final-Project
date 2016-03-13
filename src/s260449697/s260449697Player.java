package s260449697;

import halma.CCBoard;
import halma.CCMove;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import boardgame.Board;
import boardgame.Move;
import boardgame.Player;

import s260449697.mytools.*;

public class s260449697Player extends Player {

	Random rand = new Random();
	boolean wasHop;
	ArrayList<Point> visited = new ArrayList<Point>();
	ArrayList<CCMove> movesList = new ArrayList<CCMove>();
	double prevManDist;
	int lastTurn = -1;
	//set up base and goal
	HashSet<Point> base = new HashSet<Point>();
	HashSet<Point> goal = new HashSet<Point>();
	
	public s260449697Player(String name) {
		super(name);
	}

	public s260449697Player() {
		super("260449697");
	}

	//choose a move 
	public Move chooseMove(Board theboard) {
		
		// Clone the board
		CCBoard board = (CCBoard) theboard.clone();

		// if this is a new turn, set the wasHop parameter to false and set "last turn" to be the current turn
		if (lastTurn != board.getTurnsPlayed()) {
			wasHop = false;
			lastTurn = board.getTurnsPlayed();
		}
		
		if(wasHop) {
			//if the last move on your moves list isn't null, that means that you haven't ended your turn yet and are still hopping so call the rest of the method
			if(movesList.size() == 1 && movesList.get(0).getTo() != null) {
			}
			//otherwise,return the move on movesList.
			else {
				return movesList.remove(0);
			}
		}

		// If there is only one valid move available, set wasHop to false and
		// return the last move
		if (board.getLegalMoves().size() == 1) {
			wasHop = false;
			return board.getLegalMoves().get(0);
		}

		// Set your base and the goal
		base = CCBoard.bases[this.playerID];
		goal = CCBoard.bases[this.playerID ^ 3];
		
		// calculate the point that is farthest away in the goal
		Point farthestGoalPoint = new Point();
		switch (playerID) {
		case 0:
			farthestGoalPoint.setLocation(CCBoard.SIZE-1, CCBoard.SIZE-1);
			break;
		case 1:
			farthestGoalPoint.setLocation(0, CCBoard.SIZE-1);
			break;
		case 2:
			farthestGoalPoint.setLocation(CCBoard.SIZE-1 , 0);
			break;
		case 3:
			farthestGoalPoint.setLocation(0, 0);
			break;
		}

		// Create an arrayList of you pieces
		ArrayList<Point> myPieces = board.getPieces(this.playerID);
		// Create a boolean to describe whether or not you have pieces in base
		boolean piecesInBase;

		// Create a move to return and a null move to use throughout the program
		CCMove moveToReturn = new CCMove(this.playerID, null, null);
		CCMove nullMove = new CCMove(this.playerID, null, null);

		// Create a list of moves to remove:
		ArrayList<CCMove> removals = new ArrayList<CCMove>();

		// Get all current legal moves
		ArrayList<CCMove> legalMoves = board.getLegalMoves();

		if (board.getTurnsPlayed() > 25) {
			piecesInBase = piecesInBase(myPieces, base);
			if (piecesInBase) {
				for (CCMove notStartFromBase : legalMoves) {
					if (!base.contains(notStartFromBase.getFrom())) {
						removals.add(notStartFromBase);
					}
				}
			}
		}
		
		// remove the elements in removals
		for (CCMove remove : removals) {
			legalMoves.remove(remove);
		}

		// create the tree of legal moves
		LegalMovesNode root = new LegalMovesNode(board, nullMove, null, 0);
		Tree<LegalMovesNode> legalTree = new Tree<LegalMovesNode>(root);

		calculateFullLegalMoves(root, legalMoves, myPieces, base, farthestGoalPoint);
		
		// calculate the best node, and the best moveList
		LegalMovesNode bestNode = bestNodeTree(legalTree, farthestGoalPoint);
		getMovesList(bestNode);
		
		//return the first move on the movesList
		moveToReturn = movesList.remove(0);

		// if it is a hop, set the "was hop" parameter to true + add from parameter to visited.
		// if it is a hop, also make sure to set the previous manhattan distance parameter (prevmandist) to the manhattan distance of this move
		// if it is not a hop set the "was hop" parameter to false and clear visited
		if (moveToReturn.isHop()) {
			wasHop = true;
			visited.add(moveToReturn.getFrom());
			prevManDist = manhattanAfterMove(board, moveToReturn,
					farthestGoalPoint);

		} else {
			wasHop = false;
			visited.clear();
		}

		return moveToReturn;

	}

	// return a node which has the best board state for this move
	private LegalMovesNode bestNodeTree(Tree<LegalMovesNode> legalMovesTree,
			Point farthestPoint) {

		LegalMovesNode bestNode = null;
		double bestValue = Double.MAX_VALUE;
		double tempValue = Double.MAX_VALUE;

		// create a new queue
		Queue<LegalMovesNode> q = new LinkedList<LegalMovesNode>();

		// add the root node
		q.add(legalMovesTree.getHead());
		legalMovesTree.getHead().setVisited(true);

		// perform BFS to traverse the tree and find the best node
		// BFS adapted from:
		// http://www.codeproject.com/Articles/32212/Introduction-to-Graph-with-Breadth-First-Search-BF

		while (!q.isEmpty()) {

			// following are for use in minichain calculation
			int miniChainValue = 0;
			int opponentMiniChainValue = 0;

			// the following are used to calculate how many pieces are in base
			double basePieces = 0;

			// dequeue node and examine it
			LegalMovesNode n = (LegalMovesNode) q.remove();

			// set up allpieces in relation to the current potentialBoard
			ArrayList<Point> allPieces = new ArrayList<Point>();
			for (int i = 0; i < 4; i++) {
				allPieces.addAll(n.getBoard().getPieces(i));
			}

			// evaluate n:
			tempValue = evaluateManhattanDistance(farthestPoint, n.getBoard());

			// minichain detection for my pieces
			for (Point piece : n.getBoard().getPieces(this.playerID)) {
				miniChainValue = miniChainValue
						+ detectMinichains(piece, allPieces);
			}
			// Detect minichains for my ally
			for (Point allyPiece : n.getBoard().getPieces(this.playerID ^ 3)) {
				miniChainValue = miniChainValue
						+ detectMinichains(allyPiece, allPieces);
			}
			// minichain for my first opponent
			for (Point opponent2piece : n.getBoard().getPieces(
					this.playerID ^ 2)) {
				opponentMiniChainValue = miniChainValue
						+ oppositeMinichains(opponent2piece, allPieces);
			}
			// minichain detection for second opponent
			for (Point opponent1piece : n.getBoard().getPieces(
					this.playerID ^ 1)) {
				opponentMiniChainValue = miniChainValue
						+ oppositeMinichains(opponent1piece, allPieces);
			}

			// calculate the number of pieces in base
			if (piecesInBase(n.getBoard().getPieces(this.playerID), base)) {
				basePieces = numberOfPiecesInBase(
						n.getBoard().getPieces(this.playerID), base);
			} else {
				basePieces = 0;
			}

			// Perform final temp-value calculations incorporating minichain
			// values and base piece values
			if (n.getBoard().getTurnsPlayed() < 25) {
				tempValue = tempValue - 0.002 * miniChainValue + 100
						* basePieces;
			}

			else if (n.getBoard().getTurnsPlayed() > 25
					&& n.getBoard().getTurnsPlayed() < 40) {
				tempValue = tempValue - 0.0005 * miniChainValue + 0.0004
						* opponentMiniChainValue;
			} else {
				tempValue = tempValue;
			}

			// compare the temp value and best value:
			// check if the temp value is better than the best value, and if it
			// is, set best value = temp value, and n as the best node
			if (tempValue <= bestValue && (n.getParent() != null)) {
				bestValue = tempValue;
				bestNode = n;
			}

			// enqueue not yet visited children
			for (LegalMovesNode child : n.getChildren()) {
				if (child.getVisited() == false) {
					child.setVisited(true);
					q.add(child);
				}
			}
		}
		return bestNode;
	}

	// Parent trace back of a node to return a list of the next few best moves to make to attain the desired board state
	private void getMovesList(LegalMovesNode bestNode) {

		while (bestNode.getParent() != null) {
			movesList.add(0, bestNode.getPrevMove());
			bestNode = bestNode.getParent();
		}
		movesList.add(new CCMove(this.playerID, null, null));
	}

	// calculate the potential manhattan distance after a move
	private double manhattanAfterMove(CCBoard currentBoard, CCMove moveToTest, Point farthestPoint) {
		CCBoard testBoard = (CCBoard) currentBoard.clone();
		testBoard.move(moveToTest);
		double manhattanDistance = evaluateManhattanDistance(farthestPoint, testBoard);
		return manhattanDistance;
	}

	// the following method calculates the manhattan distance for a list of
	// pieces to the farthest point
	private double evaluateManhattanDistance(Point farthestPoint, CCBoard board) {
		ArrayList<Point> pieces = board.getPieces(this.playerID);
		double distance = 0;
		for (Point piece : pieces) {
			distance = distance + piece.distance(farthestPoint);
		}
		return distance;
	}

	// the following method checks if there are any pieces remaining in base
	// if there is a piece in base return true, else return false
	private boolean piecesInBase(ArrayList<Point> pieces, HashSet<Point> base) {
		boolean piecesInBase = false;
		for (Point piece : pieces) {
			if (base.contains(piece)) {
				piecesInBase = true;
				break;
			}
		}
		return piecesInBase;
	}
	
	
	//Calculates the number of pieces remaining in a given base
	private int numberOfPiecesInBase(ArrayList<Point> pieces, HashSet<Point> base) {
		int number = 0;
		for(Point piece : pieces) {
			if(base.contains(piece)) {
				number++;
			}
		}
		return number;
	}
	

	// Detect Minichains Method
	// Returns a code specifying the number of surrounding pieces. 2 pieces => 2, 1 piece in a chain => 1, no piecies in a chian => 0
	// Points one through six are numbered starting from the upper left corner going around the cell in question
	private int detectMinichains(Point center, ArrayList<Point> pieces) {
		int x = (int) center.getX();
		int y = (int) center.getY();
		Point one = new Point(x - 1, y - 1);
		Point two = new Point(x, y - 1);
		Point three = new Point(x + 1, y - 1);
		Point four = new Point(x + 1, y);
		Point five = new Point(x + 1, y + 1);
		Point six = new Point(x, y + 1);
		Point seven = new Point(x - 1, y + 1);
		Point eight = new Point(x - 1, y);
		HashSet<Point> wanted = new HashSet<Point>();
		HashSet<Point> unwanted = new HashSet<Point>();

		if (this.playerID == 0 || this.playerID == 3) {
			wanted.add(one);
			wanted.add(five);
			unwanted.add(two);
			unwanted.add(three);
			unwanted.add(four);
			unwanted.add(six);
			unwanted.add(seven);
			unwanted.add(eight);
			if (pieces.contains(wanted) && !pieces.contains(unwanted)) {
				return 2;
			} else if ((pieces.contains(one) || pieces.contains(five) && !pieces.contains(unwanted))) {
				return 1;
			} else {
				return 0;
			}
			
			

		} else {
			wanted.add(three);
			wanted.add(seven);
			unwanted.add(two);
			unwanted.add(one);
			unwanted.add(four);
			unwanted.add(six);
			unwanted.add(five);
			unwanted.add(eight);
			if (pieces.contains(wanted) && !pieces.contains(unwanted)) {
				return 2;
			} else if ((pieces.contains(three) || pieces.contains(seven))
					&& !pieces.contains(unwanted)) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	//A minichains method to score the opponents
	private int oppositeMinichains(Point center, ArrayList<Point> pieces) {
		int x = (int) center.getX();
		int y = (int) center.getY();
		Point one = new Point(x - 1, y - 1);
		Point two = new Point(x, y - 1);
		Point three = new Point(x + 1, y - 1);
		Point four = new Point(x + 1, y);
		Point five = new Point(x + 1, y + 1);
		Point six = new Point(x, y + 1);
		Point seven = new Point(x - 1, y + 1);
		Point eight = new Point(x - 1, y);
		HashSet<Point> wanted = new HashSet<Point>();
		HashSet<Point> unwanted = new HashSet<Point>();

		if (this.playerID == 1 || this.playerID == 2) {
			wanted.add(one);
			wanted.add(five);
			unwanted.add(two);
			unwanted.add(three);
			unwanted.add(four);
			unwanted.add(six);
			unwanted.add(seven);
			unwanted.add(eight);
			if (pieces.contains(wanted) && !pieces.contains(unwanted)) {
				return 2;
			} else if ((pieces.contains(one) || pieces.contains(five))
					&& !pieces.contains(unwanted)) {
				return 1;
			} else {
				return 0;
			}

		} else {
			wanted.add(three);
			wanted.add(seven);
			unwanted.add(two);
			unwanted.add(one);
			unwanted.add(four);
			unwanted.add(six);
			unwanted.add(five);
			unwanted.add(eight);
			if (pieces.contains(wanted) && !pieces.contains(unwanted)) {
				return 2;
			} else if ((pieces.contains(three) || pieces.contains(seven))
					&& !pieces.contains(unwanted)) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	//Builds the tree of all possible legal moves
	private void calculateFullLegalMoves(LegalMovesNode currentNode, ArrayList<CCMove> legalMoves, ArrayList<Point> myPieces, HashSet<Point> base, Point farthestPoint) {
		
		CCBoard tempBoard;
		LegalMovesNode newNode;
		CCMove move;

		for (int i = 0; i < legalMoves.size(); i++) {

			move = legalMoves.get(i);

			// if the move is not a hop, just add this now and you are done
			// (can't go anymore after a hop)
			if (!move.isHop()) {
				tempBoard = (CCBoard) currentNode.getBoard().clone();
				tempBoard.move(move);
				newNode = new LegalMovesNode(tempBoard, move, currentNode, currentNode.getDepth() + 1);
				currentNode.addChild(newNode);
			}

			// if the move is a hop, add this node and call recursively on the
			// node you just added
			else {
				tempBoard = (CCBoard) currentNode.getBoard().clone();
				tempBoard.move(move);
				newNode = new LegalMovesNode(tempBoard, move, currentNode, currentNode.getDepth() + 1);
				currentNode.addChild(newNode);

				ArrayList<CCMove> newLegalMoves = tempBoard.getLegalMoves();

				// recurisvely call calculateFullLegalMoves, but only if the depth of the tree is 9 or less. Otherwise just return
				if (newNode.getDepth() > 9) {

				} else {
					calculateFullLegalMoves(newNode, newLegalMoves, myPieces,
							base, farthestPoint);
				}
			}
		}
	}

}