package net.sourceforge.ondex.ovtk2.util;

import java.awt.Frame;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.layout.OVTK2Layouter;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Viewer;
import net.sourceforge.ondex.ovtk2.ui.mouse.OVTK2GraphMouse;
import net.sourceforge.ondex.tools.threading.monitoring.IndeterminateProcessAdapter;
import net.sourceforge.ondex.tools.threading.monitoring.Monitorable;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithmTransition;
import org.jungrapht.visualization.layout.algorithms.util.IterativeContext;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.layout.util.VisRunnable;

import static org.jungrapht.visualization.MultiLayerTransformer.*;

/**
 * Utility functions mainly used to influence visualisation.
 * 
 * @author taubertj
 * 
 */
public class VisualisationUtils {

	/**
	 * Dirty hack so that centring can be done after layout is ready.
	 */
	public static class MyVisRunner extends VisRunnable {

		/**
		 * viewer.
		 */
		private OVTK2PropertiesAggregator v;

		/**
		 * hacked constructor.
		 * 
		 * @param process
		 *            see super.
		 * @param activeViewer
		 *            the active OVTK2PropertiesAggregator.
		 */
		public MyVisRunner(IterativeContext process, OVTK2PropertiesAggregator activeViewer) {
			super(process);
			v = activeViewer;
		}

		/**
		 * hacked run method.
		 * 
//		 * @see edu.uci.ics.jung.algorithms.layout.util.VisRunner#run()
		 */
		public void run() {
			super.run();
			v.center();
		}

	}

	/**
	 * Relayout given OVTK2PropertiesAggregator, reset Layouter.
	 * 
	 * @param viewer
	 *            OVTK2PropertiesAggregator
	 */
	public static void relayout(final OVTK2PropertiesAggregator viewer, final Frame parent) {

		LayoutModel<ONDEXConcept> layoutModel = viewer.getVisualizationViewer().getVisualizationModel().getLayoutModel();
		LayoutAlgorithm<ONDEXConcept> layoutAlgorithm = viewer.getVisualizationViewer().getVisualizationModel().getLayoutAlgorithm();

		// get current layout
//		ObservableCachingLayout<ONDEXConcept, ONDEXRelation> layout = (ObservableCachingLayout<ONDEXConcept, ONDEXRelation>) viewer.getVisualizationViewer().getGraphLayout();

		// reuse current layouter
		if (layoutAlgorithm instanceof OVTK2Layouter) {
			final OVTK2Layouter layouter = (OVTK2Layouter) layoutAlgorithm;

			if (layouter instanceof Monitorable) {
				// layout knows about its progress
				Monitorable p = (Monitorable) layouter;

				OVTKProgressMonitor.start(parent, "Running Layout...", p);
				Thread t = new Thread() {
					public void run() {
						runLayout(layouter, viewer);
					}
				};
				t.start();
			} else {
				// wrap into indefinite process
				IndeterminateProcessAdapter p = new IndeterminateProcessAdapter() {
					public void task() {
						runLayout(layouter, viewer);
					}
				};

				// set layout
				OVTKProgressMonitor.start(parent, "Running Layout...", p);
				p.start();
			}
		} else {
			layoutAlgorithm.visit(layoutModel);
//			layout.getDelegate().reset();
		}
	}

	/**
	 * Runs new layouter and triggers transition from old one.
	 * 
	 * @param layouter_new
	 *            new OVTK2Layouter
	 * @param viewer
	 *            the current OVTK2PropertiesAggregator
	 */
	public static void runLayout(OVTK2Layouter layouter_new, OVTK2PropertiesAggregator viewer) {
		// set initial values
		viewer.getVisualizationViewer().getVisualizationModel().getLayoutModel().setSize(
				viewer.getVisualizationViewer().getWidth(), viewer.getVisualizationViewer().getHeight());

//		layouter_new.setSize(viewer.getVisualizationViewer().getSize());

		// show transition between layouts
//		LayoutAlgorithm<ONDEXConcept> layouter_old = viewer.getVisualizationViewer().getVisualizationModel().getLayoutAlgorithm();
//		LayoutAlgorithmTransition<ONDEXConcept> transition =
//				new LayoutTransition<ONDEXConcept, ONDEXRelation>(viewer.getVisualizationViewer(), layouter_old, layouter_new);
		LayoutAlgorithmTransition.animate(viewer.getVisualizationViewer(), layouter_new);


		// start layout process
//		MyVisRunner runner = new MyVisRunner(transition, viewer);
//		runner.relax();
	}

	/**
	 * Zoom into picked nodes.
	 * 
	 * @param viewer
	 *            current active OVTK2Viewer
	 */
	public static void zoomIn(OVTK2Viewer viewer) {

		// get JUNG VisualizationViewer
		VisualizationViewer<ONDEXConcept, ONDEXRelation> vv = viewer.getVisualizationViewer();

		// local copy of pick state
		Set<ONDEXConcept> picked = new HashSet<ONDEXConcept>(viewer.getSelectedNodes());
		if (picked.size() == 1) {
			// first scale it
			OVTK2GraphMouse mouse = (OVTK2GraphMouse) vv.getGraphMouse();
			mouse.getScaler().scale(vv, 1.1f, vv.getCenter());

			// move centre of graph to selection
			ONDEXConcept root = picked.iterator().next();
			Point q = vv.getVisualizationModel().getLayoutModel().apply(root);
			Point2D lvc = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getCenter());
			double dx = (lvc.getX() - q.x);
			double dy = (lvc.getY() - q.y);
			vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);

			return;
		}

		// reset scaling for predictive behaviour
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		// get boundaries of selected nodes
		for (ONDEXConcept node : picked) {
			Point pos = viewer.getVisualizationViewer().getVisualizationModel().getLayoutModel().apply(node);
			if (pos.x < minX) {
				minX = pos.x;
			}
			if (pos.x > maxX) {
				maxX = pos.x;
			}
			if (pos.y < minY) {
				minY = pos.y;
			}
			if (pos.y > maxY) {
				maxY = pos.y;
			}
		}

		// System.out.println(minX + " " + maxX + " " + minY + " " + maxY);

		// centre graph
		Point2D screen_center = vv.getCenter();
		Point2D layout_bounds = new Point2D.Double(maxX - minX, maxY - minY);
		Point2D layout_center = new Point2D.Double(screen_center.getX() - (layout_bounds.getX() / 2) - minX, screen_center.getY() - (layout_bounds.getY() / 2) - minY);
		vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).translate(layout_center.getX(), layout_center.getY());
		// scale graph
		Point2D scale_bounds = new Point2D.Double(vv.getWidth() / layout_bounds.getX(), vv.getHeight() / layout_bounds.getY());
		float scale = (float) Math.min(scale_bounds.getX(), scale_bounds.getY());
		scale = 0.92f * scale;
		OVTK2GraphMouse mouse = (OVTK2GraphMouse) vv.getGraphMouse();
		mouse.getScaler().scale(vv, scale, vv.getCenter());
	}

}
