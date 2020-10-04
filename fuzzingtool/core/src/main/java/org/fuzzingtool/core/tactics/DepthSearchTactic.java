package org.fuzzingtool.core.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;
import org.graalvm.collections.Pair;

import java.util.*;

public class DepthSearchTactic extends FuzzingTactic {
	Stack<Pair<BranchingNode, HashMap<Integer, Integer>>> last_max_nodes = new Stack<>();

	public DepthSearchTactic(BranchingNode n, Context c, Logger l) {
		this.ctx = c;
		this.logger = l;
		this.last_max_nodes.push(Pair.create(n, new HashMap<>()));
	}

	protected ArrayList<Pair<BranchingNode, HashMap<Integer, Integer>>> findCandidates(BranchingNode current_node) {
		BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

		incrementLoop(current_node);

		if (!isValidNode(current_node)) {
			decrementLoop(current_node);
			return new ArrayList<>();
		}

		if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
			ArrayList<Pair<BranchingNode, HashMap<Integer, Integer>>> candidates = new ArrayList<>();
			for (Boolean taken_flag: new boolean[]{true, false}) {
				BranchingNode child_node = current_node.getChildBranch(taken_flag);
				ArrayList<Pair<BranchingNode, HashMap<Integer, Integer>>> child_target_results = findCandidates(child_node);
				candidates.addAll(child_target_results);
			}
			decrementLoop(current_node);
			return candidates;
		} else if (node_type == BranchingNodeAttribute.UNKNOWN) {
			ArrayList<Pair<BranchingNode, HashMap<Integer, Integer>>> candidate = new ArrayList<>();
			HashMap<Integer, Integer> loop_unrolling_state = new HashMap<>(this.loop_unrolls);
			candidate.add(Pair.create(current_node, loop_unrolling_state));
			return candidate;
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	protected BranchingNode findUnexplored() {
		ArrayList<Pair<BranchingNode, HashMap<Integer, Integer>>> candidates = new ArrayList<>();
		while (candidates.isEmpty()) {
			if (this.last_max_nodes.empty()) {
				break;
			}
			Pair<BranchingNode, HashMap<Integer, Integer>> state = this.last_max_nodes.peek();
			this.loop_unrolls = new HashMap<>(state.getRight());
			candidates = findCandidates(state.getLeft());
			if (candidates.isEmpty()) {
				this.last_max_nodes.pop();
			}
		}
		if (candidates.isEmpty()) {
			return null;
		} else {
			Integer max_depth = -1;
			Pair<BranchingNode, HashMap<Integer, Integer>> max_node = Pair.create(null, null);
			for (Pair<BranchingNode, HashMap<Integer, Integer>> candidate: candidates) {
				if (candidate.getLeft().getDepth() > max_depth) {
					max_depth = candidate.getLeft().getDepth();
					max_node = candidate;
				}
			}
			this.last_max_nodes.push(max_node);
			return max_node.getLeft();
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
