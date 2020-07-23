package org.fuzzingtool.core.visualization;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizException;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.fuzzingtool.core.Logger;
import org.fuzzingtool.core.components.BranchingNode;
import org.fuzzingtool.core.components.BranchingNodeAttribute;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

public class BranchingVisualizer {
	private final MutableGraph vis_graph = mutGraph("Program Decision Flow Visualization").setDirected(true);

	private final Logger logger;

	public BranchingVisualizer(BranchingNode node, Logger l) {
		this.logger = l;
		vis_graph.nodeAttrs().add(Font.name("Ubuntu"));
		String visualization_node_name = String.valueOf(node.hashCode());
		MutableNode root = mutNode(visualization_node_name).add(getNodeContents(node));
		setNodeAttributes(root, node.getBranchingNodeAttribute());
		vis_graph.add(root);
		build_visualization(node, root);
	}

	public void save_image(String path) {
		Graphviz.useEngine(new GraphvizCmdLineEngine());
		try {
			logger.info("Saving branching-visualization to file '" + path + ".svg'");
			Graphviz.fromGraph(vis_graph).render(Format.SVG).toFile(new File(path + ".svg"));
			//Graphviz.fromGraph(vis_graph).render(Format.DOT).toFile(new File(path + ".dot"));
		} catch (GraphvizException | IOException ex) {
			logger.critical("Cannot save branching-visualization to file '" + path + ".svg'");
			logger.log(ex.getMessage());
		}
	}

    // Parents created me and gave me my identity
	private void build_visualization(BranchingNode current_node, MutableNode current_vis_node) {
		for (Boolean taken_flag: new boolean[]{true, false}) {
			BranchingNode child_node = current_node.getChildBranch(taken_flag);
			if (child_node != null) {
				MutableNode child = mutNode(String.valueOf(child_node.hashCode()));
				child.add(getNodeContents(child_node));
				setNodeAttributes(child, child_node.getBranchingNodeAttribute());
				vis_graph.add(child);
				if (taken_flag) {
					current_vis_node.addLink(Link.to(child).with(Color.rgb(0x4e, 0x9a, 0x06), Label.of("⊤")));
				} else {
					current_vis_node.addLink(Link.to(child).with(Color.rgb(0xa4, 0x00, 0x00), Label.of("⊥")));
				}
				build_visualization(child_node, child);
			}
		}
	}

	private Label getNodeContents(BranchingNode node) {
		String label_string;
		switch (node.getBranchingNodeAttribute()) {
			case BRANCH:
				label_string = "BRANCH: " + node.getSourceCodeExpression();
				break;
			case LOOP:
				label_string = "LOOP: " + node.getSourceCodeExpression();
				break;
			case UNKNOWN:
				label_string = "UNKNOWN";
				break;
			case UNREACHABLE:
				label_string = "UNREACHABLE";
				break;
			case TERMINATE:
				label_string = "TERMINATE";
				break;
			case ERROR:
				label_string = "ERROR";
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + node.getBranchingNodeAttribute());
		}
		if (node.isUndecidable()) {
			label_string += " ↯";
		}
		if (node.isExplored()) {
			label_string += " ↺";
		}
		return Label.of(label_string);
	}

	private void setNodeAttributes(MutableNode mut_node, BranchingNodeAttribute node_type) {
		switch (node_type) {
			case BRANCH:
            case LOOP:
                mut_node.add(Shape.DIAMOND, Style.FILLED, Color.rgb(0xee, 0xee, 0xec),
							 Color.rgb(0x1c, 0x1e, 0x1e).font());
				break;
            case UNKNOWN:
				mut_node.add(Shape.ELLIPSE, Style.FILLED, Color.rgb(0xad, 0x7f, 0xa8),
							 Color.rgb(0x1c, 0x1e, 0x1e).font());
				break;
			case UNREACHABLE:
				mut_node.add(Shape.ELLIPSE, Style.FILLED, Color.rgb(0x72, 0x9f, 0xcf),
							 Color.rgb(0x1c, 0x1e, 0x1e).font());
				break;
			case TERMINATE:
				mut_node.add(Shape.ELLIPSE, Style.FILLED, Color.rgb(0xba, 0xbd, 0xb6),
							 Color.rgb(0x1c, 0x1e, 0x1e).font());
				break;
			case ERROR:
				mut_node.add(Shape.ELLIPSE, Style.FILLED, Color.rgb(0xcc, 0x00, 0x00),
							 Color.rgb(0xfa, 0xfa, 0xfa).font());
				break;
		}
	}
}
