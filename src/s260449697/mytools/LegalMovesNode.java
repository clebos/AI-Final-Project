package s260449697.mytools;

import halma.CCBoard;
import halma.CCMove;

import java.util.ArrayList;

public class LegalMovesNode {
	    private CCBoard board;
	    private ArrayList<LegalMovesNode> children;
	    private CCMove prevMove;
	    private LegalMovesNode parent;
	    private boolean visited;
	    private int depth;

	    public LegalMovesNode(CCBoard b, CCMove prev, LegalMovesNode parent, int depth){
	        this.board = b;
	        this.prevMove = prev;
	        this.children = new ArrayList<LegalMovesNode>();
	        this.parent = parent;
	        this.depth = depth;
	    }

	    // methods for children
	    
	    public int getDepth() {
			return depth;
		}

		public LegalMovesNode getChildAt(int i){
	        return children.get(i);
	    }

	    public int nbChildren(){
	        return children.size();
	    }
	    
	    public ArrayList<LegalMovesNode> getChildren() {
	    	return children;
	    }

	    public void addChild(LegalMovesNode child) {
	    	children.add(child);
	    }
	    
	    //are the children empty? 
	    public boolean hasChildren() {
	    	if(children.size() > 0) {
	    		return true;
	    	}
	    	else return false;
	    }
	    //board getter 
	    public CCBoard getBoard(){
	        return this.board;
	    }
	    
		//parent getter
	    public LegalMovesNode getParent() {
	    	return this.parent;
	    }

	    //visited getter and setter
	    public boolean getVisited() {
	    	return visited;
	    }
	    
	    public void setVisited(boolean newVisited) {
	    	visited = newVisited;
	    }
	    
	    //prevMove getter
		public CCMove getPrevMove() {
			return prevMove;
		}
		
	    

	    
}
