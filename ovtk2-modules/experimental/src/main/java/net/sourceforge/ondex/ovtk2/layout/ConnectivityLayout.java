package net.sourceforge.ondex.ovtk2.layout;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import org.jgrapht.Graphs;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static java.lang.Math.PI;

public class ConnectivityLayout extends OVTK2Layouter {
	// #####FIELDS####
	/**
	 * old layouter.
	 */
	private LayoutAlgorithm<ONDEXConcept> oldLayouter;

	private double smallSeparator = 10.0;

	private double bigSeparator = 100.0;

	private int rows = 3;

	private JSpinner rowSpinner;

	private OVTK2PropertiesAggregator aViewer;

	// #####CONSTRUCTOR#####

	public ConnectivityLayout(OVTK2PropertiesAggregator viewer) {
		super(viewer);
		aViewer = viewer;
		oldLayouter = viewer.getVisualizationViewer().getVisualizationModel().getLayoutAlgorithm();
	}

	// #####METHODS#####

	@Override
	public JPanel getOptionPanel() {
		rowSpinner = new JSpinner();
		rowSpinner.setValue(Integer.valueOf(1));

		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.LINE_AXIS));
		rowPanel.add(rowSpinner);
		rowPanel.add(new JLabel("Rows"));

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.add(rowPanel);
		p.add(new JPanel());
		return p;
	}

//	@Override
	public void initialize() {
		if (rowSpinner != null) {
			rows = (Integer) rowSpinner.getValue();
		}

		Collection<ONDEXConcept> selected = aViewer.getSelectedNodes();
		if (selected == null || selected.size() == 0) {
			JOptionPane.showMessageDialog(viewer.getComponent(),
					"You need to select some nodes first!",
					"Layout requirement", JOptionPane.INFORMATION_MESSAGE);
//			copyOldLayout();
			return;
		}

		Set<ONDEXConcept> hits = new HashSet<ONDEXConcept>();
		Collection<LinkedList<ONDEXConcept>> rowList = search(selected);
		int row_curr = 0, col_curr = 0, max_rowLength = 0;
		for (LinkedList<ONDEXConcept> row : rowList) {
			col_curr = 0;
			for (ONDEXConcept c : row) {
				hits.add(c);
				layoutModel.set(c,((double) col_curr) * smallSeparator,
						((double) row_curr) * smallSeparator);
				col_curr++;
			}
			if (row.size() > max_rowLength) {
				max_rowLength = row.size();
			}
			row_curr++;
		}

		double selectionRadius = radius(selected.size());
		double y_center = ((double) (rowList.size() / 2)) * smallSeparator;
		Point2D selectionCenter = new Point2D.Double(((double) max_rowLength)
				* smallSeparator + bigSeparator + selectionRadius, y_center);
		makeCircle(selectionCenter, selected);

		// what is left over
		Collection<ONDEXConcept> restView = new HashSet<ONDEXConcept>(
				graph.vertexSet());
		restView.removeAll(selected);
		restView.removeAll(hits);

		double restRadius = radius(restView.size());
		Point2D restCenter = new Point2D.Double(selectionCenter.getX()
				+ selectionRadius + bigSeparator + restRadius, y_center);
		makeCircle(restCenter, restView);
	}

//	private void copyOldLayout() {
//		for (ONDEXConcept node : graph.vertexSet()) {
//			Point2D coord = oldLayouter.apply(node);
//			Point2D coordNew = apply(node);
//			coordNew.setLocation(coord.getX(), coord.getY());
//		}
//	}

	private Collection<LinkedList<ONDEXConcept>> search(
			Collection<ONDEXConcept> selected) {
		Map<Integer, LinkedList<ONDEXConcept>> result = new HashMap<Integer, LinkedList<ONDEXConcept>>();
		int maxCount = 0;
		for (ONDEXConcept c : graph.vertexSet()) {
			if (selected.contains(c)) {
				continue;
			}
			int count = 0;
			for (ONDEXConcept neighbour : Graphs.neighborListOf(graph, c)) {
				if (selected.contains(neighbour)) {
					count++;
				}
			}
			if (count > maxCount)
				maxCount = count;
			LinkedList<ONDEXConcept> l = result.get(count);
			if (l == null) {
				l = new LinkedList<ONDEXConcept>();
				result.put(count, l);
			}
			l.add(c);
		}

		Vector<LinkedList<ONDEXConcept>> rowlist = new Vector<LinkedList<ONDEXConcept>>();
		int rowcount = 0;
		for (int i = maxCount; i >= 0; i--) {
			LinkedList<ONDEXConcept> row_curr = result.get(i);
			if (row_curr != null) {
				rowcount++;
				rowlist.add(row_curr);
			}
			if (rowcount >= rows) {
				break;
			}
		}
		return rowlist;
	}

	private double radius(int count) {
		return smallSeparator * ((double) count) / (2.0 * PI);
	}

	private void makeCircle(Point2D center, Collection<ONDEXConcept> concepts) {

		double i = 0.0;
		double r = radius(concepts.size());
		double b = (2.0 * PI) / ((double) concepts.size());
		for (ONDEXConcept concept : concepts) {
			double phi = b * i;
			double x = (r * Math.cos(phi)) + center.getX();
			double y = (r * Math.sin(phi)) + center.getY();
			i++;

			// set new coordinates
//			Point coord = layoutModel.apply(concept);
//			coord.setLocation(x, y);
			layoutModel.set(concept, x, y);
		}
	}

//	@Override
	public void reset() {
		initialize();
	}
	
	@Override 
	public void cleanUp(){
		oldLayouter = null;
	}

}
