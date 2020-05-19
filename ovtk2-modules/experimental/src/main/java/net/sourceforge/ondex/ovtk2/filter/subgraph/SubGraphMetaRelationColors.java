package net.sourceforge.ondex.ovtk2.filter.subgraph;

import java.awt.Color;
import java.awt.Paint;
import java.util.function.Function;

import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelation;
import org.jungrapht.visualization.selection.SelectedState;

/**
 * Provides a transformation from a given ONDEXMetaRelation to a Colour.
 * 
 * @author taubertj
 * 
 */
public class SubGraphMetaRelationColors implements
		Function<ONDEXMetaRelation, Paint> {

	// ####FIELDS####

	// current SelectedState
	private SelectedState<ONDEXMetaRelation> pi = null;

	// ####CONSTRUCTOR####

	/**
	 * Initialises the colours for the edges in the graph.
	 * 
	 * @param pi
	 *            PickedInfo<ONDEXMetaRelation> pi
	 */
	public SubGraphMetaRelationColors(SelectedState<ONDEXMetaRelation> pi) {
		if (pi == null)
			throw new IllegalArgumentException(
					"PickedInfo instance must be non-null");
		this.pi = pi;
	}

	// ####METHODS####
	
	/**
	 * Returns result of transformation.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation
	 * @return Colour
	 */
	@Override
	public Color apply(ONDEXMetaRelation edge) {
		if (pi.isSelected(edge))
			return Color.YELLOW;
		if (edge.isVisible())
			return Color.BLUE;
		else
			return Color.LIGHT_GRAY;
	}
}
