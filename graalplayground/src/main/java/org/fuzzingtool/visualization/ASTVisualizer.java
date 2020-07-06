package org.fuzzingtool.visualization;

import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizException;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import org.apache.commons.text.StringEscapeUtils;
import org.fuzzingtool.Logger;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.model.Factory.*;

public class ASTVisualizer {
	private final MutableGraph vis_graph = mutGraph("AST Visualization").setDirected(true);

	private final Logger logger;

	public ASTVisualizer(Node root_node, Logger l) {
		this.logger = l;
		vis_graph.graphAttrs().add("splines", "ortho");
		vis_graph.graphAttrs().add("nodesep", 0.5);
		vis_graph.graphAttrs().add("ordering", "out");
		vis_graph.graphAttrs().add("rankdir", "TB");
		vis_graph.graphAttrs().add("ranksep", 1.5);
		vis_graph.graphAttrs().add("fontname", "LiberationSans");
		vis_graph.graphAttrs().add("fontsize", 14);
		String node_type = root_node.getClass().getSimpleName();
		String visualization_node_name = String.valueOf(root_node.hashCode());
		MutableNode root =
				mutNode(visualization_node_name).add(getNodeContents(node_type, root_node.getSourceSection()));
		root.add(Shape.ELLIPSE, Style.FILLED, Color.rgb(0x72, 0x9f, 0xcf));
		vis_graph.add(root);
		build_visualization(root_node, root);
	}

	public void save_image(String path) {
		Graphviz.useEngine(new GraphvizCmdLineEngine());
		try {
			logger.info("Saving AST-visualization to file '" + path + ".svg'");
			Graphviz.fromGraph(vis_graph).render(Format.SVG).toFile(new File(path + ".svg"));
			//Graphviz.fromGraph(vis_graph).render(Format.DOT).toFile(new File(path + ".dot"));
		} catch (GraphvizException | IOException ex) {
			logger.critical("Cannot save AST-visualization to file '" + path + ".svg'");
			logger.log(ex.getMessage());
		}
	}

    // Parents created me and gave me my identity
	private void build_visualization(Node current_node, MutableNode current_vis_node) {
		for (Node n: current_node.getChildren()) {
			Node realnode;
			Color node_color;
			try {
				InstrumentableNode.WrapperNode wn = (InstrumentableNode.WrapperNode) n;
				realnode = wn.getDelegateNode();
				node_color = Color.rgb(0x72, 0x9f, 0xcf);
			} catch (ClassCastException ex) {
				realnode = n;
				node_color = Color.rgb(0xfc, 0xaf, 0x3e);
			}

			String node_type = realnode.getClass().getSimpleName();
			String visualization_node_name = String.valueOf(realnode.hashCode());

			MutableNode child = mutNode(visualization_node_name);
			child.add(Shape.BOX, Style.FILLED, node_color);
			child.add(getNodeContents(node_type, realnode.getSourceSection()));
			vis_graph.add(child);
			current_vis_node.addLink(child);
			build_visualization(realnode, child);
		}
	}

	private Label getNodeContents(String node_type, SourceSection node_source) {
		String source = "(NO SOURCE)";
		if (node_source != null && node_source.isAvailable()) {
			String characters = node_source.getCharacters().toString().replaceAll("\\s+", " ");
			if (characters.length() > 16) {
				characters = characters.substring(0, 13) + "...";
			}
			String line_numbering;
			if (node_source.getStartLine() == node_source.getEndLine()) {
				line_numbering = "<b>" + node_source.getStartLine() + ":</b>  ";
			} else {
				line_numbering = "<b>" + node_source.getStartLine() + "-" + node_source.getEndLine() + ":</b>  ";
			}
			source = line_numbering + StringEscapeUtils.escapeHtml4(characters);
		}
		return Label.html("<table><tr><td balign='center'><font face='LiberationMono'><b>" + node_type +
								  "</b></font></td></tr><tr><td balign='left'><font face='LiberationMono'>" + source +
								  "</font></td></tr></table>");
	}
}
