package net.sourceforge.ondex.ovtk2.util;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaGraphPanel;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Viewer;
import net.sourceforge.ondex.ovtk2.ui.mouse.OVTK2GraphMouse;
import net.sourceforge.ondex.ovtk2.util.graphml.GraphMLWriter;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.jungrapht.visualization.MultiLayerTransformer.Layer;

/**
 * @author hindlem, peschr, taubertj
 * 
 */
public class ImageWriterUtil<V, E> {

	private VisualizationViewer<V, E> visviewer;

	private int width;

	private int height;

	@SuppressWarnings("unchecked")
	public ImageWriterUtil(Object view) {
		if (view instanceof OVTK2Viewer) {
			visviewer = (VisualizationViewer<V, E>) ((OVTK2Viewer) view).getVisualizationViewer();
		} else if (view instanceof ONDEXMetaGraphPanel) {
			visviewer = (VisualizationViewer<V, E>) ((ONDEXMetaGraphPanel) view).getVisualizationViewer();
		} else if (view instanceof VisualizationViewer) {
			visviewer = (VisualizationViewer<V, E>) view;
		}
	}

	/**
	 * Calculates actual bounds of a painted graph.
	 * 
	 * @return Point2D[]
	 */
	private Point[] calcBounds() {
		Point min = null;
		Point max = null;
		LayoutModel<V> layout = visviewer.getVisualizationModel().getLayoutModel();
		for (V ondexNode : layout.getGraph().vertexSet()) {
			Point point = layout.apply(ondexNode);
			if (min == null) {
				min = point;
			}
			if (max == null) {
				max = point;
			}
			min = Point.of(Math.min(min.x, point.x), Math.min(min.y, point.y));
			max = Point.of(Math.max(max.x, point.x), Math.max(max.y, point.y));
		}
		// sanity checks, in case of empty graph
		if (min == null)
			min = Point.of(0, 0);
		if (max == null)
			max = Point.of(0, 0);
		// put results together
		Point[] result = new Point[2];
		result[0] = min;
		result[1] = max;
		// case for just one node, make distinct
		if (min.equals(max)) {
			min = Point.of(min.x - 20, min.y - 20);
			max = Point.of(max.x + 20, max.y + 20);
		}
		return result;
	}

	/**
	 * Centres all nodes to the available window
	 */
	public void center() {

		// reset scaling for predictive behaviour
		visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
		visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();

		// place layout center in center of the view
		Point[] calc = calcBounds();
		Point min = calc[0];
		Point max = calc[1];

		if (min == null || max == null) {
			return; // nothing to center on
		}

		Point2D screen_center = visviewer.getCenter();
		Point2D layout_bounds = new Point2D.Double(max.x - min.x, max.y - min.y);
		Point2D layout_center = new Point2D.Double(screen_center.getX() - (layout_bounds.getX() / 2) - min.x,
				screen_center.getY() - (layout_bounds.getY() / 2) - min.y);
		visviewer.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).translate(layout_center.getX(), layout_center.getY());

		// scale graph
		if (visviewer.getGraphMouse() instanceof OVTK2GraphMouse) {
			Point2D scale_bounds = new Point2D.Double(visviewer.getWidth() / layout_bounds.getX(), visviewer.getHeight() / layout_bounds.getY());
			float scale = (float) Math.min(scale_bounds.getX(), scale_bounds.getY());
			scale = 0.95f * scale;
			OVTK2GraphMouse mouse = (OVTK2GraphMouse) visviewer.getGraphMouse();
			mouse.getScaler().scale(visviewer, scale, visviewer.getCenter());
		}
	}

	/**
	 * Scales current viewer by given factor
	 * 
	 * @param scaleResolution
	 */
	private void scaleViewer(float scaleResolution) {
		// save old size
		width = visviewer.getWidth();
		height = visviewer.getHeight();

		// only scale when required
		if (scaleResolution != 1) {

			// this is for image scaling
			int newWidth = Math.round(width * scaleResolution);
			int newHeight = Math.round(height * scaleResolution);

			// set new size
			visviewer.getComponent().setSize(newWidth, newHeight);
			visviewer.setDoubleBuffered(false);
			center();
		}
	}

	/**
	 * Render content of OVTK2Viewer to an image file.
	 * 
	 * @param file
	 *            file to save to
	 * @param format
	 *            format to use
	 * @param scaleResolution
	 *            magnifier to scale resolution by
	 */
	public void writeImage(File file, String format, float scaleResolution) {

		Color c = visviewer.getBackground();

		// deselect nodes
		Iterator<V> SelectedVIt = visviewer.getSelectedVertexState().getSelected().iterator();
		while (SelectedVIt.hasNext()) {
			visviewer.getSelectedVertexState().select(SelectedVIt.next(), false);
		}
		// deselect edges
		Iterator<E> SelectedEIt = visviewer.getSelectedEdgeState().getSelected().iterator();
		while (SelectedEIt.hasNext()) {
			E Selected = SelectedEIt.next();
			visviewer.getSelectedEdgeState().select(Selected, false);
		}

		// get current setting for anti-aliasing
		Map<Key, Object> hints = visviewer.getRenderingHints();
		Object antializing = hints.get(RenderingHints.KEY_ANTIALIASING);

		// special case
		if (format.equalsIgnoreCase("eps")) {
			BufferedOutputStream fos = null;

			try {
				fos = new BufferedOutputStream(new FileOutputStream(file));

				// resize viewer
				scaleViewer(scaleResolution);

				// save old double buffer state
				boolean doubleBuff = visviewer.isDoubleBuffered();
				visviewer.setDoubleBuffered(false);

				// generate EPS graphics
				EpsGraphics g = new EpsGraphics("ONDEX_EPS_Graph_Export", fos, 0, 0, visviewer.getWidth(), visviewer.getHeight(), ColorMode.COLOR_CMYK);
				g.setAccurateTextMode(true);

				hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

				visviewer.setRenderingHints(hints); // not ness
				/*
				 * HACK
				 * 
				 * This allows identification of the background, <code>0.9607843
				 * 0.9607843 0.9607843 setrgbcolor</close> or 0.0 0.0 0.0
				 * 0.039215683937072754 setcmykcolor so "fill" can be commented
				 * out in the resulting eps file to achieve a transparent bg.
				 */
				// TODO: Create method in EPS2D to toggle background
				// painting
				visviewer.setBackground(new Color(0.96f, 0.96f, 0.96f));
				visviewer.getComponent().printAll(g);

				visviewer.setDoubleBuffered(doubleBuff);
				g.flush();
				g.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (fos != null)
					try {
						fos.close();
						visviewer.setDoubleBuffered(true);
						// only scale when required
						if (scaleResolution != 1) {
							visviewer.getComponent().setSize(width, height);
							center();
						}
						// restore original setting
						visviewer.setBackground(c);
						hints.put(RenderingHints.KEY_ANTIALIASING, antializing);
						visviewer.setRenderingHints(hints);
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		} else if (format.equalsIgnoreCase("graphml")) {
			try {
				// get graph of viewer
				Graph<V, E> graph = visviewer.getVisualizationModel().getGraph();

				// setup writer and save to file
				GraphMLWriter<V, E> writer = new GraphMLWriter<V, E>(graph);
				writer.setVertexFillTransformer(visviewer.getRenderContext().getVertexFillPaintFunction());
				writer.setEdgeColourTransformer(visviewer.getRenderContext().getEdgeDrawPaintFunction());
				writer.setVertexLabelTransformer(visviewer.getRenderContext().getVertexLabelFunction());
				writer.setEdgeLabelTransformer(visviewer.getRenderContext().getEdgeLabelFunction());

				writer.save(new BufferedWriter(new FileWriter(file)));

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			try {
				BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));

				// resize viewer
				scaleViewer(scaleResolution);

				// pick first image writer
				ImageWriter iw = null;
				Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName(format);
				int i = 0;
				while (it.hasNext()) {
					iw = it.next();
					ImageWriterSpi spi = iw.getOriginatingProvider();
					System.out.println("Using: " + spi.getPluginClassName() + " ; " + spi.getVendorName() + " ; " + spi.getVersion());
					i++;
				}
				if (i > 1)
					System.err.println("Multiple exporter plugins found. Order not specified.");
				iw.reset();
				iw.setOutput(ImageIO.createImageOutputStream(fos));

				// caches painted graph
				BufferedImage bi = null;

				// special requirements
				if (format.equalsIgnoreCase("bmp"))
					bi = new BufferedImage(visviewer.getWidth(), visviewer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
				else if (format.equalsIgnoreCase("png"))
					bi = new BufferedImage(visviewer.getWidth(), visviewer.getHeight(), BufferedImage.TYPE_INT_ARGB);
				else
					// standard case
					bi = new BufferedImage(visviewer.getWidth(), visviewer.getHeight(), BufferedImage.TYPE_INT_RGB);

				// actually paint graph
				Graphics2D graphics = bi.createGraphics();
				visviewer.getComponent().paintAll(graphics);
				graphics.dispose();

				// write to image
				iw.write(bi);
				iw.dispose();
				fos.flush();
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				visviewer.setDoubleBuffered(true);
				// only scale when required
				if (scaleResolution != 1) {
					visviewer.getComponent().setSize(width, height);
					center();
				}
			}
		}
	}
}
