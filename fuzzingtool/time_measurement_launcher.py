#!/usr/bin/env python3
import time
import argparse
import subprocess
import os

NUM_ITERATIONS = 30
NUM_SAMPLES = 10
INPUT_VAL = "1"
TYPE = "non_instrumented"
OUTPUT_FILE = ""

MAIN_CLASS = "org.fuzzingtool.wrapper.TimeMeasurement"
BUILD_DIRECTORY = "build"
WRAPPER = "wrapper-1.0-SNAPSHOT.jar"
INSTRUMENTATION = "instrumentation-1.0-SNAPSHOT.jar"
CORE = "core-1.0-SNAPSHOT.jar"
DEPENDENCIES = []
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

def main():
	parser = argparse.ArgumentParser(description="Run fuzzing tool")
	parser.add_argument("program", metavar="PROGRAM", help="The Program to test.")
	args = parser.parse_args()

	environ_java_home = os.getenv("JAVA_HOME")
	if environ_java_home is None:
		print("ERROR: JAVA_HOME Variable not set.")
		exit(1)

	program_path = args.program
	
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
		program_path,
		str(NUM_ITERATIONS),
		str(INPUT_VAL),
		TYPE,
		OUTPUT_FILE
	]

	for sample in range(NUM_SAMPLES):
		print(f"Sample {sample + 1}/{NUM_SAMPLES}")
		subprocess.run(args, capture_output=True)


if __name__ == "__main__":
	main()
