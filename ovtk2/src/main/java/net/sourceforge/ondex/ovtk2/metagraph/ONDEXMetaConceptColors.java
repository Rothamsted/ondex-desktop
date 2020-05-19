package net.sourceforge.ondex.ovtk2.metagraph;

import java.awt.Color;
import java.awt.Paint;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;

import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.ovtk2.config.Config;
import net.sourceforge.ondex.ovtk2.graph.ONDEXJUNGGraph;
import org.jungrapht.visualization.selection.SelectedState;

public class ONDEXMetaConceptColors implements Function<ONDEXMetaConcept, Paint> {

	// ####FIELDS####

	// contains mapping concept class to colour
	private Map<ConceptClass, Color> colors = null;

	// current ONDEXJUNGGraph
	private ONDEXJUNGGraph graph = null;

	// current PickedInfo
	private SelectedState<ONDEXMetaConcept> pi = null;

	// ####CONSTRUCTOR####

	/**
	 * Initialises the colours for the nodes in the graph.
	 * 
	 * @param graph
	 *            ONDEXJUNGGraph
	 */
	public ONDEXMetaConceptColors(ONDEXJUNGGraph graph, SelectedState<ONDEXMetaConcept> pi) {
		if (pi == null)
			throw new IllegalArgumentException("PickedInfo instance must be non-null");
		this.pi = pi;

		this.graph = graph;
		this.colors = new Hashtable<ConceptClass, Color>();

		// initialise colours
		updateAll();
	}

	// ####METHODS####

	/**
	 * Returns result of transformation.
	 * 
	 * @param node
	 *            ONDEXMetaRelation
	 * @return Colour
	 */
	@Override
	public Color apply(ONDEXMetaConcept node) {
		if (pi.isSelected(node)) {
			return Config.nodePickedColor;
		} else {
			updateColor(node);
			return colors.get(node.id);
		}
	}

	/**
	 * Update all colours from the graph.
	 * 
	 */
	public void updateAll() {
		for (ConceptClass cc : graph.getMetaData().getConceptClasses()) {
			// update with a dummy edge
			updateColor(new ONDEXMetaConcept(graph, cc));
		}
	}

	/**
	 * Update the colour of a given edge.
	 * 
	 * @param node
	 *            ONDEXMetaRelation
	 */
	public void updateColor(ONDEXMetaConcept node) {
		Color c = Config.getColorForConceptClass((ConceptClass) node.id);
		updateColor(node, c);
	}

	/**
	 * Update the colour of a given edge with a given colour.
	 * 
	 * @param node
	 *            ONDEXMetaRelation
	 * @param c
	 *            Colour
	 */
	public void updateColor(ONDEXMetaConcept node, Color c) {
		if (!node.isVisible()) {
			c = new Color(255, 255, 255, 0);// make difference clearer
		}
		colors.put(node.getConceptClass(), c);
	}

}