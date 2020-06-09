#!/usr/bin/env python3

import sys
import subprocess
import testgenerator

def main(iterations):
	for i in range(iterations):
		print("Iteration " + str(i + 1))
		expr = testgenerator.generate_simple_expression((1,19))
		args = ["js", "-e", expr]
		status = subprocess.run(args, stderr=subprocess.STDOUT, stdout=subprocess.PIPE)
		if (status.returncode != 0):
			print("### ERROR ###")
			print("Expression: " + expr)
			print("Message:")
			print(status.stdout.decode("utf-8"), end='')


if __name__ == "__main__":
	main(int(sys.argv[1]))
