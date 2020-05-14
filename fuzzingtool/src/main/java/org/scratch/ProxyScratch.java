package org.scratch;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

class SpecialInteger implements Proxy {

}

/*class ComputedArray implements ProxyArray {
	public Object get(long index) {
		return index * 2;
	}
	public void set(long index, Value value) {
		throw new UnsupportedOperationException();
	}
	public long getSize() {
		return Long.MAX_VALUE;
	}
}*/

public class ProxyScratch {
    public static void main(String[] args) {
        final String source_file_path = "../Testprogramme/javascript/factorial.js";
        String source_code = "";
        try {
            source_code = Files.readString(Paths.get(source_file_path));
        } catch (IOException e) {
            System.out.println("[ERROR] Cannot open file");
            e.printStackTrace();
        }
        //System.out.println(source_code);

        Context context = Context.newBuilder("js")
                .option("js.strict", "true")
                .allowAllAccess(true)
                .build();
        Value jsBindings = context.getBindings("js");
        SpecialInteger specint = new SpecialInteger();
        int myNum = 42;
        jsBindings.putMember("specint", specint);
        jsBindings.putMember("inj", myNum);
        Value result = context.eval("js", source_code);
        System.out.println(jsBindings.getMember("inj").asInt());
        System.out.println(jsBindings.getMember("specint").asInt());
    }
}
