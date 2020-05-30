#!/usr/bin/env python3
import argparse
import subprocess
import os

FUZZINGTOOL_EXEC = "target/fuzzingtool-1.0-SNAPSHOT.jar"
TESTING_ARGUMENTS = ""
DEPENDENCY_PACKAGES = ["guru.nidi", "org.slf4j.slf4j-api","org.apache.logging.log4j.log4j-slf4j-impl","org.apache.logging.log4j.log4j-api", "org.apache.logging.log4j.log4j-core", "org.apache.commons"]
DEPENDENCY_REPOSITORY = "/home/robert/.m2/repository"
ADDITIONAL_DEPENDENCIES = "/home/robert/Seafile/Studium/Master Informatik/Masterarbeit/Projekt/Software"


def get_full_name(name):
    if "." in name:
        return name
    else:
        name_splitted = name.split(os.sep)
        path_joined = os.path.join(*name_splitted[:-1])
        files = [f for f in os.listdir(path_joined) if os.path.isfile(os.path.join(path_joined, f))]
        for f in files:
            if f.startswith(name_splitted[-1]):
                return os.path.join(path_joined, f)
        return name

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
    parser.add_argument("program", metavar="PROGRAM", help="The Program to test.")
    args = parser.parse_args()

    environ_java_home = os.getenv("JAVA_HOME")
    if environ_java_home is None:
        print("ERROR: JAVA_HOME Variable not set.")
        exit(1)

    engine = args.engine
    program = args.program

    #program = get_full_name(program)
    
    dependency_classpaths = get_all_dependencies()
    dependency_classpaths += get_additional_dependencies()
    classpath_string = ""
    for cp_index in range(len(dependency_classpaths)):
        if cp_index == 0:
            classpath_string += f'{dependency_classpaths[cp_index]}'
        else:
            classpath_string += f':{dependency_classpaths[cp_index]}'

    engine_exec_path = os.path.join(environ_java_home, "bin", engine)

    args = [
            engine_exec_path,
            "--jvm",
            '--vm.Dtruffle.class.path.append=' + FUZZINGTOOL_EXEC + ':' + classpath_string,
            "--fuzzing-tool",
            program
           ]

    subprocess.run(args)


if __name__ == "__main__":
    main()
