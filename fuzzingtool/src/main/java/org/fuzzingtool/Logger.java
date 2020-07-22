package org.fuzzingtool;

import java.io.OutputStream;
import java.io.PrintStream;

public class Logger {
	final PrintStream outStream;

	// logging statistics
	private static final boolean SHOW_DEBUGGING_STATS = true;
	private int num_log = 0;
	private int num_debug = 0;
	private int num_info = 0;
	private int num_warning = 0;
	private int num_critical = 0;
	private int num_alert = 0;
	private int num_mesmerize = 0;
	private int num_shock = 0;
	private int num_highlight = 0;
	private int num_hypnotize = 0;
	private int num_fascinate = 0;


	public Logger(OutputStream os) {
		this.outStream = new PrintStream(os);
	}

	public void log(String msg) {
		outStream.println(msg);
		num_log++;
	}

	public void debug(String msg) {
		outStream.println("\033[35m[DEBUG]\033[0m " + msg);
		num_debug++;
	}

	public void info(String msg) {
		outStream.println("\033[34m[INFO]\033[0m " + msg);
		num_info++;
	}

	public void warning(String msg) {
		outStream.println("\033[33m[WARNING]\033[0m " + msg);
		num_warning++;
	}

	public void critical(String msg) {
		outStream.println("\033[31m[CRITICAL]\033[0m " + msg);
		num_critical++;
	}

	public void alert(String msg) {
		outStream.println("\033[41m" + msg + "\033[0m");
		num_alert++;
	}

	public void mesmerize(String msg) {
		outStream.println("\033[42m" + msg + "\033[0m");
		num_mesmerize++;
	}

	public void shock(String msg) {
		outStream.println("\033[43m" + msg + "\033[0m");
		num_shock++;
	}

	public void highlight(String msg) {
		outStream.println("\033[44m" + msg + "\033[0m");
		num_highlight++;
	}

	public void hypnotize(String msg) {
		outStream.println("\033[45m" + msg + "\033[0m");
		num_hypnotize++;
	}

	public void fascinate(String msg) {
		outStream.println("\033[46m" + msg + "\033[0m");
		num_fascinate++;
	}



	public void printStatistics() {
		outStream.println("===MESSAGE STATISTICS===");
		outStream.println("LOG: " + num_log);
		outStream.println("DEBUG: " + num_debug);
		outStream.println("INFO: " + num_info);
		outStream.println("WARNING: " + num_warning);
		outStream.println("CRITICAL: " + num_critical);
		if (SHOW_DEBUGGING_STATS) {
			outStream.println("ALERT: " + num_alert);
			outStream.println("MESMERIZE: " + num_mesmerize);
			outStream.println("SHOCK: " + num_shock);
			outStream.println("HIGHLIGHT: " + num_highlight);
			outStream.println("HYPNOTIZE: " + num_hypnotize);
			outStream.println("FASCINATE: " + num_fascinate);
		}
	}
}
