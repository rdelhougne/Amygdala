package org.fuzzingtool.visualization;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.fuzzingtool.components.BranchingNode;
import org.fuzzingtool.symbolic.SymbolicException;

import java.io.File;
import java.io.IOException;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

public class BranchingVisualizer {
    private MutableGraph vis_graph = mutGraph("Program Decision Flow Visualization").setDirected(true);

    public BranchingVisualizer(BranchingNode node) {
        String visualization_node_name = String.valueOf(node.hashCode());
        MutableNode root = mutNode(visualization_node_name).add(getNodeContents(node));
        vis_graph.add(root);
        build_visualization(node, root);
    }

    public void save_image(String path) {
        Graphviz.useEngine(new GraphvizCmdLineEngine());
        try {
            Graphviz.fromGraph(vis_graph).render(Format.SVG).toFile(new File(path + ".svg"));
            //Graphviz.fromGraph(vis_graph).render(Format.DOT).toFile(new File(path + ".dot"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void build_visualization(BranchingNode current_node, MutableNode current_vis_node) { // Parents created me and gave me my identity
        for (Boolean taken_flag: new boolean[]{true, false}) {
            BranchingNode child_node = current_node.getChildBranch(taken_flag);
            if (child_node != null) {
                MutableNode child = mutNode(String.valueOf(child_node.hashCode()));
                child.add(getNodeContents(child_node));
                vis_graph.add(child);
                current_vis_node.addLink(child);
                build_visualization(child_node, child);
            }
        }
    }

    private Label getNodeContents(BranchingNode node) {
        try {
            switch (node.getBranchingNodeAttribute()) {
                case BRANCH:
                    return Label.of("BRANCH: " + node.getLocalHRExpression(true));
                case LOOP:
                    return Label.of("LOOP: " + node.getLocalHRExpression(true));
                case UNKNOWN:
                    return Label.of("UNKNOWN");
                case UNREACHABLE:
                    return Label.of("UNREACHABLE");
                case TERMINATE:
                    return Label.of("TERMINATE");
                case ERROR:
                    return Label.of("ERROR");
            }
        } catch (SymbolicException.IncompatibleType | SymbolicException.WrongParameterSize incompatibleType) {
            incompatibleType.printStackTrace();
        }
        return Label.of("!VISUALIZING ERROR!");
    }
}
