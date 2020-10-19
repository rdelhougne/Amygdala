#!/usr/bin/env python3
import argparse
import subprocess
import os
import time

NUM_ITERATIONS = 30
NUM_SAMPLES = 10
INPUT_VAL = "1"
TYPE = "vm_restart"
OUTPUT_FILE = ""


def main():
	parser = argparse.ArgumentParser(description="Run fuzzing tool")
	parser.add_argument("configuration", metavar="PROGRAM", help="The Program to test.")
	args = parser.parse_args()

	environ_java_home = os.getenv("JAVA_HOME")
	if environ_java_home is None:
		print("ERROR: JAVA_HOME Variable not set.")
		exit(1)

	fuzzing_configuration = args.configuration
	
	engine_exec_path = os.path.join(environ_java_home, "bin", "js")

	args = [
		engine_exec_path,
		"--vm.Xss128m",
		fuzzing_configuration
	]
	iteration_durations = []

	for sample in range(NUM_SAMPLES):
		for iteration in range(NUM_ITERATIONS):
			print(80 * " ",  end='\r')
			print(f"Sample {sample + 1}/{NUM_SAMPLES} Iteration {iteration + 1}/{NUM_ITERATIONS}", end='\r')
			iter_start = time.perf_counter_ns()
			subprocess.run(args, capture_output=True)
			iter_end = time.perf_counter_ns()
			iteration_durations.append((iteration + 1, iter_end - iter_start))
	
	with open(OUTPUT_FILE, 'a') as outfile:
		for measurement in iteration_durations:
			measure_string = f"{TYPE},{INPUT_VAL},{measurement[0]},{measurement[1]}\n"
			outfile.write(measure_string)
	
	print("")


if __name__ == "__main__":
	main()
