/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.core.tactics;

import com.microsoft.z3.Context;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;

public class InOrderSearchTactic extends FuzzingTactic {
	private final BranchingNode root_node;

	public InOrderSearchTactic(BranchingNode n, Context c, Logger l) {
		this.root_node = n;
		this.ctx = c;
		this.logger = l;
	}

	protected BranchingNode findCandidate(BranchingNode current_node) {
		BranchingNodeAttribute node_type = current_node.getBranchingNodeAttribute();

		incrementLoop(current_node);

		if (nodeIsInvalid(current_node)) {
			decrementLoop(current_node);
			return null;
		}

		if (node_type == BranchingNodeAttribute.BRANCH || node_type == BranchingNodeAttribute.LOOP) {
			for (Boolean taken_flag: new boolean[]{true, false}) {
				BranchingNode child_node = current_node.getChildBranch(taken_flag);
				BranchingNode child_target_result = findCandidate(child_node);
				if (child_target_result != null) {
					return child_target_result;
				}
			}
			decrementLoop(current_node);
			return null;
		} else if (node_type == BranchingNodeAttribute.UNKNOWN) {
			return current_node;
		} else {
			return null;
		}
	}

	@Override
	protected BranchingNode findUnexplored() {
		this.loop_unrolls.clear();
		return findCandidate(root_node);
	}

	@Override
	public void setOption(String option_name, Object value) {
		switch (option_name) {
			case "max_loop_unrolling":
				try {
					this.max_loop_unrolling = (Integer) value;
					logger.info("IN_ORDER_SEARCH.max_loop_unrolling option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("InOrderSearchTactic: Wrong parameter type for option 'max_loop_unrolling' (Integer)");
				}
				break;
			case "max_depth":
				try {
					this.max_depth = (Integer) value;
					logger.info("IN_ORDER_SEARCH.max_depth option set: " + value);
				} catch (ClassCastException cce) {
					logger.warning("InOrderSearchTactic: Wrong parameter type for option 'max_depth' (Integer)");
				}
				break;
			default:
				logger.warning("IN_ORDER_SEARCH: Unknown option '" + option_name + "'");
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
				logger.warning("InOrderSearchTactic: Unknown option '" + option_name + "'");
				return null;
		}
	}

	@Override
	public String getTactic() {
		return "IN_ORDER_SEARCH";
	}
}
