package org.fuzzingtool.wrapper;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimeMeasurement {
	public static void main(String[] args) {
		/* Options
		 * 0: program path
		 * 1: num iterations
		 * 2: input value
		 * 3: type
		 * 4: output path
		 */
		if (args.length != 5) {
			System.out.println("ERROR: Wrong number of options");
			return;
		}

		Engine engine = Engine.newBuilder().build();
		Context context = Context.newBuilder("js").engine(engine).build();
		Source source = loadSource(args[0]);

		int num_iterations = Integer.parseInt(args[1]);
		String input_val = args[2];
		String type = args[3];

		// Measure
		List<Long> durations = new ArrayList<>();

		for (int i = 0; i < num_iterations; i++) {
			System.out.println("Iteration " + (i + 1) + "/" + num_iterations);
			long start_time = 0;
			long end_time = 0;
			try {
				start_time = System.nanoTime();
				context.eval(source);
				end_time = System.nanoTime();
			} catch (PolyglotException pe) {
				pe.printStackTrace();
				end_time = System.nanoTime();
			}
			durations.add(end_time - start_time);
		}

		/*System.out.println("===DURATIONS===");
		for (int i = 0; i < num_iterations; i++) {
			System.out.println(durations.get(i));
		}*/

		try {
			FileWriter fw = new FileWriter(args[4], true);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < num_iterations; i++) {
				String measure_string = type + "," + input_val + "," + (i + 1) + "," + durations.get(i);
				bw.write(measure_string);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Source loadSource(String source_path) {
		File source_file = new File(source_path);
		String language = null;
		Source source = null;
		try {
			language = Source.findLanguage(source_file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			source = Source.newBuilder(language, source_file).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return source;
	}
}
