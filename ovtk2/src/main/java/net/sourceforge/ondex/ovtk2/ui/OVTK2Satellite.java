package net.sourceforge.ondex.ovtk2.ui;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.config.Config;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.SatelliteVisualizationViewer;
import org.jungrapht.visualization.control.CrossoverScalingControl;
import org.jungrapht.visualization.control.ScalingControl;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import static org.jungrapht.visualization.MultiLayerTransformer.Layer;

/**
 * Represents a dynamic satellite view on the graph of a OVTK2Viewer.
 * 
 * @author taubertj
 * 
 */
public class OVTK2Satellite extends RegisteredJInternalFrame implements ActionListener {

	// generated
	private static final long serialVersionUID = 5016903790709100530L;

	// preferred size of this gadget
	private Dimension preferredSize = new Dimension(280, 210);

	// used for scaling by scaleToFit
	private ScalingControl scaler = new CrossoverScalingControl();

	// current OVTK2Viewer
	private OVTK2Viewer viewer = null;

	// contained satallite viewer
	private SatelliteVisualizationViewer<ONDEXConcept, ONDEXRelation> satellite = null;

	/**
	 * Initialise satellite view on a given viewer.
	 * 
	 * @param viewer
	 *            OVTK2Viewer
	 */
	public OVTK2Satellite(OVTK2Viewer viewer) {
		// set title and icon
		super(Config.language.getProperty("Satellite.Title"), "Satellite", Config.language.getProperty("Satellite.Title"), true, true, true, true);

		// dispose viewer on close
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		initIcon();

		// set layout
		this.getContentPane().setLayout(new BorderLayout());
		this.setViewer(viewer);
		this.pack();
	}

	/**
	 * Sets frame icon from file.
	 * 
	 */
	private void initIcon() {
		File imgLocation = new File("config/toolbarButtonGraphics/development/Application16.gif");
		URL imageURL = null;

		try {
			imageURL = imgLocation.toURI().toURL();
		} catch (MalformedURLException mue) {
			System.err.println(mue.getMessage());
		}

		this.setFrameIcon(new ImageIcon(imageURL));
	}

	/**
	 * Calculates the bounds of all nodes in a given viewer.
	 * 
	 * @return Point2D[] min bounds, max bounds
	 */
	private Point[] calcBounds() {
		Point[] result = new Point[2];
		Point min = null;
		Point max = null;
		LayoutModel<ONDEXConcept> layout = viewer.getVisualizationViewer().getVisualizationModel().getLayoutModel();
		Iterator<ONDEXConcept> it = layout.getGraph().vertexSet().iterator();
		while (it.hasNext()) {
			Point point = layout.apply(it.next());
			if (min == null) {
				min = point;
			}
			if (max == null) {
				max = point;
			}
			min = Point.of(Math.min(min.x, point.x), Math.min(min.y, point.y));
			max= Point.of(Math.max(max.x, point.x), Math.max(max.y, point.y));
		}
		result[0] = min;
		result[1] = max;
		return result;
	}

	/**
	 * Scale current satellite view to fit in whole graph.
	 * 
	 */
	public void scaleToFit() {

		// reset scaling for predictive behaviour
		satellite.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		satellite.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();

		// place layout center in center of the view
		Point[] calc = calcBounds();
		Point min = calc[0];
		Point max = calc[1];
		Point layout_bounds = Point.of(max.x - min.x, max.y - min.y);
		// layouter produced nice bounds
		if (layout_bounds.x > 0 && layout_bounds.y > 0) {
			Point2D screen_center = satellite.getCenter();
			Point2D layout_center = new Point2D.Double(screen_center.getX() - (layout_bounds.x / 2) - min.x, screen_center.getY() - (layout_bounds.y / 2) - min.y);
			satellite.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).translate(layout_center.getX(), layout_center.getY());

			// scale graph
			Point2D scale_bounds = new Point2D.Double(satellite.getWidth() / layout_bounds.x, satellite.getHeight() / layout_bounds.y);
			float scale = (float) Math.min(scale_bounds.getX(), scale_bounds.getY());
			scale = 0.85f * scale;
			scaler.scale(satellite, scale, satellite.getCenter());
		} else {
			// default scaler if layout not yet ready
			satellite.scaleToLayout(scaler);
		}
	}

	/**
	 * Sets viewer to be used in satellite view.
	 * 
	 * @param viewer
	 *            OVTK2Viewer
	 */
	public void setViewer(OVTK2Viewer viewer) {
		this.viewer = viewer;

		// new satellite viewer
		satellite = SatelliteVisualizationViewer.builder(viewer.getVisualizationViewer())
						.viewSize(preferredSize)
						.transparent(false)
						.build();

		satellite.setPreferredSize(this.preferredSize);
		satellite.getComponent().setSize(this.preferredSize);

		RenderContext<ONDEXConcept, ONDEXRelation> context = viewer.getVisualizationViewer().getRenderContext();

		// configure satellite appearance
		satellite.getRenderContext().setVertexDrawPaintFunction(context.getVertexDrawPaintFunction());
		satellite.getRenderContext().setVertexFillPaintFunction(context.getVertexFillPaintFunction());
		satellite.getRenderContext().setVertexShapeFunction(context.getVertexShapeFunction());
		satellite.getRenderContext().setEdgeDrawPaintFunction(context.getEdgeDrawPaintFunction());
//		satellite.getRenderContext().setEdgesetEdgeArrowPredicate(context.edgeStrokeFunction());
		satellite.getRenderContext().setEdgeStrokeFunction(context.edgeStrokeFunction());

		// add to content pane
		this.getContentPane().removeAll();
		this.getContentPane().add(satellite.getComponent(), BorderLayout.CENTER);

		JButton scaleToFit = new JButton(Config.language.getProperty("Satellite.ScaleToFit"));
		scaleToFit.addActionListener(this);
		this.getContentPane().add(scaleToFit, BorderLayout.SOUTH);
		this.revalidate();

		// fit graph in
		scaleToFit();
	}

	/**
	 * Returns current viewer.
	 * 
	 * @return OVTK2Viewer
	 */
	public OVTK2Viewer getViewer() {
		return viewer;
	}

	public void actionPerformed(ActionEvent arg0) {
		scaleToFit();
	}
}
