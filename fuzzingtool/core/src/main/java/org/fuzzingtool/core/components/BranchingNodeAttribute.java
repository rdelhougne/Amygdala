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

package org.fuzzingtool.core.components;

public enum BranchingNodeAttribute {
	BRANCH, // This node is a simple branch (e.g. if)
	LOOP, // This node is a loop predicate, important for loop unrolling limits
	// The following Nodes can't have any children
	UNKNOWN, // The following path was never taken
	UNREACHABLE, // The SMT-Solver was unable to solve the expression for this path
	TERMINATE, // The program terminates in this node
	ERROR // This path causes the program to crash
}
