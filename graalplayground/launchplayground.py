#!/usr/bin/env python3
import argparse
import subprocess
import os

FUZZINGTOOL_EXEC = "target/graalplayground-1.0-SNAPSHOT.jar"
TESTING_ARGUMENTS = ""
DEPENDENCY_PACKAGES = ["guru.nidi", "org.slf4j.slf4j-api","org.apache.logging.log4j.log4j-slf4j-impl","org.apache.logging.log4j.log4j-api", "org.apache.logging.log4j.log4j-core", "org.apache.commons", "org.snakeyaml"]
DEPENDENCY_REPOSITORY = "/home/robert/.m2/repository"
ADDITIONAL_DEPENDENCIES = ["/usr/share/java/com.microsoft.z3.jar"]

def get_all_dependencies():
    classpaths = []
    for root, dirs, files in os.walk(DEPENDENCY_REPOSITORY):
        for file_name in files:
            if file_name.endswith(".jar"):
                for groupid in DEPENDENCY_PACKAGES:
                    expected_path = os.path.join(DEPENDENCY_REPOSITORY, groupid.replace(".", "/"))
                    if root.startswith(expected_path):
                        classpaths.append(os.path.join(root, file_name))
    return classpaths

def get_additional_dependencies():
    classpaths = []
    for root, dirs, files in os.walk(ADDITIONAL_DEPENDENCIES):
        for file_name in files:
            if file_name.endswith(".jar"):
                classpaths.append(os.path.join(root, file_name))
    return classpaths

def main():
    parser = argparse.ArgumentParser(description="Run fuzzing tool")
    parser.add_argument("-v", "--verbose", action="store_true", help="increases verbosity.")
    parser.add_argument("engine", metavar="ENGINE", help="The GraalVM Engine to run the program.")
    parser.add_argument("program", metavar="PROGRAM", help="The Program file to test.")
    args = parser.parse_args()

    environ_java_home = os.getenv("JAVA_HOME")
    if environ_java_home is None:
        print("ERROR: JAVA_HOME Variable not set.")
        exit(1)

    engine = args.engine
    program_path = args.program
    
    if not os.path.exists(program_path):
        print("File not found: " + program_path)
        exit(1)
    
    dependency_classpaths = get_all_dependencies()
    dependency_classpaths += ADDITIONAL_DEPENDENCIES
    classpath_string = ""
    for cp_index in range(len(dependency_classpaths)):
        if cp_index == 0:
            classpath_string += f'{dependency_classpaths[cp_index]}'
        else:
            classpath_string += f':{dependency_classpaths[cp_index]}'

    engine_exec_path = os.path.join(environ_java_home, "bin", engine)

    args = [
            engine_exec_path,
            "--polyglot",
            "--jvm",
            "--graalplayground",
            '--vm.Dtruffle.class.path.append=' + FUZZINGTOOL_EXEC + ':' + classpath_string,
            program_path
           ]

    subprocess.run(args)


if __name__ == "__main__":
    main()
