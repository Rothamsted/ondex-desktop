package net.sourceforge.ondex.ovtk2.layout;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

/**
 * Vertically flips the selected nodes.
 * 
 * @author Jochen Weile, B.Sc.
 * 
 */
public class FlipLayout extends OVTK2Layouter {

	// ####FIELDS####

	/**
	 * debug flag.
	 */
	private static final boolean DEBUG = false;

	/**
	 * old layouter.
	 */
//	private LayoutModel<ONDEXConcept> oldLayouter;

	private OVTK2PropertiesAggregator aViewer;

	// ####CONSTRUCTOR####

	/**
	 * constructor
	 */
	public FlipLayout(OVTK2PropertiesAggregator viewer) {
		super(viewer);
		aViewer = viewer;
//		oldLayouter = viewer.getVisualizationViewer().getVisualizationModel().getLayoutModel();
//		graph = oldLayouter.getGraph();
	}

	@Override
	public void visit(LayoutModel<ONDEXConcept> layoutModel) {
		this.layoutModel = layoutModel;
		this.graph = layoutModel.getGraph();
		initialize();
	}

	// ####METHODS####

	/**
	 * gives the options panel.
	 */
	@Override
	public JPanel getOptionPanel() {
		JPanel panel = new JPanel();
		panel.add(new JLabel("No options available."));
		return panel;
	}

	/**
	 * runs layouter
	 * 
//	 * @see edu.uci.ics.jung.algorithms.layout.Layout#initialize()
	 */
//	@Override
	public void initialize() {
		Collection<ONDEXConcept> nodes = aViewer.getSelectedNodes();
		if (nodes == null || nodes.size() == 0)
			nodes = graph.vertexSet();
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		for (ONDEXConcept node : nodes) {
			Point coord = layoutModel.apply(node);
			if (coord.y < min)
				min = coord.y;
			if (coord.y > max)
				max = coord.y;
		}
		if (DEBUG)
			System.out.println("min = " + min + "\tmax = " + max);

		if (nodes.size() < graph.vertexSet().size()) {
			Collection<ONDEXConcept> rest = new HashSet<ONDEXConcept>();
			rest.addAll(graph.vertexSet());
			rest.removeAll(nodes);
			for (ONDEXConcept node : rest) {
				Point coord = layoutModel.apply(node);
				double y = coord.y;
				double x = coord.x;
//				Point coordNew = apply(node);
				layoutModel.set(node, x, y);
			}
		}

		for (ONDEXConcept node : nodes) {
			Point coord = layoutModel.apply(node);
			double y = coord.y;
			double x = coord.x;
			if (DEBUG)
				System.out.println("x = " + x + "\ty = " + y);
			y = max - (y - min);
//			Point2D coordNew = transform(node);
//			coordNew.setLocation(x, y);
			layoutModel.set(node, x, y);
		}
	}

	/**
	 * reruns layouter
	 * 
//	 * @see edu.uci.ics.jung.algorithms.layout.Layout#reset()
	 */
//	@Override
	public void reset() {
		initialize();
	}
	
//	@Override
//	public void cleanUp(){
//		oldLayouter = null;
//	}

}
