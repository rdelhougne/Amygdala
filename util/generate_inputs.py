#!/usr/bin/env python3

# Copyright 2021 Robert Delhougne
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import re
import random
import yaml

FILE = ""
RANDOM_SAMPLES = True

IDENTIFIER_PATTERN = r"\s*(this\.|var\s+)(\w+)\s*=\s*(\w+);\s*\/\/ input"
MATCHER = re.compile(IDENTIFIER_PATTERN)


def get_random_int():
	return random.randint(0, 128)


def get_random_boolean():
	return random.choice([True, False])


def extract_inputs():
	inputs = []
	with open(FILE, "r") as programfile:
		programfile_content = programfile.read().splitlines()
		for line_num, content in enumerate(programfile_content):
			res = MATCHER.fullmatch(content)
			if res is not None:
				var_name = res.group(2)
				var_sample = res.group(3)

				if var_sample == "true" or var_sample == "false":
					var_type = "BOOLEAN"
					if (RANDOM_SAMPLES):
						var_sample = get_random_boolean()
					else:
						var_sample = True
				if var_sample == "42":
					var_type = "INTEGER"
					if (RANDOM_SAMPLES):
						var_sample = get_random_int()
					else:
						var_sample = 42

				var_spec = {
					"line_num": line_num + 1,
					"name": var_name,
					"type": var_type,
					"sample": var_sample
				}
				inputs.append(var_spec)
	return inputs


def print_inputs(inputs):
	print(yaml.dump({"variables": inputs}))
	print(f"Num inputs: {len(inputs)}")


def main():
	inputs = extract_inputs()
	print_inputs(inputs)


if __name__ == "__main__":
	main()
