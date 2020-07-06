package net.sourceforge.ondex.ovtk2.metagraph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Point2D;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.config.Config;
import net.sourceforge.ondex.ovtk2.graph.ONDEXJUNGGraph;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Desktop;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Viewer;
import net.sourceforge.ondex.ovtk2.ui.popup.MetaConceptMenu;
import net.sourceforge.ondex.ovtk2.ui.popup.MetaRelationMenu;
import net.sourceforge.ondex.ovtk2.ui.popup.PopupVertexEdgeMenuMousePlugin;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.VisualizationScrollPane;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.control.CrossoverScalingControl;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse.Mode;
import org.jungrapht.visualization.control.ScalingControl;
import org.jungrapht.visualization.layout.algorithms.KKLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.util.IterativeContext;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.renderers.Renderer;
import org.jungrapht.visualization.renderers.Renderer.VertexLabel.Position;

import static org.jungrapht.visualization.MultiLayerTransformer.*;

public class ONDEXMetaGraphPanel extends JPanel implements ActionListener, ChangeListener, ComponentListener {

	// generated
	private static final long serialVersionUID = -981647571571550005L;

	// rendering hints
	private Map<RenderingHints.Key, Object> hints = new HashMap<>();

	// MetaGraph layout used
	private KKLayoutAlgorithm<ONDEXMetaConcept> layout = null;

	private VisualizationModel<ONDEXConcept, ONDEXRelation> visualizationModel;

	// display metagraph
	private ONDEXMetaGraph meta = null;

	// preferred size of this gadget
	private Dimension preferredSize = new Dimension(400, 300);

	// used for scaling by scaleToFit
	private ScalingControl scaler = new CrossoverScalingControl();

	// parent OVTK2Viewer
	private OVTK2Viewer viewer = null;

	// JUNG visualisation
	private VisualizationViewer<ONDEXMetaConcept, ONDEXMetaRelation> visviewer = null;

	/**
	 * Constructor for a given ONDEXMetaGraph.
	 * 
	 * @param viewer
	 *            OVTK2Viewer
	 */
	public ONDEXMetaGraphPanel(OVTK2Viewer viewer) {
		super(new BorderLayout());
		this.viewer = viewer;
		this.meta = viewer.getMetaGraph();

		initVisviewer();
	}

	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();

		// short cut to metadata legend
		if (cmd.equals("legend")) {
			// fire action to show legend
			JCheckBoxMenuItem item = new JCheckBoxMenuItem();
			item.setSelected(true);
			OVTK2Desktop.getInstance().actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, "legend"));
		}

		// show main graph
		else if (cmd.equals("mainGraph")) {
			try {
				viewer.setIcon(false);
				// OVTK-320
				viewer.toFront();
			} catch (PropertyVetoException e) {
				System.err.println("De-Iconification was vetoed by property");
			}
		}

	}

	/**
	 * Calculates the bounds of all nodes in a given layout.
	 * 
	 * @return Point2D[] min bounds, max bounds
	 */
	private Point[] calcBounds() {
		Point[] result = new Point[2];
		Point min = null;
		Point max = null;
		VisualizationModel<ONDEXMetaConcept, ONDEXMetaRelation> visualizationModel =
				visviewer.getVisualizationModel();
		LayoutModel<ONDEXMetaConcept> layoutModel = visualizationModel.getLayoutModel();
		Iterator<ONDEXMetaConcept> it = visualizationModel.getGraph().vertexSet().iterator();
		while (it.hasNext()) {
			Point point = layoutModel.apply(it.next());
			if (min == null) {
				min = point;
			}
			if (max == null) {
				max = point;
			}
			min = Point.of(Math.min(min.x, point.x), Math.min(min.y, point.y));
			max = Point.of(Math.max(max.x, point.x), Math.max(max.y, point.y));
		}
		result[0] = min;
		result[1] = max;
		return result;
	}

	@Override
	public void componentHidden(ComponentEvent arg0) {

	}

	@Override
	public void componentMoved(ComponentEvent arg0) {

	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		// automatic scaling to new window size
		this.scaleToFit();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {

	}

	public VisualizationViewer<ONDEXMetaConcept, ONDEXMetaRelation> getVisualizationViewer() {
		return visviewer;
	}

	/**
	 * Creates JUNG visualization of metagraph.
	 * 
	 */
	private void initVisviewer() {

		ONDEXJUNGGraph jung = viewer.getONDEXJUNGGraph();
//		visualizationModel = viewer.getVisualizationViewer().getVisualizationModel();

		// new metagraph viewer
		layout = new KKLayoutAlgorithm<>();
		visviewer = VisualizationViewer.builder(meta).layoutAlgorithm(layout).viewSize(preferredSize).build();
//				new VisualizationViewer<ONDEXMetaConcept, ONDEXMetaRelation>(layout, preferredSize);
//		LayoutAlgorithm<ONDEXMetaConcept> layout = new KKLayoutAlgorithm<>();
//		visviewer.getVisualizationModel().setGraph(meta, false);
//		visviewer.getVisualizationModel().setLayoutAlgorithm(layout);
		visviewer.setDoubleBuffered(false);
		visviewer.setBackground(Color.white);

		// set label position and label transformer
		visviewer.getRenderContext().setVertexLabelPosition(Position.AUTO);
		visviewer.getRenderContext().setVertexLabelFunction(new ONDEXMetaConceptLabels(jung));
		visviewer.getRenderContext().setEdgeLabelFunction(new ONDEXMetaRelationLabels(jung));

		// set visible attribute renderer
		visviewer.getRenderContext().setEdgeDrawPaintFunction(new ONDEXMetaRelationColors(jung, visviewer.getSelectedEdgeState()));
		visviewer.getRenderContext().setVertexShapeFunction(new ONDEXMetaConceptShapes(jung));
		// visviewer.getRenderContext().setVertexDrawPaintTransformer(
		// new ONDEXMetaConceptColors( aog));
		visviewer.getRenderContext().setVertexFillPaintFunction(new ONDEXMetaConceptColors(jung, visviewer.getSelectedVertexState()));
//		visviewer.getRenderContext().setEdgeArrowPredicate(new ONDEXMetaRelationArrows(jung));
		visviewer.getRenderContext().setEdgeStrokeFunction(new ONDEXMetaRelationStrokes(jung));

		// set antialiasing painting on
		Map<?, ?> temp = visviewer.getRenderingHints();

		// copying necessary because of typesafety
		Iterator<?> it = temp.keySet().iterator();
		while (it.hasNext()) {
			RenderingHints.Key key = (RenderingHints.Key) it.next();
			hints.put(key, temp.get(key));
		}
		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		visviewer.setRenderingHints(hints);

		// standard mouse support
		DefaultModalGraphMouse<ONDEXMetaConcept, ONDEXMetaRelation> graphMouse = new DefaultModalGraphMouse<>();

		// Trying out our new popup menu mouse producer...
		PopupVertexEdgeMenuMousePlugin<ONDEXMetaConcept, ONDEXMetaRelation> myPlugin = new PopupVertexEdgeMenuMousePlugin<>();
		// Add some popup menus for the edges and vertices to our mouse
		// producer.
		JPopupMenu edgeMenu = new MetaRelationMenu(viewer);
		JPopupMenu vertexMenu = new MetaConceptMenu(viewer);
		myPlugin.setEdgePopup(edgeMenu);
		myPlugin.setVertexPopup(vertexMenu);

		graphMouse.add(myPlugin); // Add our new producer to the mouse

		visviewer.setGraphMouse(graphMouse);
		visviewer.addKeyListener(graphMouse.getModeKeyListener());

		// zoom pane and mouse menu in the corner
		VisualizationScrollPane scrollPane = new VisualizationScrollPane(visviewer);
		JMenuBar menu = new JMenuBar();
		menu.add(graphMouse.getModeMenu());
		scrollPane.setCorner(menu);

		// set graph mode to picking
		graphMouse.setMode(Mode.PICKING);

		// button panel
		JPanel buttons = new JPanel();
		BoxLayout buttons_layout = new BoxLayout(buttons, BoxLayout.LINE_AXIS);
		buttons.setLayout(buttons_layout);

		// metadata legend button
		JButton legend = new JButton(Config.language.getProperty("MetaGraph.MetaDataLegend"));
		legend.addActionListener(this);
		legend.setActionCommand("legend");
		buttons.add(legend);

		// metadata legend button
		JButton mainGraph = new JButton(Config.language.getProperty("MetaGraph.ShowMainGraph"));
		mainGraph.addActionListener(this);
		mainGraph.setActionCommand("mainGraph");
		buttons.add(mainGraph);

		// make sure that the layout has actually finished
		if (layout instanceof IterativeContext) {
			IterativeContext iterativeContext = (IterativeContext)layout;
			while (!iterativeContext.done()) {
				iterativeContext.step();
			}
		}


		// add to content pane
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(buttons, BorderLayout.SOUTH);
		this.revalidate();
		this.scaleToFit();
		this.addComponentListener(this);
	}

	/**
	 * Performs a re-layout of the meta graph.
	 */
	public void relayout() {
			LayoutModel<ONDEXMetaConcept> layoutModel =
					visviewer.getVisualizationModel().getLayoutModel();
			layoutModel.setSize(visviewer.getWidth(), visviewer.getHeight());
			while (!layout.done()) {
				layout.step();
			}
			visviewer.getVisualizationModel().getModelChangeSupport().fireModelChanged();
			this.scaleToFit();
	}

	/**
	 * Scale current metagraph view to fit in whole graph.
	 * 
	 */
	public void scaleToFit() {

		// reset scaling for predictive behaviour
		visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();

		// place layout centre in centre of the view
		Point[] calc = calcBounds();
		Point min = calc[0];
		Point max = calc[1];

		// check for empty graph
		if (min != null && max != null) {
			Point2D layout_bounds = new Point2D.Double(max.x - min.x, max.y - min.y);
			// layouter produced nice bounds
			if (layout_bounds.getX() > 0 && layout_bounds.getY() > 0) {
				Point2D screen_center = visviewer.getCenter();
				Point2D layout_center = new Point2D.Double(screen_center.getX() - (layout_bounds.getX() / 2) - min.x, screen_center.getY() - (layout_bounds.getY() / 2) - min.y);
				visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).translate(layout_center.getX(), layout_center.getY());

				// scale graph
				Point2D scale_bounds = new Point2D.Double(visviewer.getSize().getWidth() / layout_bounds.getX(), visviewer.getSize().getHeight() / layout_bounds.getY());
				float scale = (float) Math.min(scale_bounds.getX(), scale_bounds.getY());
				scale = 0.8f * scale;
				scaler.scale(visviewer, scale, visviewer.getCenter());
			}
		}
	}

	public void setMouseMode(boolean picking) {
		ModalGraphMouse graphMouse = (ModalGraphMouse) visviewer.getGraphMouse();
		graphMouse.setMode(picking ? Mode.PICKING : Mode.TRANSFORMING);
	}

	public void stateChanged(ChangeEvent arg0) {
		this.updateUI();
	}

}
