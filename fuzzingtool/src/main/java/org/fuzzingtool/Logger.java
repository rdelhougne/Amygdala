package org.fuzzingtool;

import java.io.OutputStream;
import java.io.PrintStream;

public class Logger {
    PrintStream outStream;

    public Logger(OutputStream os) {
        this.outStream = new PrintStream(os);
    }

    public void log(String msg) {
        outStream.println(msg);
    }

    public void debug(String msg) {
        outStream.println("\033[35m[DEBUG]\033[0m " + msg);
    }

    public void info(String msg) {
        outStream.println("\033[34m[INFO]\033[0m " + msg);
    }

    public void warning(String msg) {
        outStream.println("\033[33m[WARNING]\033[0m " + msg);
    }

    public void critical(String msg) {
        outStream.println("\033[31m[CRITICAL]\033[0m " + msg);
    }

    public void alert(String msg) {
        outStream.println("\033[41m!ALERT! " + msg + "\033[0m");
    }

    public void highlight(String msg) {
        outStream.println("\033[44m" + msg + "\033[0m");
    }
}
