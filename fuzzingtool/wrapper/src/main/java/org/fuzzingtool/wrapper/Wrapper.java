/*
 * Copyright 2021 Robert Delhougne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fuzzingtool.wrapper;

public class Wrapper {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("ERROR: Wrong number of options");
			return;
		}

		/*Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Shutdown hook ran!");
			}
		});*/

		Fuzzer fuzzer = new Fuzzer(args[0]);
		if (fuzzer.usable()) {
			fuzzer.fuzz();
			fuzzer.saveResults();
			fuzzer.printResults();
			fuzzer.saveAndPrintRuntimeInformation();
		}
	}
}
