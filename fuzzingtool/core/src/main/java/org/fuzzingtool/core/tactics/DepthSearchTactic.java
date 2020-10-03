package org.fuzzingtool.core.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;

import java.util.*;

public class DepthSearchTactic extends FuzzingTactic {
	Stack<BranchingNode> last_max_nodes = new Stack<>();

	public DepthSearchTactic(BranchingNode n, Context c, Logger l) {
		this.ctx = c;
		this.logger = l;
		this.last_max_nodes.push(n);
	}

	protected ArrayList<BranchingNode> findCandidates(BranchingNode current_node) {
		BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

		if (node_type == BranchingNodeAttribute.LOOP) {
			incrementLoop(current_node);
		}

		if (!isValidNode(current_node)) {
			return new ArrayList<>();
		}

		if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
			ArrayList<BranchingNode> candidates = new ArrayList<>();
			for (Boolean taken_flag: new boolean[]{true, false}) {
				BranchingNode child_node = current_node.getChildBranch(taken_flag);
				ArrayList<BranchingNode> child_target_results = findCandidates(child_node);
				candidates.addAll(child_target_results);
			}
			if (node_type == BranchingNodeAttribute.LOOP) {
				decrementLoop(current_node);
			}
			return candidates;
		} else if (node_type == BranchingNodeAttribute.UNKNOWN) {
			ArrayList<BranchingNode> candidate = new ArrayList<>();
			candidate.add(current_node);
			return candidate;
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	protected BranchingNode findUnexplored() {
		ArrayList<BranchingNode> candidates = new ArrayList<>();
		while (candidates.isEmpty()) {
			try {
				candidates = findCandidates(this.last_max_nodes.peek());
			} catch (EmptyStackException ese) {
				break;
			}
			if (candidates.isEmpty()) {
				this.last_max_nodes.pop();
			}
		}
		if (candidates.isEmpty()) {
			return null;
		} else {
			Integer max_depth = -1;
			BranchingNode max_node = null;
			for (BranchingNode candidate: candidates) {
				if (candidate.getDepth() > max_depth) {
					max_depth = candidate.getDepth();
					max_node = candidate;
				}
			}
			this.last_max_nodes.push(max_node);
			return max_node;
		}
	}

	@Override
	public void setOption(String option_name, Object value) {
		switch (option_name) {
			case "max_loop_unrolling":
				try {
					this.max_loop_unrolling = (Integer) value;
					logger.info("DEPTH_SEARCH.max_loop_unrolling option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("DepthSearchTactic: Wrong parameter type for option 'max_loop_unrolling' (Integer).");
				}
				break;
			case "max_depth":
				try {
					this.max_depth = (Integer) value;
					logger.info("DEPTH_SEARCH.max_depth option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("DepthSearchTactic: Wrong parameter type for option 'max_depth' (Integer).");
				}
				break;
			default:
				logger.warning("DEPTH_SEARCH: Unknown option '" + option_name + "'.");
		}
	}

	@Override
	public Object getOption(String option_name) {
		switch (option_name) {
			case "max_loop_unrolling":
				return this.max_loop_unrolling;
			case "max_depth":
				return this.max_depth;
			default:
				logger.warning("DepthSearchTactic: Unknown option '" + option_name + "'.");
				return null;
		}
	}

	@Override
	public String getTactic() {
		return "DEPTH_SEARCH";
	}
}
