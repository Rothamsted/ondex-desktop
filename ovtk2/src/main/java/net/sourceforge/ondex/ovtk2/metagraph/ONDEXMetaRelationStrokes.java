package net.sourceforge.ondex.ovtk2.metagraph;

import net.sourceforge.ondex.core.ONDEXGraph;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.function.Function;

/**
 * Provides a transformation from a given ONDEXMetaRelation to a edge stroke.
 * 
 * @author taubertj
 * 
 */
public class ONDEXMetaRelationStrokes implements Function<ONDEXMetaRelation, Stroke> {

	// default case
	private Stroke defaultStroke = new BasicStroke(1.0f);

	/**
	 * Initialises the strokes for the edges in the graph.
	 * 
	 * @param aog
	 *            AbstractONDEXGraph
	 */
	public ONDEXMetaRelationStrokes(ONDEXGraph aog) {

	}

	public void setThickness(int thickness) {
		defaultStroke = new BasicStroke(Float.valueOf(thickness));
	}

	/**
	 * Returns result of transformation.
	 * 
	 * @param edge
	 *            ONDEXEdge
	 * @return String
	 */
	@Override
	public Stroke apply(ONDEXMetaRelation edge) {
		return defaultStroke;
	}

}
