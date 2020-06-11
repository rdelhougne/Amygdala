#!/usr/bin/env python3
import argparse
import subprocess
import os
import uuid
import yaml

FUZZINGTOOL_EXEC = "target/fuzzingtool-1.0-SNAPSHOT.jar"
TESTING_ARGUMENTS = ""
DEPENDENCY_PACKAGES = ["guru.nidi", "org.slf4j.slf4j-api","org.apache.logging.log4j.log4j-slf4j-impl","org.apache.logging.log4j.log4j-api", "org.apache.logging.log4j.log4j-core", "org.apache.commons", "com.amihaiemil.web"]
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
    line_offset = 0
    with open(abs_path, "r") as sourcefile:
        source_contents = str(sourcefile.read())
    if source_contents.startswith("\"use strict\";"):
        new_source = "\"use strict\";\nwhile(true) { //" + str(main_loop_identifier) + "\ntry {\n\n" + source_contents
        main_loop_line_num = 2
        line_offset = 4
    else:
        new_source = "while(true) { //" + str(main_loop_identifier) + "\ntry {\n\n" + source_contents
        main_loop_line_num = 1
        line_offset = 3
    error_line_num = len(new_source.split("\n")) + 2
    new_source = new_source + "\n} catch(ex_" + str(error_identifier.fields[5]) + ") {\nprint(ex_" + str(error_identifier.fields[5]) + ") //" + str(error_identifier) + "\n}\n}"
    
    working_dir, source_file_name = os.path.split(abs_path)
    new_filename = source_file_name.split(".")[0] + "_" + str(filename_uid) + ".js"
    new_filepath = os.path.join(working_dir, new_filename)
    with open(new_filepath, "w") as nf:
        nf.write(new_source)
    return new_filepath, main_loop_line_num, main_loop_identifier, error_line_num, error_identifier, line_offset

def main():
    parser = argparse.ArgumentParser(description="Run fuzzing tool")
    parser.add_argument("-v", "--verbose", action="store_true", help="increases verbosity.")
    parser.add_argument("engine", metavar="ENGINE", help="The GraalVM Engine to run the program.")
    parser.add_argument("configuration", metavar="PROGRAM", help="The Program configuration file (yaml) to test.")
    args = parser.parse_args()

    environ_java_home = os.getenv("JAVA_HOME")
    if environ_java_home is None:
        print("ERROR: JAVA_HOME Variable not set.")
        exit(1)

    engine = args.engine
    fuzzing_configuration = args.configuration
    
    program_path = ""
    
    with open(fuzzing_configuration, "r") as cfile:
        try:
            yaml_dict = yaml.safe_load(cfile)
            program_path = yaml_dict["program_path"]
        except yaml.YAMLError as exc:
            exit(1)
    
    if not os.path.isabs(program_path):
        working_dir, source_file_name = os.path.split(fuzzing_configuration)
        program_path = os.path.join(working_dir, program_path)
    
    dependency_classpaths = get_all_dependencies()
    dependency_classpaths += get_additional_dependencies()
    classpath_string = ""
    for cp_index in range(len(dependency_classpaths)):
        if cp_index == 0:
            classpath_string += f'{dependency_classpaths[cp_index]}'
        else:
            classpath_string += f':{dependency_classpaths[cp_index]}'

    engine_exec_path = os.path.join(environ_java_home, "bin", engine)
    
    new_filepath, main_loop_line_num, main_loop_identifier, error_line_num, error_identifier, source_code_line_offset = prepare_js_program(program_path)

    args = [
            engine_exec_path,
            "--polyglot",
            "--jvm",
            '--vm.Dtruffle.class.path.append=' + FUZZINGTOOL_EXEC + ':' + classpath_string,
            "--fuzzingtool",
            "--fuzzingtool.mainLoopLineNumber=" + str(main_loop_line_num),
            "--fuzzingtool.mainLoopIdentString=" + str(main_loop_identifier),
            "--fuzzingtool.errorLineNumber=" + str(error_line_num),
            "--fuzzingtool.errorIdentString=" + str(error_identifier),
            "--fuzzingtool.optionFile=" + fuzzing_configuration,
            "--fuzzingtool.sourceCodeLineOffset=" + str(source_code_line_offset),
            new_filepath
           ]

    subprocess.run(args)

    os.remove(new_filepath)


if __name__ == "__main__":
    main()
