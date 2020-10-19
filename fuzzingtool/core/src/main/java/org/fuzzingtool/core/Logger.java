package org.fuzzingtool.core;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;

public class Logger {
	final PrintStream out_stream;
	final Queue<String> aggregate_queue = new CircularFifoQueue<>(8);

	// logging statistics
	private static final boolean SHOW_DEBUGGING_STATS = true;
	private static final boolean AGGREGATE_EVENTS = true;
	private int num_log = 0;
	private int num_event = 0;
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
		this.out_stream = new PrintStream(os);
	}

	public void log(String msg) {
		out_stream.println(msg);
		num_log++;
	}

	public void event(String msg) {
		if (AGGREGATE_EVENTS) {
			aggregate_queue.offer(msg);
		} else {
			out_stream.println(msg);
		}
		num_event++;
	}

	public void debug(String msg) {
		out_stream.println("\033[35m[DEBUG]\033[0m " + msg);
		num_debug++;
	}

	public void info(String msg) {
		out_stream.println("\033[34m[INFO]\033[0m " + msg);
		num_info++;
	}

	public void warning(String msg) {
		out_stream.println("\033[33m[WARNING]\033[0m " + msg);
		num_warning++;
	}

	public void critical(String msg) {
		if (AGGREGATE_EVENTS) {
			for (String message: aggregate_queue) {
				out_stream.println(message);
			}
		}
		out_stream.println("\033[31m[CRITICAL]\033[0m " + msg);
		num_critical++;
	}

	public void alert(String msg) {
		out_stream.println("\033[41m" + msg + "\033[0m");
		num_alert++;
	}

	public void mesmerize(String msg) {
		out_stream.println("\033[42m" + msg + "\033[0m");
		num_mesmerize++;
	}

	public void shock(String msg) {
		out_stream.println("\033[43m" + msg + "\033[0m");
		num_shock++;
	}

	public void highlight(String msg) {
		out_stream.println("\033[44m" + msg + "\033[0m");
		num_highlight++;
	}

	public void hypnotize(String msg) {
		out_stream.println("\033[45m" + msg + "\033[0m");
		num_hypnotize++;
	}

	public void fascinate(String msg) {
		out_stream.println("\033[46m" + msg + "\033[0m");
		num_fascinate++;
	}

	public static String capBack(String str, int max_size) {
		if (str.length() > max_size) {
			str = str.substring(0, max_size - 3) + "...";
		}
		return str;
	}

	public static String capFront(String str, int max_size) {
		if (str.length() > max_size) {
			str = "..." + str.substring(str.length() - max_size + 3);
		}
		return str;
	}

	public void printStatistics() {
		out_stream.println("===MESSAGE STATISTICS===");
		out_stream.println("LOG: " + num_log);
		out_stream.println("EVENT: " + num_event);
		out_stream.println("DEBUG: " + num_debug);
		out_stream.println("INFO: " + num_info);
		out_stream.println("WARNING: " + num_warning);
		out_stream.println("CRITICAL: " + num_critical);
		if (SHOW_DEBUGGING_STATS) {
			out_stream.println("ALERT: " + num_alert);
			out_stream.println("MESMERIZE: " + num_mesmerize);
			out_stream.println("SHOCK: " + num_shock);
			out_stream.println("HIGHLIGHT: " + num_highlight);
			out_stream.println("HYPNOTIZE: " + num_hypnotize);
			out_stream.println("FASCINATE: " + num_fascinate);
		}
		out_stream.println("");
	}
}
