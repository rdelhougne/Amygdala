package org.fuzzingtool.wrapper;

import org.fuzzingtool.instrumentation.FuzzingTool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Wrapper {
	public static void main(String[] args) {
		final String source_file_path = "test.js";
		String source_code = "";
		try {
			source_code = Files.readString(Paths.get(source_file_path));
		} catch (IOException e) {
			System.out.println("[ERROR] Cannot open file");
			e.printStackTrace();
		}

		//ModuleLayer.boot().modules().stream().map(Module::getName).forEach(System.out::println);
		for (String lang: Engine.create().getLanguages().keySet()) {
			System.out.println(lang);
		}

		//if (Engine.create().getLanguages().containsKey("js")) {
			Context context = Context.newBuilder("js").option(FuzzingTool.ID, "true").build();
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
		//} else {
			//System.out.println("[ERROR] No JavaScript implementation present.");
		//}
	}
}
