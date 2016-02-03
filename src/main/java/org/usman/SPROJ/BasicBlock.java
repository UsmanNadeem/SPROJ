
package org.usman.SPROJ;

import java.util.List;
import java.util.ArrayList;
import java.util.*;


public class BasicBlock {
	List<BasicBlockInstruction> instructions;
	int startInstructionAddress;
	int endInstructionAddress;
	int outgoingEdges;
	int instructionCount;
	List<Integer> destinations;
	TreeSet<Object> tanitedVarSet;
	boolean visited;
	public void clearVisited() {
		visited = false;
	}
	public void setVisited() {
		visited = true;
	}
	public void clearTaints() {
		tanitedVarSet = new TreeSet<Object>();
	}
	public void setTanitedVarSet(TreeSet<Object> t) {
		tanitedVarSet = t;
	}
	public BasicBlock(List<BasicBlockInstruction> bbList){
		visited = false;
		instructions = new ArrayList<BasicBlockInstruction>(bbList);
		instructionCount = instructions.size();
		startInstructionAddress = instructions.get(0).address;
		BasicBlockInstruction tail = instructions.get(instructionCount-1);
		endInstructionAddress = tail.address;
		/*if(tail.destinations == null) {
			outgoingEdges = 1;
			this.destinations = new ArrayList<Integer>();
			this.destinations.add(tail.address + tail.instruction.getCodeUnits());
		} else {//*/
		if(tail.destinations != null) {
			outgoingEdges = tail.destinations.size();
			this.destinations = new ArrayList<Integer>(tail.destinations);
		} else {
			outgoingEdges = 0;
			this.destinations = null;
		}
		//}
	}
	// public getInstructions()
}
