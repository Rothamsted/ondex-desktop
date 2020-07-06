package net.sourceforge.ondex.ovtk2.graph.custom;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.graph.ONDEXJUNGGraph;
import net.sourceforge.ondex.ovtk2.util.AppearanceSynchronizer;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.VisualizationModel;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;
import org.jungrapht.visualization.renderers.AbstractVertexRenderer;
import org.jungrapht.visualization.renderers.HeavyweightVertexRenderer;
import org.jungrapht.visualization.transform.shape.GraphicsDecorator;

public class ONDEXBasicVertexRenderer extends HeavyweightVertexRenderer<ONDEXConcept, ONDEXRelation> {

	ONDEXJUNGGraph graph;

	public ONDEXBasicVertexRenderer(ONDEXJUNGGraph graph) {
		super();
		this.graph = graph;
	}

	private Path2D.Double createArrow() {
		int length = 80;
		Path2D.Double path = new Path2D.Double();
		Rectangle2D.Double rect = new Rectangle2D.Double(-1, -length, 2, length);
		path.append(rect, true);
		Rectangle2D.Double rect2 = new Rectangle2D.Double(0, -length, 2, length);
		path.append(rect2, true);
		CubicCurve2D c = new CubicCurve2D.Double();
		c.setCurve(0, -length, length / 4, -length - 20, length / 2, -length + 10, length, -length);
		path.append(c, true);
		Line2D line = new Line2D.Double(length, -length, length, -length / 3);
		path.append(line, true);
		c = new CubicCurve2D.Double();
		c.setCurve(length, -length / 3, length / 2, -length / 3 + 10, length / 4, -length / 3 - 20, 0, -length / 3);
		path.append(c, true);
		return path;
	}

	// TODO: this needs work to make sure it's okay with the jungrapht rendering
	@Override
	protected void paintIconForVertex(RenderContext<ONDEXConcept, ONDEXRelation> rc, VisualizationModel<ONDEXConcept, ONDEXRelation> visualizationModel, ONDEXConcept v) {
		super.paintIconForVertex(rc, visualizationModel, v);

		LayoutModel<ONDEXConcept> layoutModel = visualizationModel.getLayoutModel();
		// check for flagged drawing
		boolean flagged = false;
		AttributeName anFlag = graph.getMetaData().getAttributeName(AppearanceSynchronizer.FLAGGED);
		if (anFlag != null && v.getAttribute(anFlag) != null) {
			flagged = (Boolean) v.getAttribute(anFlag).getValue();
		}

		// draw arrow / flag
		if (flagged) {

			boolean vertexHit = true;
			// get the shape to be rendered
			Shape shape = createArrow();

			Point lp = layoutModel.apply(v);
			Point2D p = new Point2D.Double(lp.x, lp.y);
			p = rc.getMultiLayerTransformer().transform(MultiLayerTransformer.Layer.LAYOUT, p);
			float x = (float) p.getX();
			float y = (float) p.getY();
			// create a transform that translates to the location of
			// the vertex to be rendered
			AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
			// transform the vertex shape with xtransform
			shape = xform.createTransformedShape(shape);

			paintShapeForVertex(rc, v, shape);

		}
	}

	@Override
	protected void paintShapeForVertex(RenderContext<ONDEXConcept, ONDEXRelation> rc, ONDEXConcept v, Shape shape) {

		GraphicsDecorator g = rc.getGraphicsContext();
		Paint oldPaint = g.getPaint();
		Paint fillPaint = rc.getVertexFillPaintFunction().apply(v);
		if (fillPaint != null) {
			g.setPaint(fillPaint);
			// set alpha composite
			if (fillPaint instanceof Color && ((Color) fillPaint).getAlpha() == 0) {
				// TODO: introduce proper alpha blending
			} else {
				g.fill(shape);
			}
			g.setPaint(oldPaint);
		}
		Paint drawPaint = rc.getVertexDrawPaintFunction().apply(v);
		if (drawPaint != null) {
			g.setPaint(drawPaint);
			Stroke oldStroke = g.getStroke();
			Stroke stroke = rc.getVertexStrokeFunction().apply(v);
			if (stroke != null) {
				g.setStroke(stroke);
			}
			if (drawPaint instanceof Color && ((Color) drawPaint).getAlpha() == 0) {
				// TODO: introduce proper alpha blending
			} else {
				g.draw(shape);
			}
			g.setPaint(oldPaint);
			g.setStroke(oldStroke);
		}
	}
}
