package net.sourceforge.ondex.ovtk2.graph;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import org.jgrapht.Graph;
import org.jungrapht.visualization.util.Context;

import java.util.function.Predicate;

/**
 * Provides a predicate whether or not to draw arrows at edges.
 * 
 * @author taubertj
 * 
 */
public class ONDEXEdgeArrows implements Predicate<Context<Graph<ONDEXConcept, ONDEXRelation>, ONDEXRelation>> {

	// whether or not to display arrows on edges
	private boolean showArrow = true;

	/**
	 * Whether or not to show arrows on edges.
	 * 
	 * @param show
	 */
	public void setShowArrow(boolean show) {
		this.showArrow = show;
	}

	/**
	 * Returns current arrow status.
	 * 
	 * @return arrow shown
	 */
	public boolean isShowArrow() {
		return this.showArrow;
	}

	/**
	 * Return whether or not to draw arrows for a given Context.
	 * 
	 * @param object
	 *            Context<Graph<ONDEXNode, ONDEXEdge>, ONDEXEdge>
	 * @return boolean
	 */
	@Override
	public boolean test(Context<Graph<ONDEXConcept, ONDEXRelation>, ONDEXRelation> object) {
		return showArrow;
	}

}
