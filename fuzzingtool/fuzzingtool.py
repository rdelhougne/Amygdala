#!/usr/bin/env python3
import argparse
import subprocess
import os
import uuid

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

def prepare_js_program(path):
    abs_path = os.path.abspath(path)
    filename_uid = uuid.uuid4()
    main_loop_identifier = uuid.uuid4()
    error_identifier = uuid.uuid4()
    main_loop_line_num = 0
    error_line_num = 0
    with open(abs_path, "r") as sourcefile:
        source_contents = str(sourcefile.read())
    if source_contents.startswith("\"use strict\";"):
        new_source = "\"use strict\";\nwhile(true) { //" + str(main_loop_identifier) + "\ntry {\n\n" + source_contents
        main_loop_line_num = 2
    else:
        new_source = "while(true) { //" + str(main_loop_identifier) + "\ntry {\n\n" + source_contents
        main_loop_line_num = 1
    error_line_num = len(new_source.split("\n")) + 2
    new_source = new_source + "\n} catch(ex_" + str(error_identifier.fields[5]) + ") {\nprint(ex_" + str(error_identifier.fields[5]) + ") //" + str(error_identifier) + "\n}\n}"
    
    working_dir, source_file_name = os.path.split(abs_path)
    new_filename = source_file_name.split(".")[0] + "_" + str(filename_uid) + ".js"
    new_filepath = os.path.join(working_dir, new_filename)
    with open(new_filepath, "w") as nf:
        nf.write(new_source)
    return new_filepath, main_loop_line_num, main_loop_identifier, error_line_num, error_identifier

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
    
    dependency_classpaths = get_all_dependencies()
    dependency_classpaths += get_additional_dependencies()
    classpath_string = ""
    for cp_index in range(len(dependency_classpaths)):
        if cp_index == 0:
            classpath_string += f'{dependency_classpaths[cp_index]}'
        else:
            classpath_string += f':{dependency_classpaths[cp_index]}'

    engine_exec_path = os.path.join(environ_java_home, "bin", engine)
    
    new_filepath, main_loop_line_num, main_loop_identifier, error_line_num, error_identifier = prepare_js_program(program)

    args = [
            engine_exec_path,
            "--jvm",
            '--vm.Dtruffle.class.path.append=' + FUZZINGTOOL_EXEC + ':' + classpath_string,
            "--fuzzingtool",
            "--fuzzingtool.mainLoopLineNumber=" + str(main_loop_line_num),
            "--fuzzingtool.mainLoopIdentString=" + str(main_loop_identifier),
            "--fuzzingtool.errorLineNumber=" + str(error_line_num),
            "--fuzzingtool.errorIdentString=" + str(error_identifier),
            new_filepath
           ]

    subprocess.run(args)
    
    os.remove(new_filepath)


if __name__ == "__main__":
    main()
