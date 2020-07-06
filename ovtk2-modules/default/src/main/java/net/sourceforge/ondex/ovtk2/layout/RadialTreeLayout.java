package net.sourceforge.ondex.ovtk2.layout;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import net.sourceforge.ondex.tools.threading.monitoring.Monitorable;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.map.LazyMap;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.model.PolarPoint;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A radial layout for Tree or Forest graphs. Modified to perform pseudo tree
 * layout on arbitrary graphs.
 * 
 * @author Tom Nelson, hacked by taubertj
 * 
 */
public class RadialTreeLayout extends OVTK2Layouter implements Monitorable {

	// keep track of progress
	private final Set<ONDEXConcept> allreadyDone = new HashSet<>();

	// x positions of nodes
	private final Map<ONDEXConcept, Integer> basePositions = new HashMap<>();

	// use default
	private int distX = 10;

	// use default
	private int distY = 10;

	// final locations after layout
	private final Map<ONDEXConcept, Point> locations = new HashMap<>();

	// processing position of current node
	private Point m_currentPoint = Point.ORIGIN;

	// coordinates in polar system
	private final Map<ONDEXConcept, PolarPoint> polarLocations = new HashMap<>();

	/**
	 * build tree for reversed edge direction
	 */
	private boolean reversed = false;

	// collection of nodes representing roots of trees
	private Collection<ONDEXConcept> roots;

	/**
	 * Current progress made for Monitorable
	 */
	private int progress = 0;

	/**
	 * Current state for Monitorable
	 */
	private String state = Monitorable.STATE_IDLE;

	/**
	 * If the process gets cancelled
	 */
	private boolean cancelled = false;

	/**
	 * Sets internal variables and creates roots of trees.
	 * 
	 * @param viewer
	 *            OVTK2PropertiesAggregator to get ONDEXJUNGGraph from
	 */
	public RadialTreeLayout(OVTK2PropertiesAggregator viewer) {
		super(viewer);
	}

	/**
	 * Build trees starting at each root.
	 * 
	 */
	void buildTree() {

		this.m_currentPoint = Point.of(0, 20);
		if (roots.size() > 0 && graph != null) {
			calculateDimensionX(roots);
			for (ONDEXConcept v : roots) {
				state = "building tree " + progress;
				calculateDimensionX(v);
				m_currentPoint.add(this.basePositions.get(v) / 2 + 50, 0);
				buildTree(v, (int)this.m_currentPoint.x);
				progress++;
				if (cancelled)
					return;
			}
		}

		setRadialLocations();
	}

	/**
	 * Build tree for one root node and parental X position.
	 * 
	 * @param v
	 *            ONDEXConcept
	 * @param x
	 *            parental X position
	 */
	void buildTree(ONDEXConcept v, int x) {

		// check for potential loops
		if (!allreadyDone.contains(v)) {
			allreadyDone.add(v);

			// go one level further down
//			this.m_currentPoint.y += this.distY;
//			this.m_currentPoint.x = x;
			this.m_currentPoint.add(x, this.distY);

			this.setCurrentPositionFor(v);

			int sizeXofCurrent = basePositions.get(v);

			int lastX = x - sizeXofCurrent / 2;

			int sizeXofChild;
			int startXofChild;

			// check selection of edge direction
			Collection<ONDEXConcept> children = null;
			if (reversed)
				children = Graphs.predecessorListOf(graph, v);
			else
				children = Graphs.successorListOf(graph, v);

			// get new x position from children
			for (ONDEXConcept element : children) {
				sizeXofChild = this.basePositions.get(element);
				startXofChild = lastX + sizeXofChild / 2;

				// recursion over all children
				buildTree(element, startXofChild);
				lastX = lastX + sizeXofChild + distX;
			}

			// simply align them in y dimension, this is edge direction
			// dependent, otherwise it gets upside down
//			this.m_currentPoint.y -= this.distY;
			this.m_currentPoint.add(0, -this.distY);
		}
	}

	/**
	 * Calculate the global size of trees for all nodes.
	 * 
	 * @param roots
	 *            list of roots
	 * @return max X for all subtrees
	 */
	private int calculateDimensionX(Collection<ONDEXConcept> roots) {

		int size = 0;
		for (ONDEXConcept v : roots) {

			// check selection of edge direction
			Collection<ONDEXConcept> children = null;
			if (reversed)
				children = Graphs.predecessorListOf(graph, v);
			else
				children = Graphs.successorListOf(graph, v);

			// check dimension for children
			int childrenNum = children.size();
			if (childrenNum != 0) {
				for (ONDEXConcept element : children) {
					// recursion step
					size += calculateDimensionX(element) + distX;
				}
			}

			// base step
			size = Math.max(0, size - distX);
			basePositions.put(v, size);
		}

		return size;
	}

	/**
	 * Dimension are calculated directed.
	 * 
	 * @param v
	 *            node to start with
	 * @return max X for subtree starting at v
	 */
	private int calculateDimensionX(ONDEXConcept v) {

		int size = 0;

		// stack for performing a breadth first search
		LinkedList<ONDEXConcept> stack = new LinkedList<ONDEXConcept>();
		stack.add(v);

		Set<ONDEXConcept> seen = new HashSet<ONDEXConcept>();
		while (!stack.isEmpty()) {
			ONDEXConcept current = stack.pop();
			if (!seen.contains(current)) {
				// check selection of edge direction
				if (reversed)
					stack.addAll(Graphs.predecessorListOf(graph, current));
				else
					stack.addAll(Graphs.successorListOf(graph, current));
				// check dimension for children
				size = distX;
				size = Math.max(0, size - distX);
				basePositions.put(current, size);
				seen.add(current);
			}
		}

		return size;
	}

	/**
	 * Returns list of all neighbours of a node.
	 * 
	 * @param p
	 *            ONDEXConcept
	 * @return list of all neighbours
	 */
	public List<ONDEXConcept> getAtomics(ONDEXConcept p) {
		List<ONDEXConcept> v = new ArrayList<ONDEXConcept>();
		getAtomics(p, v);
		return v;
	}

	/**
	 * Recursive method for depth first search of neighbours.
	 * 
	 * @param p
	 *            ONDEXConcept
	 * @param v
	 *            already found nodes
	 */
	private void getAtomics(ONDEXConcept p, List<ONDEXConcept> v) {

		// check selection of edge direction
		Collection<ONDEXConcept> parents = null;
		if (reversed)
			parents = Graphs.predecessorListOf(graph, p);
		else
			parents = Graphs.successorListOf(graph, p);
		for (ONDEXConcept c : parents) {

			// check selection of edge direction
			Collection<ONDEXConcept> children = null;
			if (reversed)
				children = Graphs.predecessorListOf(graph, c);
			else
				children = Graphs.successorListOf(graph, c);

			if (children.size() == 0) {
				v.add(c);
			} else {
				getAtomics(c, v);
			}
		}
	}

	/**
	 * Maximal depth of subtree starting at node.
	 * 
	 * @param v
	 *            node to start at
	 * @return depth of subtree
	 */
	public int getDepth(ONDEXConcept v) {
		int depth = 0;

		// check selection of edge direction
		Collection<ONDEXConcept> parents = null;
		if (reversed)
			parents = Graphs.predecessorListOf(graph, v);
		else
			parents = Graphs.successorListOf(graph, v);
		for (ONDEXConcept c : parents) {

			// check selection of edge direction
			Collection<ONDEXConcept> children = null;
			if (reversed)
				children = Graphs.predecessorListOf(graph, c);
			else
				children = Graphs.successorListOf(graph, c);

			// base: at leaf node, no more children
			if (children.size() == 0) {
				depth = 0;
			} else {
				// recursion get maximum path depth
				depth = Math.max(depth, getDepth(c));
			}
		}

		return depth + 1;
	}

	/**
	 * Get maximum of all locations of nodes.
	 * 
	 * @return maximum position
	 */
	private Point2D getMaxXY() {
		double maxx = 0;
		double maxy = 0;
		for (Point p : locations.values()) {
			maxx = Math.max(maxx, p.x);
			maxy = Math.max(maxy, p.y);
		}
		return new Point2D.Double(maxx, maxy);
	}

	@Override
	public JPanel getOptionPanel() {

		// new option panel and layout
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		// check box for reversed edge direction
		JCheckBox checkBox = new JCheckBox("reversed edge direction?");
		checkBox.setSelected(reversed);
		checkBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				reversed = ((JCheckBox) e.getSource()).isSelected();
			}
		});

		// text field for X distance
		JTextField distanceX = new JTextField(String.valueOf(distX));
		distanceX.addCaretListener(new CaretListener() {

			@Override
			public void caretUpdate(CaretEvent e) {
				String text = ((JTextField) e.getSource()).getText();
				try {
					distX = Integer.parseInt(text);
				} catch (NumberFormatException nfe) {
					// result default
					distX = 50;
				}
			}
		});
		JPanel panelX = new JPanel(new BorderLayout());
		panelX.add(BorderLayout.WEST, new JLabel("Distance X: "));
		panelX.add(BorderLayout.CENTER, distanceX);

		// text field for Y distance
		JTextField distanceY = new JTextField(String.valueOf(distY));
		distanceY.addCaretListener(new CaretListener() {

			@Override
			public void caretUpdate(CaretEvent e) {
				String text = ((JTextField) e.getSource()).getText();
				try {
					distY = Integer.parseInt(text);
				} catch (NumberFormatException nfe) {
					// result default
					distY = 50;
				}
			}
		});
		JPanel panelY = new JPanel(new BorderLayout());
		panelY.add(BorderLayout.WEST, new JLabel("Distance Y: "));
		panelY.add(BorderLayout.CENTER, distanceY);

		// horizontal arrangement
		layout.setHorizontalGroup(layout
				.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(checkBox).addComponent(panelX)
				.addComponent(panelY));

		// vertical arrangement
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(checkBox).addComponent(panelX)
				.addComponent(panelY));

		// for spacing issues
		JPanel result = new JPanel();
		result.add(panel);
		return result;
	}

	/**
	 * Returns map of polar coordinates.
	 * 
	 * @return polar coordinates
	 */
	public Map<ONDEXConcept, PolarPoint> getPolarLocations() {
		return polarLocations;
	}

	/**
	 * Returns list of nodes that are roots of trees in the graph.
	 * 
	 * @param g
	 *            ONDEXJUNGGraph
	 * @return list of root nodes
	 */
	private Collection<ONDEXConcept> getRoots(
			Graph<ONDEXConcept, ONDEXRelation> g) {
		Set<ONDEXConcept> roots = new HashSet<ONDEXConcept>();
		for (ONDEXConcept n : g.vertexSet()) {
			// check selection of edge direction
			if (reversed && g.outDegreeOf(n) == 0) {
				roots.add(n);
			}
			if (!reversed && g.inDegreeOf(n) == 0) {
				roots.add(n);
			}
		}
		return roots;
	}

	/**
	 * This layout is none incremental.
	 * 
	 *
	 */
	public boolean incrementsAreDone() {
		return true;
	}

//	@Override
	public void initialize() {

		cancelled = false;
		progress = 0;
		state = Monitorable.STATE_IDLE;

		// clear potential results from previous runs
		allreadyDone.clear();
		basePositions.clear();
		distX = distY = 10;
		locations.clear();
		polarLocations.clear();

		// get roots in graph
		roots = getRoots(graph);
		if (roots.size() == 0)
			throw new IllegalStateException("Graph has no roots.");
		// else
		// System.out.println(roots);

		// construct tree
		buildTree();

		state = Monitorable.STATE_TERMINAL;
	}

//	@Override
	public void reset() {
		initialize();
	}

	/**
	 * Set layout position of given node.
	 * 
	 * @param vertex
	 *            node to set position for
	 */
	private void setCurrentPositionFor(ONDEXConcept vertex) {
		locations.put(vertex, m_currentPoint);
	}

//	@Override
	public void setLocation(ONDEXConcept v, Point2D location) {
		Point2D c = getMaxXY();
		c.setLocation(c.getX() / 2, c.getY() / 2);
		Point pv = Point.of(location.getX() - c.getX(),
				location.getY() - c.getY());
		PolarPoint newLocation = PolarPoint.cartesianToPolar(pv);
//		polarLocations.get(v).setLocation(newLocation);
		polarLocations.put(v, newLocation);
	}

	/**
	 * Transform into radial positions.
	 * 
	 */
	private void setRadialLocations() {
		state = "calculating node positions";

		Point2D max = getMaxXY();
		double maxx = max.getX();
		double maxy = max.getY();
		double theta = 2 * Math.PI / maxx;

		double deltaRadius = maxy / 2;
		for (Map.Entry<ONDEXConcept, Point> entry : locations.entrySet()) {
			ONDEXConcept v = entry.getKey();
			Point p = entry.getValue();
			PolarPoint polarPoint = PolarPoint.of(p.x * theta,
					(p.y - 50) * deltaRadius);
			polarLocations.put(v, polarPoint);
			if (cancelled)
				return;
		}

		progress++;
	}

//	@Override
	public Point apply(ONDEXConcept v) {
		PolarPoint pp = polarLocations.get(v);
		Point2D c = getMaxXY();
		c.setLocation(c.getX() / 2, c.getY() / 2);
		Point cartesian = PolarPoint.polarToCartesian(pp);
		cartesian = cartesian.add(cartesian.x + c.getX(),
				cartesian.y + c.getY());
//		cartesian.setLocation(cartesian.getX() + c.getX(),
//				cartesian.getY() + c.getY());
		return cartesian;
	}

	@Override
	public int getMaxProgress() {
		if (roots != null)
			return roots.size() + 1;
		return 1;
	}

	@Override
	public int getMinProgress() {
		return 0;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public Throwable getUncaughtException() {
		return null;
	}

	@Override
	public boolean isAbortable() {
		return true;
	}

	@Override
	public boolean isIndeterminate() {
		return true;
	}

	@Override
	public void setCancelled(boolean c) {
		cancelled = c;
	}
}
