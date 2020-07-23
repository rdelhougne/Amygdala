package org.fuzzingtool;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Wrapper {
	public static void main(String[] args) {
		final String source_file_path = "../graalfuzzing/Testprogramme/javascript/factorial.js";
		String source_code = "";
		try {
			source_code = Files.readString(Paths.get(source_file_path));
		} catch (IOException e) {
			System.out.println("[ERROR] Cannot open file");
			e.printStackTrace();
		}

		if (Engine.create().getLanguages().containsKey("js")) {
			// .option(SimpleCoverageInstrument.ID, "true").option(SimpleCoverageInstrument.ID + ".PrintCoverage", "false")
			Context context = Context.newBuilder("js").build();
			Source source = null;
			try {
				source = Source.newBuilder("js", source_code, "main").build();
			} catch (IOException e) {
				e.printStackTrace();
			}
			context.eval(source);

			Value jsBindings = context.getBindings("js");
			//SpecialInteger specint = new SpecialInteger();
			//jsBindings.putMember("k", specint);

			context.eval("js", source_code);
		} else {
			System.out.println("[ERROR] No JavaScript implementation present.");
		}
	}
}
