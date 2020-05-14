#!/usr/bin/env python3
import argparse
import subprocess
import os

FUZZINGTOOL_EXEC = "target/fuzzingtool-1.0-SNAPSHOT.jar"
TESTING_ARGUMENTS = ""


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

    engine_exec_path = os.path.join(environ_java_home, "bin", engine)

    args = [
            engine_exec_path,
            "--jvm",
            "--vm.Dtruffle.class.path.append=" + FUZZINGTOOL_EXEC,
            "--fuzzing-tool",
            program
           ]

    subprocess.run(args)


if __name__ == "__main__":
    main()
