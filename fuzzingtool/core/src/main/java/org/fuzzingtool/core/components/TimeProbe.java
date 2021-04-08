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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TimeProbe {
	private final Map<ProgramState, Long> durations;
	private long iteration_duration = 0;

	private long last_timestamp;
	private long last_iteration_timestamp = 0;
	private ProgramState current_state;
	private final boolean only_iteration;

	public TimeProbe(boolean only_iteration) {
		this.last_timestamp = System.nanoTime();
		this.current_state = ProgramState.MANAGE;
		this.only_iteration = only_iteration;

		this.durations = new HashMap<>();
		this.durations.put(ProgramState.MANAGE, (long) 0);
		this.durations.put(ProgramState.EXECUTION, (long) 0);
		this.durations.put(ProgramState.INSTRUMENTATION, (long) 0);
		this.durations.put(ProgramState.SOLVE, (long) 0);
		this.durations.put(ProgramState.TACTIC, (long) 0);
	}

	public void switchState(ProgramState new_state) {
		if (!only_iteration) {
			long time = System.nanoTime();
			long duration = time - last_timestamp;
			this.durations.put(current_state, this.durations.get(current_state) + duration);
			this.last_timestamp = time;
			this.current_state = new_state;
		}
	}

	/**
	 * Use this function to update the state AND measure iteration duration.
	 *
	 * @param new_state new state, preferably ProgramState.EXECUTION
	 */
	public void switchStateAndStartIteration(ProgramState new_state) {
		long time = System.nanoTime();
		this.last_iteration_timestamp = time;
		if (!only_iteration) {
			long duration = time - last_timestamp;
			this.durations.put(current_state, this.durations.get(current_state) + duration);
			this.last_timestamp = time;
			this.current_state = new_state;
		}
	}

	/**
	 * Use this function to update the state AND measure iteration duration.
	 *
	 * @param new_state new state, preferably ProgramState.MANAGE
	 */
	public void switchStateAndEndIteration(ProgramState new_state) {
		long time = System.nanoTime();
		this.iteration_duration = time - this.last_iteration_timestamp;
		if (!only_iteration) {
			long duration = time - last_timestamp;
			this.durations.put(current_state, this.durations.get(current_state) + duration);
			this.last_timestamp = time;
			this.current_state = new_state;
		}
	}

	public long getIterationDuration() {
		return this.iteration_duration;
	}

	public long getDuration(ProgramState state) {
		return this.durations.get(state);
	}

	public enum ProgramState {
		MANAGE,
		EXECUTION,
		INSTRUMENTATION,
		SOLVE,
		TACTIC,
		STOP
	}

	public String toString() {
		StringBuilder duration_str = new StringBuilder();
		duration_str.append("===RUNTIME BEHAVIOR===\n");
		if (only_iteration) {
			duration_str.append("Precise measurement disabled\n");
		} else {
			long complete_duration = 0;
			for (Map.Entry<ProgramState, Long> pair: this.durations.entrySet()) {
				complete_duration += pair.getValue() / 1000000;
			}
			for (Map.Entry<ProgramState, Long> pair: this.durations.entrySet()) {
				duration_str.append(pair.getKey().toString()).append(": ");
				duration_str.append(pair.getValue() / 1000000).append("ms ");
				double percentage = ((double) (pair.getValue() / 1000000) / (double) complete_duration) * 100.0;
				duration_str.append("(").append(String.format(Locale.US, "%.1f", percentage)).append("%)\n");
			}
		}
		return duration_str.toString();
	}
}
