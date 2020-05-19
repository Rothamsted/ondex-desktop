package net.sourceforge.ondex.ovtk2.layout;

import net.sourceforge.ondex.core.Attribute;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.ovtk2.graph.ONDEXJUNGGraph;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import net.sourceforge.ondex.ovtk2.util.AppearanceSynchronizer;
import net.sourceforge.ondex.tools.threading.monitoring.Monitorable;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jungrapht.visualization.layout.algorithms.util.IterativeContext;
import org.jungrapht.visualization.layout.algorithms.util.Pair;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Layout based on the KKLayout from JUNG taking Attribute values into account
 * for edge weights.
 * 
 * @author taubertj
 * @version 18.03.2008
 */
public class AttributeKKLayout extends OVTK2Layouter implements ActionListener,
		ChangeListener, IterativeContext, Monitorable {

	// attribute name of conf value
	private AttributeName an = null;

	// A factor which partly the "preferred" length of an edge.
	private int edge_length = 50;

	// A factor which specifies the inter-vertex distance between disconnected
	// vertices.
	private int disconnected_length = 15;

	// number of maximum iterations
	private int maxIterations = 2000;

	// scale edge length
	private boolean inverseScale = true;

	// exchange vertices
	private boolean exchangeVertices = true;

	// slider for length factor
	private JSlider sliderEdgeLength = null;

	// slider for disconnected multiplier
	private JSlider sliderDisconnectedLength = null;

	// slider for maxIter
	private JSlider sliderMaxIter = null;

	// checkbox for exchange
	private JCheckBox exchangeBox = null;

	// checkbox to normalise length of edges
	private JCheckBox inverseScaleBox = null;

	private double EPSILON = 0.1d;

	private int currentIteration;

	private String status = Monitorable.STATE_IDLE;

	private double K = 1; // arbitrary const number

	private double[][] dm; // distance matrix

	private ONDEXConcept[] vertices;

	private Point[] xydata;

	private boolean cancelled = false;

	// Retrieves graph distances between vertices of the visible graph
	protected Map<Pair<ONDEXConcept>, Integer> distance;

	public String getStatus() {
		return status + this.layoutModel.getWidth();
	}

	/**
	 * This one is an incremental visualization.
	 */
	public boolean isIncremental() {
		return true;
	}

	/**
	 * Returns true once the current iteration has passed the maximum count.
	 */
	public boolean done() {
		if (currentIteration > maxIterations) {
			return true;
		}
		return false;
	}

	public void initialize() {
		currentIteration = 0;

		if (graph != null) {

			int n = graph.vertexSet().size();
			dm = new double[n][n];
			vertices = graph.vertexSet().toArray(new ONDEXConcept[0]);
			xydata = new Point[n];

			// assign IDs to all visible vertices
			while (true) {
				try {
					int index = 0;
					for (ONDEXConcept v : graph.vertexSet()) {
						Point xyd = layoutModel.apply(v);
						vertices[index] = v;
						xydata[index] = xyd;
						index++;
					}
					break;
				} catch (ConcurrentModificationException cme) {
				}
			}

			for (int i = 0; i < n - 1; i++) {
				for (int j = i + 1; j < n; j++) {
					Number d_ij = distance
							.get(Pair.of(vertices[i], vertices[j]));
					Number d_ji = distance
							.get(Pair.of(vertices[j], vertices[i]));
					double dist = disconnected_length;
					if (d_ij != null)
						dist = Math.min(d_ij.doubleValue(), dist);
					if (d_ji != null)
						dist = Math.min(d_ji.doubleValue(), dist);
					dm[i][j] = dm[j][i] = dist;
				}
			}
		}
	}

	public void step() {
		try {
			currentIteration++;
			if (cancelled)
				return;

			double energy = calcEnergy();
			status = "Kamada-Kawai V=" + graph.vertexSet().size() + "("
					+ graph.vertexSet().size() + ")" + " IT: "
					+ currentIteration + " E=" + energy;

			int n = graph.vertexSet().size();
			if (n == 0)
				return;

			double maxDeltaM = 0;
			int pm = -1; // the node having max deltaM
			for (int i = 0; i < n; i++) {
				if (layoutModel.isLocked(vertices[i]))
					continue;
				double deltam = calcDeltaM(i);

				if (maxDeltaM < deltam) {
					maxDeltaM = deltam;
					pm = i;
				}
			}
			if (pm == -1)
				return;

			for (int i = 0; i < 100; i++) {
				double[] dxy = calcDeltaXY(pm);
				xydata[pm] = Point.of(xydata[pm].x + dxy[0], xydata[pm].y + dxy[1]);
				double deltam = calcDeltaM(pm);
				if (deltam < EPSILON)
					break;
			}

			if (exchangeVertices && maxDeltaM < EPSILON) {
				energy = calcEnergy();
				for (int i = 0; i < n - 1; i++) {
					if (layoutModel.isLocked(vertices[i])) {
						continue;
					}
					for (int j = i + 1; j < n; j++) {
						if (layoutModel.isLocked(vertices[j])) {
							continue;
						}
						double xenergy = calcEnergyIfExchanged(i, j);
						if (energy > xenergy) {
							double sx = xydata[i].x;
							double sy = xydata[i].y;
							xydata[i] = Point.of(xydata[j].x, xydata[j].y);
							xydata[j] = Point.of(sx, sy);
							return;
						}
					}
				}
			}
		} finally {
			// fireStateChanged();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.uci.ics.jung.visualization.layout.AbstractLayout#setSize(java.awt
	 * .Dimension)
	 */

//	@Override
//	public void setSize(Dimension size) {
//		layoutModel.setSize(size.width, size.height);
//		layoutModel.setInitializer(
//		new RandomLocationTransformer<>(
//				layoutModel.getWidth(), layoutModel.getHeight(), graph.vertexSet().size()));
//		super.setSize(size);
//	}

	/**
	 * Enable or disable the local minimum escape technique by exchanging
	 * vertices.
	 */
	public void setExchangeVertices(boolean on) {
		exchangeVertices = on;
	}

	/**
	 * Returns true if the local minimum escape technique by exchanging vertices
	 * is enabled.
	 */
	public boolean getExchangeVertices() {
		return exchangeVertices;
	}

	/**
	 * Determines a step to new position of the vertex m.
	 */
	private double[] calcDeltaXY(int m) {
		double dE_dxm = 0;
		double dE_dym = 0;
		double d2E_d2xm = 0;
		double d2E_dxmdym = 0;
		double d2E_dymdxm = 0;
		double d2E_d2ym = 0;

		for (int i = 0; i < vertices.length; i++) {
			if (i != m) {

				double dist = dm[m][i];
				double l_mi = edge_length * dist;
				double k_mi = K / (dist * dist);
				double dx = xydata[m].x - xydata[i].x;
				double dy = xydata[m].y - xydata[i].y;
				double d = Math.sqrt(dx * dx + dy * dy);
				double ddd = d * d * d;

				dE_dxm += k_mi * (1 - l_mi / d) * dx;
				dE_dym += k_mi * (1 - l_mi / d) * dy;
				d2E_d2xm += k_mi * (1 - l_mi * dy * dy / ddd);
				d2E_dxmdym += k_mi * l_mi * dx * dy / ddd;
				d2E_d2ym += k_mi * (1 - l_mi * dx * dx / ddd);
			}
		}
		// d2E_dymdxm equals to d2E_dxmdym.
		d2E_dymdxm = d2E_dxmdym;

		double denomi = d2E_d2xm * d2E_d2ym - d2E_dxmdym * d2E_dymdxm;
		double deltaX = (d2E_dxmdym * dE_dym - d2E_d2ym * dE_dxm) / denomi;
		double deltaY = (d2E_dymdxm * dE_dxm - d2E_d2xm * dE_dym) / denomi;
		return new double[] { deltaX, deltaY };
	}

	/**
	 * Calculates the gradient of energy function at the vertex m.
	 */
	private double calcDeltaM(int m) {
		double dEdxm = 0;
		double dEdym = 0;
		for (int i = 0; i < vertices.length; i++) {
			if (i != m) {
				double dist = dm[m][i];
				double l_mi = edge_length * dist;
				double k_mi = K / (dist * dist);

				double dx = xydata[m].x - xydata[i].x;
				double dy = xydata[m].y - xydata[i].y;
				double d = Math.sqrt(dx * dx + dy * dy);

				double common = k_mi * (1 - l_mi / d);
				dEdxm += common * dx;
				dEdym += common * dy;
			}
		}
		return Math.sqrt(dEdxm * dEdxm + dEdym * dEdym);
	}

	/**
	 * Calculates the energy function E.
	 */
	private double calcEnergy() {
		double energy = 0;
		for (int i = 0; i < vertices.length - 1; i++) {
			for (int j = i + 1; j < vertices.length; j++) {
				double dist = dm[i][j];
				double l_ij = edge_length * dist;
				double k_ij = K / (dist * dist);
				double dx = xydata[i].x - xydata[j].x;
				double dy = xydata[i].y - xydata[j].y;
				double d = Math.sqrt(dx * dx + dy * dy);

				energy += k_ij / 2
						* (dx * dx + dy * dy + l_ij * l_ij - 2 * l_ij * d);
			}
		}
		return energy;
	}

	/**
	 * Calculates the energy function E as if positions of the specified
	 * vertices are exchanged.
	 */
	private double calcEnergyIfExchanged(int p, int q) {
		if (p >= q)
			throw new RuntimeException("p should be < q");
		double energy = 0; // < 0
		for (int i = 0; i < vertices.length - 1; i++) {
			for (int j = i + 1; j < vertices.length; j++) {
				int ii = i;
				int jj = j;
				if (i == p)
					ii = q;
				if (j == q)
					jj = p;

				double dist = dm[i][j];
				double l_ij = edge_length * dist;
				double k_ij = K / (dist * dist);
				double dx = xydata[ii].x - xydata[jj].x;
				double dy = xydata[ii].y - xydata[jj].y;
				double d = Math.sqrt(dx * dx + dy * dy);

				energy += k_ij / 2
						* (dx * dx + dy * dy + l_ij * l_ij - 2 * l_ij * d);
			}
		}
		return energy;
	}

	public void reset() {
		cancelled = false;
		status = Monitorable.STATE_IDLE;
		currentIteration = 0;
	}

	/**
	 * Initialises unit distance measure.
	 * 
	 * @param viewer
	 *            OVTK2PropertiesAggregator
	 */
	public AttributeKKLayout(OVTK2PropertiesAggregator viewer) {
		super(viewer);
	}

	@Override
	public void visit(LayoutModel<ONDEXConcept> layoutModel) {
		super.visit(layoutModel);
		if (graph != null) {
			this.distance = getDistances(graph);
		}
		initialize();
	}

	private Map<Pair<ONDEXConcept>, Integer> getDistances(Graph<ONDEXConcept, ?> graph) {

		DijkstraShortestPath<ONDEXConcept, ?> dijkstra = new DijkstraShortestPath<>(graph);
		Map<Pair<ONDEXConcept>, Integer> distanceMap = new HashMap<>();
		for (ONDEXConcept vertex : graph.vertexSet()) {

			ShortestPathAlgorithm.SingleSourcePaths<ONDEXConcept, ?> distances = dijkstra.getPaths(vertex);
			for (ONDEXConcept n : graph.vertexSet()) {
				GraphPath<ONDEXConcept, ?> graphPath = distances.getPath(n);
				if (graphPath != null && graphPath.getWeight() != 0) {
					distanceMap.put(Pair.of(vertex, n), (int) graphPath.getWeight());
				}
			}
		}
		return distanceMap;
	}

	@Override
	public JPanel getOptionPanel() {

		ONDEXJUNGGraph aog = (ONDEXJUNGGraph) graph;

		// new option panel
		JPanel panel = new JPanel();
		BoxLayout layout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(layout);

		// combobox for attributenames
		JComboBox box = new JComboBox();
		box.addActionListener(this);
		box.addItem("None");
		box.setSelectedIndex(0);
		for (AttributeName an : aog.getMetaData().getAttributeNames()) {
			Class<?> cl = an.getDataType();
			if (cl != null && Number.class.isAssignableFrom(cl)) {
				Set<ONDEXRelation> relations = aog
						.getRelationsOfAttributeName(an);
				if (relations.size() > 0
						&& !AppearanceSynchronizer.attr.contains(an.getId()))
					box.addItem(an.getId());
			}
		}

		panel.add(new JLabel("Select AttributeName:"));
		panel.add(box);

		inverseScaleBox = new JCheckBox("inverse scaling");
		inverseScaleBox.setSelected(inverseScale);
		inverseScaleBox.addChangeListener(this);
		panel.add(inverseScaleBox);

		sliderMaxIter = new JSlider();
		sliderMaxIter.setBorder(BorderFactory
				.createTitledBorder("max iterations"));
		sliderMaxIter.setMinimum(0);
		sliderMaxIter.setMaximum(5000);
		sliderMaxIter.setValue(maxIterations);
		sliderMaxIter.setMajorTickSpacing(1000);
		sliderMaxIter.setMinorTickSpacing(100);
		sliderMaxIter.setPaintTicks(true);
		sliderMaxIter.setPaintLabels(true);
		sliderMaxIter.addChangeListener(this);
		panel.add(sliderMaxIter);

		sliderEdgeLength = new JSlider();
		sliderEdgeLength.setBorder(BorderFactory
				.createTitledBorder("Preferred Edge Length"));
		sliderEdgeLength.setMinimum(0);
		sliderEdgeLength.setMaximum(100);
		sliderEdgeLength.setValue(edge_length);
		sliderEdgeLength.setMajorTickSpacing(20);
		sliderEdgeLength.setMinorTickSpacing(5);
		sliderEdgeLength.setPaintTicks(true);
		sliderEdgeLength.setPaintLabels(true);
		sliderEdgeLength.addChangeListener(this);
		panel.add(sliderEdgeLength);

		sliderDisconnectedLength = new JSlider();
		sliderDisconnectedLength.setBorder(BorderFactory
				.createTitledBorder("Disconnected Distance"));
		sliderDisconnectedLength.setMinimum(0);
		sliderDisconnectedLength.setMaximum(40);
		sliderDisconnectedLength.setValue(disconnected_length);
		sliderDisconnectedLength.setMajorTickSpacing(10);
		sliderDisconnectedLength.setMinorTickSpacing(2);
		sliderDisconnectedLength.setPaintTicks(true);
		sliderDisconnectedLength.setPaintLabels(true);
		sliderDisconnectedLength.addChangeListener(this);
		panel.add(sliderDisconnectedLength);

		exchangeBox = new JCheckBox("exchange vertices");
		exchangeBox.setSelected(exchangeVertices);
		exchangeBox.addChangeListener(this);
		panel.add(exchangeBox);

		return panel;
	}

	/**
	 * Check for selection of an AttributeName.
	 */
	public void actionPerformed(ActionEvent arg0) {
		ONDEXJUNGGraph aog = (ONDEXJUNGGraph) graph;
		JComboBox box = (JComboBox) arg0.getSource();
		String name = (String) box.getSelectedItem();
		an = aog.getMetaData().getAttributeName(name);
		this.distance = getDistances(graph);
		this.reset();
//		if (an == null) {
//			this.distance = new UnweightedShortestPath<ONDEXConcept, ONDEXRelation>(
//					graph);
//			this.reset();
//		} else {
//			this.distance = new DijkstraDistance<ONDEXConcept, ONDEXRelation>(
//					graph, new GDSEdges(an, inverseScale), true);
//			this.reset();
//		}
	}

	/**
	 * Class for wrapping edge weights returned from Attribute.
	 * 
	 * @author taubertj
	 * @version 12.03.2008
	 */
	private class GDSEdges implements Function<ONDEXRelation, Number> {

		// cache for edge weights
		Map<ONDEXRelation, Number> cache;

		// minimum and maximum for scaling
		double minimum = Double.POSITIVE_INFINITY;

		double maximum = Double.NEGATIVE_INFINITY;

		/**
		 * Extract edge weights from Attribute for a given AttributeName.
		 * 
		 * @param an
		 *            AttributeName
		 * @param inverse
		 *            boolean
		 */
		public GDSEdges(AttributeName an, boolean inverse) {
			ONDEXJUNGGraph aog = (ONDEXJUNGGraph) graph;
			cache = new Hashtable<ONDEXRelation, Number>();

			// calculate normalisation per rt
			Map<RelationType, Map<ONDEXRelation, Number>> temp = new Hashtable<RelationType, Map<ONDEXRelation, Number>>();

			// iterate over all relations that have this Attribute
			for (ONDEXRelation r : aog.getRelationsOfAttributeName(an)) {
				RelationType rt = r.getOfType();
				Attribute attribute = r.getAttribute(an);
				// add cast to Number to cache
				if (!temp.containsKey(rt))
					temp.put(rt, new Hashtable<ONDEXRelation, Number>());
				temp.get(rt).put(r, (Number) attribute.getValue());
			}

			// process every rt
			for (RelationType rt : temp.keySet()) {

				// get minimum and maximum
				for (ONDEXRelation key : temp.get(rt).keySet()) {
					double value = temp.get(rt).get(key).doubleValue();
					if (value < minimum)
						minimum = value;
					if (value > maximum)
						maximum = value;
				}

				// scale to [0,1]
				double diff = maximum - minimum;
				double value, newvalue;
				if (diff != 0) {
					for (ONDEXRelation key : temp.get(rt).keySet()) {
						value = temp.get(rt).get(key).doubleValue();
						// normalise length of edge
						if (inverse)
							newvalue = 2 - ((value - minimum) / diff);
						else
							newvalue = 1 + ((value - minimum) / diff);
						// System.out.println(rtset.getId()+": "+value+" ->
						// "+newvalue);
						cache.put(key, Double.valueOf(newvalue));
					}
				}
			}
		}

		/**
		 * Return transformation lookup from cache.
		 * 
		 * @param input
		 *            ONDEXEdge
		 * @return Number
		 */
		public Number apply(ONDEXRelation input) {
			Number number = cache.get(input);
			if (number == null)
				return 1;
			return number;
		}
	}

	/**
	 * Performs updates of layout parameters.
	 */
	public void stateChanged(ChangeEvent arg0) {
		if (arg0.getSource().equals(sliderEdgeLength)) {
			edge_length = sliderEdgeLength.getValue();
			currentIteration = 0;
			cancelled = false;
			status = Monitorable.STATE_IDLE;
		} else if (arg0.getSource().equals(sliderDisconnectedLength)) {
			disconnected_length = sliderDisconnectedLength.getValue();
			currentIteration = 0;
			cancelled = false;
			status = Monitorable.STATE_IDLE;
		} else if (arg0.getSource().equals(sliderMaxIter)) {
			maxIterations = sliderMaxIter.getValue();
			currentIteration = 0;
			cancelled = false;
			status = Monitorable.STATE_IDLE;
		} else if (arg0.getSource().equals(exchangeBox)) {
			exchangeVertices = exchangeBox.isSelected();
			currentIteration = 0;
			cancelled = false;
			status = Monitorable.STATE_IDLE;
		} else if (arg0.getSource().equals(inverseScaleBox)) {
			inverseScale = inverseScaleBox.isSelected();
			currentIteration = 0;
			cancelled = false;
			status = Monitorable.STATE_IDLE;
		}
	}

	@Override
	public int getProgress() {
		return currentIteration;
	}

	@Override
	public String getState() {
		return status;
	}

	@Override
	public int getMaxProgress() {
		return maxIterations;
	}

	@Override
	public int getMinProgress() {
		return 0;
	}

	@Override
	public void setCancelled(boolean c) {
		cancelled = c;
		status = Monitorable.STATE_TERMINAL;
	}

	@Override
	public boolean isIndeterminate() {
		return false;
	}

	@Override
	public boolean isAbortable() {
		return true;
	}

	@Override
	public Throwable getUncaughtException() {
		return null;
	}

}
