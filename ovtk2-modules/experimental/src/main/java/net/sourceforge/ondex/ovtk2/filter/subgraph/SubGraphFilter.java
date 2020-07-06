package net.sourceforge.ondex.ovtk2.filter.subgraph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.StateEdit;

import net.sourceforge.ondex.InvalidPluginArgumentException;
import net.sourceforge.ondex.ONDEXPluginArguments;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.util.BitSetFunctions;
import net.sourceforge.ondex.filter.subgraph.ArgumentNames;
import net.sourceforge.ondex.filter.subgraph.Filter;
import net.sourceforge.ondex.ovtk2.config.Config;
import net.sourceforge.ondex.ovtk2.filter.OVTK2Filter;
import net.sourceforge.ondex.ovtk2.graph.VisibilityUndo;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConcept;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConceptLabels;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConceptShapes;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaGraph;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelation;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelationArrows;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelationLabels;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelationStrokes;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Desktop;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Viewer;
import net.sourceforge.ondex.ovtk2.util.ErrorDialog;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.CrossoverScalingControl;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse;
import org.jungrapht.visualization.control.ScalingControl;
import org.jungrapht.visualization.control.SelectingGraphMousePlugin;
import org.jungrapht.visualization.layout.algorithms.KKLayoutAlgorithm;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.renderers.Renderer;
import org.jungrapht.visualization.selection.SelectedState;

import static org.jungrapht.visualization.MultiLayerTransformer.*;
import static org.jungrapht.visualization.control.ModalGraphMouse.*;

/**
 * Filter to extract specified subgraphs.
 * 
 * @author taubertj
 */
public class SubGraphFilter extends OVTK2Filter implements ComponentListener,
		ActionListener, ListSelectionListener {

	/**
	 * generated
	 */
	private static final long serialVersionUID = -72634806239183449L;

	// preferred size of this gadget
	private Dimension preferredSize = new Dimension(400, 300);

	// display meta graph
	private ONDEXMetaGraph meta = null;

	// used for scaling by scaleToFit
	private ScalingControl scaler = new CrossoverScalingControl();

	// JUNG visualisation
	private VisualizationViewer<ONDEXMetaConcept, ONDEXMetaRelation> visviewer = null;

	// MetaGraph layout used
	private KKLayoutAlgorithm<ONDEXMetaConcept> layout = null;

	// rendering hints
	private Map<RenderingHints.Key, Object> hints = new HashMap<RenderingHints.Key, Object>();

	// triggers processing
	private JButton goButton = null;

	// uses shortest paths then
	private JCheckBox shortest = new JCheckBox(
			"non-redundant meta data selection?");

	// presents list of concepts in graph alphabetically sorted
	private JList nodeSelection = new JList();

	// caching labels
	private Map<ONDEXConcept, String> labels = new HashMap<ONDEXConcept, String>();

	// list index of node
	private Map<ONDEXConcept, Integer> indices = new HashMap<ONDEXConcept, Integer>();
	
	/**
	 * Filter has been used
	 */
	private boolean used = false;

	/**
	 * Wraps an ONDEXNode to provide a cache based toString function and
	 * compareTo.
	 * 
	 * @author taubertj
	 * 
	 */
	private class ONDEXNodeWrapper implements Comparable<ONDEXNodeWrapper> {

		ONDEXConcept node;

		public ONDEXNodeWrapper(ONDEXConcept node) {
			this.node = node;
		}

		public String toString() {
			return labels.get(node);
		}

		@Override
		public int compareTo(ONDEXNodeWrapper o) {
			if (o != null)
				return toString().compareToIgnoreCase(o.toString());
			else
				return -1;
		}

		@Override
		public boolean equals(Object obj) {
			return node.equals(obj);
		}

		@Override
		public int hashCode() {
			return node.hashCode();
		}
	}

	public SubGraphFilter(OVTK2Viewer viewer) {
		super(viewer);

		this.setLayout(new BorderLayout());

		// get current meta graph
		meta = viewer.getMetaGraph();

		initGUI();

		populateNodeList();

		propagateGraphSelection();

		this.revalidate();
	}

	/**
	 * Populates the visible node list sorted alphabetically.
	 */
	private void populateNodeList() {

		// clear previous
		labels.clear();
		indices.clear();

		// calculate labels for visible nodes and sort
		ONDEXNodeWrapper[] sorted = new ONDEXNodeWrapper[graph.getVertices()
				.size()];
		int i = 0;
		for (ONDEXConcept node : graph.getVertices()) {
			String label = viewer.getNodeLabels().getLabel(node);
			labels.put(node, label);
			sorted[i] = new ONDEXNodeWrapper(node);
			i++;
		}
		Arrays.sort(sorted);

		// make new list model and add nodes to it
		DefaultListModel model = new DefaultListModel();
		i = 0;
		for (ONDEXNodeWrapper wrapper : sorted) {
			model.addElement(wrapper);
			indices.put(wrapper.node, i);
			i++;
		}

		// set model to list
		nodeSelection.setModel(model);
		nodeSelection.revalidate();
	}

	/**
	 * Reflect the selection of concepts in the viewer as selection of concept
	 * class here.
	 */
	private void propagateGraphSelection() {

		// get selection from graph
		List<Integer> selected = new ArrayList<Integer>();
		for (ONDEXConcept node : viewer.getSelectedNodes()) {
			if (indices.containsKey(node)) {
				ConceptClass cc = node.getOfType();
				for (ONDEXMetaConcept mc : meta.getVertices()) {
					if (mc.getMetaData().equals(cc)) {
						visviewer.getSelectedVertexState().select(mc, true);
						break;
					}
				}
				selected.add(indices.get(node));
			}
		}

		// set list selection to same as graph selection
		int[] array = new int[selected.size()];
		int i = 0;
		for (Integer j : selected) {
			array[i] = j;
			i++;
		}
		if (array.length > 0)
			nodeSelection.setSelectedIndices(array);
	}

	/**
	 * Present user with nice graph selection.
	 */
	private void initGUI() {

		// scroll and border around selection list for concepts
		nodeSelection.addListSelectionListener(this);
		JScrollPane listScroller = new JScrollPane(nodeSelection);
		listScroller.setPreferredSize(new Dimension(400, 100));
		listScroller.setBorder(BorderFactory
				.createTitledBorder("Root concept selection"));

		// new meta graph viewer
		layout = new KKLayoutAlgorithm<>();
		visviewer = VisualizationViewer.<ONDEXMetaConcept, ONDEXMetaRelation>builder(meta)
				.viewSize(preferredSize).layoutAlgorithm(layout).build();
		visviewer.setDoubleBuffered(false);
		visviewer.setBackground(Color.white);

		// set label position and label transformer
		visviewer.getRenderContext().setVertexLabelPosition(Renderer.VertexLabel.Position.AUTO);
		visviewer.getRenderContext().setVertexLabelFunction(
				new ONDEXMetaConceptLabels(graph));
		visviewer.getRenderContext().setEdgeLabelFunction(
				new ONDEXMetaRelationLabels(graph));

		// set visible attribute renderer
		visviewer.getRenderContext().setEdgeDrawPaintFunction(
				new SubGraphMetaRelationColors(visviewer.getSelectedEdgeState()));
		visviewer.getRenderContext().setVertexShapeFunction(
				new ONDEXMetaConceptShapes(graph));
		visviewer.getRenderContext()
				.setVertexFillPaintFunction(
						new SubGraphMetaConceptColors(visviewer
								.getSelectedVertexState()));
//		visviewer.getRenderContext().setEdgeArrowPredicate(
//				new ONDEXMetaRelationArrows(graph));
		visviewer.getRenderContext().setEdgeStrokeFunction(
				new ONDEXMetaRelationStrokes(graph));

		// set anti-aliasing painting on
		Map<?, ?> temp = visviewer.getRenderingHints();

		// copying necessary because of type safety
		Iterator<?> it = temp.keySet().iterator();
		while (it.hasNext()) {
			RenderingHints.Key key = (RenderingHints.Key) it.next();
			hints.put(key, temp.get(key));
		}
		hints.put(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		visviewer.setRenderingHints(hints);

		// standard mouse support
		DefaultModalGraphMouse<ONDEXMetaConcept, ONDEXMetaRelation> graphMouse = new DefaultModalGraphMouse<ONDEXMetaConcept, ONDEXMetaRelation>();
		graphMouse
				.add(new SelectingGraphMousePlugin<ONDEXMetaConcept, ONDEXMetaRelation>(
						InputEvent.BUTTON1_DOWN_MASK, 0, InputEvent.BUTTON3_DOWN_MASK));

		visviewer.setGraphMouse(graphMouse);
		visviewer.addKeyListener(graphMouse.getModeKeyListener());

		// zoom pane and mouse menu in the corner
		VisualizationScrollPane scrollPane = new VisualizationScrollPane(visviewer);
		JMenuBar menu = new JMenuBar();
		menu.add(graphMouse.getModeMenu());
		scrollPane.setCorner(menu);
		scrollPane.setBorder(BorderFactory
				.createTitledBorder("Select path in meta-graph"));

		// set graph mode to picking
		graphMouse.setMode(Mode.PICKING);

		goButton = new JButton("Apply");
		goButton.setEnabled(false);
		goButton.addActionListener(this);

		JButton selectAll = new JButton("Select All");
		selectAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (ONDEXMetaConcept mc : meta.getVertices())
					visviewer.getSelectedVertexState().select(mc, true);
				for (ONDEXMetaRelation mr : meta.getEdges())
					visviewer.getSelectedEdgeState().select(mr, true);
			}
		});

		JPanel south = new JPanel();
		BoxLayout layout = new BoxLayout(south, BoxLayout.LINE_AXIS);
		south.setLayout(layout);
		south.add(shortest);
		south.add(selectAll);
		south.add(goButton);

		// splits meta-graph and concept selection apart
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				listScroller, scrollPane);
		splitPane.setOneTouchExpandable(true);

		// add to content pane
		this.add(splitPane, BorderLayout.CENTER);
		this.add(south, BorderLayout.SOUTH);
		this.revalidate();
		this.scaleToFit();
	}

	/**
	 * Calculates the bounds of all nodes in a given layout.
	 * 
	 * @return Point2D[] min bounds, max bounds
	 */
	private Point[] calcBounds() {
		LayoutModel<ONDEXMetaConcept> layoutModel = visviewer.getVisualizationModel().getLayoutModel();
		Point[] result = new Point[2];
		Point min = null;
		Point max = null;
		Iterator<ONDEXMetaConcept> it = layoutModel.getGraph().vertexSet()
				.iterator();
		while (it.hasNext()) {
			Point point = layoutModel.apply(it.next());
			if (min == null) {
				min = point;
			}
			if (max == null) {
				max = point;
			}
			min = Point.of(Math.min(min.x, point.x),
					Math.min(min.y, point.y));
			max = Point.of(Math.max(max.x, point.x),
					Math.max(max.y, point.y));
		}
		result[0] = min;
		result[1] = max;
		return result;
	}

	/**
	 * Scale current metagraph view to fit in whole graph.
	 */
	public void scaleToFit() {

		// reset scaling for predictive behaviour
		visviewer.getRenderContext().getMultiLayerTransformer()
				.getTransformer(Layer.LAYOUT).setToIdentity();
		visviewer.getRenderContext().getMultiLayerTransformer()
				.getTransformer(Layer.VIEW).setToIdentity();

		// place layout centre in centre of the view
		Point[] calc = calcBounds();
		Point min = calc[0];
		Point max = calc[1];

		// check for empty graph
		if (min != null && max != null) {
			Point2D layout_bounds = new Point2D.Double(max.x - min.x,
					max.y - min.y);
			// layouter produced nice bounds
			if (layout_bounds.getX() > 0 && layout_bounds.getY() > 0) {
				Point2D screen_center = visviewer.getCenter();
				Point2D layout_center = new Point2D.Double(screen_center.getX()
						- (layout_bounds.getX() / 2) - min.x,
						screen_center.getY() - (layout_bounds.getY() / 2)
								- min.y);
				visviewer.getRenderContext().getMultiLayerTransformer()
						.getTransformer(Layer.VIEW)
						.translate(layout_center.getX(), layout_center.getY());

				// scale graph
				Point2D scale_bounds = new Point2D.Double(visviewer.getSize()
						.getWidth() / layout_bounds.getX(), visviewer.getSize()
						.getHeight() / layout_bounds.getY());
				float scale = (float) Math.min(scale_bounds.getX(),
						scale_bounds.getY());
				scale = 0.8f * scale;
				scaler.scale(visviewer, scale, visviewer.getCenter());
			}
		}
	}

	@Override
	public String getName() {
		return Config.language.getProperty("Name.Menu.Filter.SubGraph");
	}

	@Override
	public void componentHidden(ComponentEvent e) {

	}

	@Override
	public void componentMoved(ComponentEvent e) {

	}

	@Override
	public void componentResized(ComponentEvent e) {
		this.scaleToFit();
	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		try {
			callFilter();
			used = true;
		} catch (InvalidPluginArgumentException e) {
			ErrorDialog.show(e);
		}
	}

	private void callFilter() throws InvalidPluginArgumentException {
		StateEdit edit = new StateEdit(new VisibilityUndo(
				viewer.getONDEXJUNGGraph()), this.getName());
		OVTK2Desktop desktop = OVTK2Desktop.getInstance();
		desktop.setRunningProcess(this.getName());

		// contains results
		Set<ONDEXConcept> concepts = null;
		Set<ONDEXRelation> relations = null;

		// get user choices
		SelectedState<ONDEXMetaConcept> ccPI = visviewer.getSelectedVertexState();
		SelectedState<ONDEXMetaRelation> rtPI = visviewer.getSelectedEdgeState();

		// seed nodes from viewer start subgraph at each of them
		for (Object o : nodeSelection.getSelectedValuesList ()) {
			ONDEXConcept root = ((ONDEXNodeWrapper) o).node;
			ConceptClass rootCC = root.getOfType();

			// execute filter
			Filter filter = new Filter();

			ONDEXPluginArguments fa = new ONDEXPluginArguments(
					filter.getArgumentDefinitions());
			fa.addOption(ArgumentNames.ROOT_ARG, root.getId());

			// graph traversal to identify selected concept classes
			for (ONDEXMetaConcept mc : meta.getVertices()) {
				// found root, make sure its still picked
				if (mc.getMetaData().equals(rootCC) && ccPI.isSelected(mc)) {
					seenConcepts = new HashSet<ONDEXMetaConcept>();
					seenRelations = new HashSet<ONDEXMetaRelation>();
					traverseGraph(fa, ccPI, rtPI, mc);
				}
			}

			filter.setArguments(fa);
			filter.setONDEXGraph(graph);
			try {
				filter.start();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// concatenate results
			if (concepts == null) {
				concepts = filter.getVisibleConcepts();
				relations = filter.getVisibleRelations();
			} else {
				concepts = BitSetFunctions.or(concepts,
						filter.getVisibleConcepts());
				relations = BitSetFunctions.or(relations,
						filter.getVisibleRelations());
			}
		}

		// show results in graph
		if (concepts != null) {

			// set all relations to invisible
			graph.setVisibility(graph.getRelations(), false);

			// set all concepts to invisible
			graph.setVisibility(graph.getConcepts(), false);

			// first set concepts visible
			graph.setVisibility(concepts, true);

			// second set relations visible
			graph.setVisibility(relations, true);

			// propagate change to viewer
			viewer.getVisualizationViewer().getVisualizationModel().getModelChangeSupport().fireModelChanged();}
		edit.end();
		viewer.getUndoManager().addEdit(edit);
		desktop.getOVTK2Menu().updateUndoRedo(viewer);
		desktop.notifyTerminationOfProcess();
	}

	private Set<ONDEXMetaConcept> seenConcepts = null;

	private Set<ONDEXMetaRelation> seenRelations = null;

	/**
	 * Simple Tuple implementation.
	 * 
	 * @author taubertj
	 * @param <A>
	 * @param <B>
	 */
	public class Tuple<A, B> {

		private A first;

		private B second;

		public Tuple(A first, B second) {
			this.first = first;
			this.second = second;
		}

		public A getFirst() {
			return first;
		}

		public B getSecond() {
			return second;
		}
	}

	/**
	 * Performs a breath first search to find first occurrences of relation
	 * types and concept clases.
	 * 
	 * @param fa
	 *            FilterArguments get filled on the way
	 * @param ccPI
	 *            PickedState<ONDEXMetaConcept> for checking user selection
	 * @param rtPI
	 *            PickedState<ONDEXMetaRelation> for checking user selection
	 * @param root
	 *            ONDEXMetaConcept where to start
	 */
	private void traverseGraph(ONDEXPluginArguments fa,
			SelectedState<ONDEXMetaConcept> ccPI,
							   SelectedState<ONDEXMetaRelation> rtPI, ONDEXMetaConcept root)
			throws InvalidPluginArgumentException {

		if (shortest.isSelected())
			seenConcepts.add(root);

		// breath first search (shortest paths
		LinkedList<Tuple<ONDEXMetaConcept, Integer>> queue = new LinkedList<Tuple<ONDEXMetaConcept, Integer>>();
		queue.add(new Tuple<ONDEXMetaConcept, Integer>(root, Integer
				.valueOf(-1)));

		while (!queue.isEmpty()) {

			// get next element from queue and add to arguments
			Tuple<ONDEXMetaConcept, Integer> current = queue.poll();

			// unwrap tuple
			int depth = current.getSecond();
			if (depth < Filter.levelsCC.length) {
				ONDEXMetaConcept mc = current.getFirst();

				// special case for ignoring root
				if (depth > -1)
					fa.addOption(Filter.levelsCC[depth], mc.getMetaData()
							.getId());

				// first outgoing relations
				for (ONDEXMetaRelation outRel : meta.getOutEdges(mc)) {
					if (!seenRelations.contains(outRel)) {
						if (shortest.isSelected())
							seenRelations.add(outRel);
						if (rtPI.isSelected(outRel)
								&& depth + 1 < Filter.levelsCC.length)
							fa.addOption(Filter.levelsRT[depth + 1], outRel
									.getMetaData().getId());

						ONDEXMetaConcept to = meta.getDest(outRel);
						// prevent self loop
						if (!seenConcepts.contains(to) && ccPI.isSelected(to)) {
							if (shortest.isSelected())
								seenConcepts.add(to);
							queue.add(new Tuple<ONDEXMetaConcept, Integer>(to,
									Integer.valueOf(depth + 1)));
						}
					}
				}

				// second incoming relations
				for (ONDEXMetaRelation inRel : meta.getInEdges(mc)) {
					if (!seenRelations.contains(inRel)) {
						if (shortest.isSelected())
							seenRelations.add(inRel);
						if (rtPI.isSelected(inRel)
								&& depth + 1 < Filter.levelsCC.length)
							fa.addOption(Filter.levelsRT[depth + 1], inRel
									.getMetaData().getId());

						ONDEXMetaConcept from = meta.getSource(inRel);
						// prevent self loop
						if (!seenConcepts.contains(from) && ccPI.isSelected(from)) {
							if (shortest.isSelected())
								seenConcepts.add(from);
							queue.add(new Tuple<ONDEXMetaConcept, Integer>(
									from, Integer.valueOf(depth + 1)));
						}
					}
				}
			}
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {

			if (nodeSelection.getSelectedIndex() == -1) {
				// No selection, disable fire button.
				goButton.setEnabled(false);

			} else {
				// Selection, enable the fire button.
				goButton.setEnabled(true);

				// propagate list selection to meta-graph
				for (Object value : nodeSelection.getSelectedValuesList ()) {
					ConceptClass cc = ((ONDEXNodeWrapper) value).node
							.getOfType();
					for (ONDEXMetaConcept mc : meta.getVertices()) {
						if (mc.getMetaData().equals(cc)) {
							visviewer.getSelectedVertexState().select(mc, true);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public boolean hasBeenUsed() {
		return used;
	}

}
