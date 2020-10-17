#!/usr/bin/env python3
import time
import signal
import argparse
import subprocess
import os
import yaml

import testgenerator


MEASURE_TIME = False
NUM_SAMPLES = 10


MAIN_CLASS = "org.fuzzingtool.wrapper.Wrapper"
BUILD_DIRECTORY = "build"
WRAPPER = "wrapper-1.0-SNAPSHOT.jar"
INSTRUMENTATION = "instrumentation-1.0-SNAPSHOT.jar"
CORE = "core-1.0-SNAPSHOT.jar"
DEPENDENCIES = [
	"/home/robert/.m2/repository/org/snakeyaml/snakeyaml-engine/2.1/snakeyaml-engine-2.1.jar",
	"/home/robert/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar",
	"/home/robert/.m2/repository/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar",
	"/home/robert/.m2/repository/org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar",
	"/home/robert/.m2/repository/org/slf4j/slf4j-api/1.7.7/slf4j-api-1.7.7.jar",
	"/home/robert/.m2/repository/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar",
	"/home/robert/.m2/repository/org/apache/logging/log4j/log4j-api/2.13.0/log4j-api-2.13.0.jar",
	"/home/robert/.m2/repository/org/apache/logging/log4j/log4j-core/2.13.0/log4j-core-2.13.0.jar",
	"/home/robert/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.13.0/log4j-slf4j-impl-2.13.0.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-lang3/3.7/commons-lang3-3.7.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-lang3/3.9/commons-lang3-3.9.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-compress/1.14/commons-compress-1.14.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-compress/1.19/commons-compress-1.19.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-compress/1.9/commons-compress-1.9.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-text/1.8/commons-text-1.8.jar",
	"/home/robert/.m2/repository/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar",
	"/home/robert/.m2/repository/guru/nidi/com/kitfox/svgSalamander/1.1.3/svgSalamander-1.1.3.jar",
	"/home/robert/.m2/repository/guru/nidi/graphviz-java/0.16.1/graphviz-java-0.16.1.jar",
	"/home/robert/.m2/repository/com/microsoft/z3/4.8.7/z3-4.8.7.jar"
]
TRUFFLE_JS = [
	"/opt/graalvm/jre/languages/js/graaljs.jar",
	"/opt/graalvm/jre/languages/js/asm7.1.jar",
	"/opt/graalvm/jre/languages/js/asm-analysis-7.1.jar",
	"/opt/graalvm/jre/languages/js/asm-commons-7.1.jar",
	"/opt/graalvm/jre/languages/js/asm-tree-7.1.jar",
	"/opt/graalvm/jre/languages/js/asm-util-7.1.jar",
	"/opt/graalvm/jre/languages/js/icu4j.jar",
	"/opt/graalvm/jre/languages/js/trufflenode.jar"
]

TERMINATE_FILE_NAME = "terminate-1bfa427b-a460-4088-b578-e388a6bce94d"

fuzzing_process = None

def signal_handler(sig, frame):
	if fuzzing_process is not None:
		open(TERMINATE_FILE_NAME, 'a')
signal.signal(signal.SIGINT, signal_handler)

def generate_test_program(path):
	with open(path, "w") as new_file:
		new_file.write(testgenerator.generate_program((2,4), (1,1), (1,2), (1,2)))

def main():
	global fuzzing_process
	parser = argparse.ArgumentParser(description="Run fuzzing tool")
	parser.add_argument("-v", "--verbose", action="store_true", help="increases verbosity.")
	parser.add_argument("-g", "--generate", action="store_true", help="Generate the program.")
	parser.add_argument("configuration", metavar="PROGRAM", help="The Program configuration file (yaml) to test.")
	args = parser.parse_args()

	environ_java_home = os.getenv("JAVA_HOME")
	if environ_java_home is None:
		print("ERROR: JAVA_HOME Variable not set.")
		exit(1)

	fuzzing_configuration = args.configuration
	generate = args.generate

	program_path = ""
	runtime_fractional_output = ""

	# Check if program exists
	try:
		with open(fuzzing_configuration, "r") as cfile:
			try:
				yaml_dict = yaml.safe_load(cfile)
				program_path = yaml_dict["program_path"]
				if "runtime_fractional_output" in yaml_dict:
					runtime_fractional_output = yaml_dict["runtime_fractional_output"]
			except yaml.YAMLError as ye:
				print("ERROR: Cannot parse " + fuzzing_configuration + ".")
				exit(1)
	except FileNotFoundError as fnfe:
		print("ERROR: File " + fuzzing_configuration + " not found.")
		exit(1)
	except:
		print("ERROR: Cannot open file " + fuzzing_configuration + ".")
		exit(1)

	if not os.path.isabs(program_path):
		working_dir, source_file_name = os.path.split(fuzzing_configuration)
		program_path = os.path.join(working_dir, program_path)

	if generate:
		if os.path.exists(program_path):
			choice = str(input("The program file already exists, overwrite? [y|N]: "))
			if choice == "Y" or choice == "y":
				generate_test_program(program_path)
		else:
			generate_test_program(program_path)

	if not os.path.exists(program_path):
		print("ERROR: File not found: " + program_path)
		exit(1)
	
	dtruffle_classpaths = os.path.join(BUILD_DIRECTORY, INSTRUMENTATION) + ":" + \
							os.path.join(BUILD_DIRECTORY, CORE) + ":" + \
							":".join(DEPENDENCIES)
	java_classpaths = os.path.join(BUILD_DIRECTORY, WRAPPER) + ":" \
						+ os.path.join(BUILD_DIRECTORY, INSTRUMENTATION) + ":" + \
						os.path.join(BUILD_DIRECTORY, CORE) + ":" + \
						":".join(DEPENDENCIES) + ":" + \
						":".join(TRUFFLE_JS)

	engine_exec_path = os.path.join(environ_java_home, "bin", "java")

	args = [
		engine_exec_path,
		"-XX:-UseJVMCIClassLoader",
		"-Xss128m",
		"-Dgraalvm.locatorDisabled=true",
		"-Dtruffle.class.path.append=" + dtruffle_classpaths,
		"-cp", java_classpaths,
		MAIN_CLASS,
		fuzzing_configuration
	]
	
	if MEASURE_TIME:
		for sample in range(NUM_SAMPLES):
			print(f"Sample {sample + 1}/{NUM_SAMPLES}")
			iter_start = time.perf_counter_ns()
			subprocess.run(args, capture_output=True)
			iter_end = time.perf_counter_ns()
			total_duration = iter_end - iter_start
			if runtime_fractional_output != "":
				with open(runtime_fractional_output, 'a+') as outfile:
					outfile.seek(0)
					outfile_contents = outfile.read()
					last_line = outfile_contents.split('\n')[-1].split(',')
					total_probed_duration = sum([int(x) for x in last_line])
					vm_duration = total_duration - total_probed_duration
					measure_string = f",{vm_duration},{total_duration}\n"
					outfile.write(measure_string)
	else:
		fuzzing_process = subprocess.Popen(args, preexec_fn=os.setpgrp)
		fuzzing_process.wait()

	


if __name__ == "__main__":
	main()
