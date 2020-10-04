package org.fuzzingtool.core.tactics;

import com.microsoft.z3.*;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;

import java.util.*;

public class RandomSearchTactic extends FuzzingTactic {
	private final BranchingNode root_node;
	private final Random generator;

	public RandomSearchTactic(BranchingNode n, Context c, Logger l) {
		this.ctx = c;
		this.logger = l;
		this.root_node = n;
		this.generator = new Random();
	}

	protected BranchingNode findCandidate(BranchingNode current_node) {
		BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

		incrementLoop(current_node);

		if (!isValidNode(current_node)) {
			decrementLoop(current_node);
			return null;
		}

		if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
			boolean taken = generator.nextBoolean();
			BranchingNode child_node = current_node.getChildBranch(taken);
			BranchingNode child_target_result = findCandidate(child_node);
			if (child_target_result != null) {
				return child_target_result;
			} else {
				BranchingNode alternative_child_node = current_node.getChildBranch(!taken);
				BranchingNode alternative_child_target_result = findCandidate(alternative_child_node);
				if (alternative_child_target_result != null) {
					return alternative_child_target_result;
				}
				decrementLoop(current_node);
				return null;
			}
		} else if (node_type == BranchingNodeAttribute.UNKNOWN) {
			return current_node;
		} else {
			return null;
		}
	}

	@Override
	protected BranchingNode findUnexplored() {
		return findCandidate(root_node);
	}

	@Override
	public void setOption(String option_name, Object value) {
		switch (option_name) {
			case "max_loop_unrolling":
				try {
					this.max_loop_unrolling = (Integer) value;
					logger.info("RANDOM_SEARCH.max_loop_unrolling option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("RandomSearchTactic: Wrong parameter type for option 'max_loop_unrolling' (Integer).");
				}
				break;
			case "max_depth":
				try {
					this.max_depth = (Integer) value;
					logger.info("RANDOM_SEARCH.max_depth option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("RandomSearchTactic: Wrong parameter type for option 'max_depth' (Integer).");
				}
				break;
			case "seed":
				try {
					this.generator.setSeed((Long) value);
					logger.info("RANDOM_SEARCH.seed option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("RandomSearchTactic: Wrong parameter type for option 'seed' (Long).");
				}
				break;
			default:
				logger.warning("RANDOM_SEARCH: Unknown option '" + option_name + "'.");
		}
	}

	@Override
	public Object getOption(String option_name) {
		switch (option_name) {
			case "max_loop_unrolling":
				return this.max_loop_unrolling;
			case "max_depth":
				return this.max_depth;
			case "seed":
				logger.warning("RandomSearchTactic: Cannot get option 'seed'.");
				return null;
			default:
				logger.warning("RandomSearchTactic: Unknown option '" + option_name + "'.");
				return null;
		}
	}

	@Override
	public String getTactic() {
		return "RANDOM_SEARCH";
	}
}
