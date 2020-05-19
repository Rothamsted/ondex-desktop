package net.sourceforge.ondex.ovtk2.filter.subgraph;

import java.awt.Color;
import java.awt.Paint;
import java.util.function.Function;

import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConcept;
import org.jungrapht.visualization.selection.SelectedState;

/**
 * Provides a transformation from a given ONDEXMetaConcept to a Colour.
 * 
 * @author taubertj
 * 
 */
public class SubGraphMetaConceptColors implements
		Function<ONDEXMetaConcept, Paint> {

	// ####FIELDS####

	// current SelectedState
	private SelectedState<ONDEXMetaConcept> pi = null;

	// ####CONSTRUCTOR####

	/**
	 * Initialises the colours for the nodes in the graph.
	 * 
	 * @param pi
	 *            PickedInfo<ONDEXMetaConcept>
	 */
	public SubGraphMetaConceptColors(SelectedState<ONDEXMetaConcept> pi) {
		if (pi == null)
			throw new IllegalArgumentException(
					"PickedInfo instance must be non-null");
		this.pi = pi;
	}

	// ####METHODS####

	/**
	 * Returns result of transformation.
	 * 
	 * @param node
	 *            ONDEXMetaConcept
	 * @return Colour
	 */
	@Override
	public Color apply(ONDEXMetaConcept node) {
		if (pi.isSelected(node))
			return Color.YELLOW;
		if (node.isVisible())
			return Color.BLUE;
		else
			return Color.LIGHT_GRAY;
	}

}