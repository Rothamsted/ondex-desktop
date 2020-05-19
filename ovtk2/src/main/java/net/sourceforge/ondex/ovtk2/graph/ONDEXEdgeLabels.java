package net.sourceforge.ondex.ovtk2.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;

/**
 * Provides a transformation from a given ONDEXRelation to a String as label.
 * 
 * @author taubertj
 * @author Matthew Pocock
 */
public class ONDEXEdgeLabels implements Function<ONDEXRelation, String> {

	/**
	 * contains mapping id to label
	 */
	private final Map<ONDEXRelation, String> labels;

	/**
	 * Map contains what relation labels to show
	 */
	private final Map<ONDEXRelation, Boolean> mask;

	/**
	 * How to fill the label mask
	 */
	private Boolean defaultMask = Boolean.FALSE;

	/**
	 * Initialises the labels for the edges in the graph.
	 * 
	 */
	public ONDEXEdgeLabels() {
		this.labels = new HashMap<>();
		this.mask = new HashMap<>();
	}
	/**
	 * Mask fillMask
	 * 
	 * @param value
	 *            boolean
	 */
	public void fillMask(boolean value) {
		for (ONDEXRelation r : this.mask.keySet()) {
			this.mask.put(r, value);
		}
		// necessary for edges not yet displayed
		defaultMask = value;
	}

	/**
	 * Returns the mask
	 * 
	 */
	public Map<ONDEXRelation, Boolean> getMask() {
		return this.mask;
	}

	/**
	 * Returns result of transformation.
	 * 
	 * @param edge
	 *            ONDEXRelation
	 * @return String
	 */
	public String apply(ONDEXRelation edge) {
		if (!mask.getOrDefault(edge, defaultMask)) {
			return "";
		} else {
			if (!labels.containsKey(edge))
				updateLabel(edge);
			return labels.get(edge);
		}
	}

	/**
	 * Update all labels from the graph.
	 * 
	 */
	public void updateAll() {
		labels.clear();
	}

	/**
	 * Update the label of a given edge.
	 * 
	 * @param edge
	 *            ONDEXRelation
	 */
	public void updateLabel(ONDEXRelation edge) {
		ONDEXRelation ar = edge;
		labels.put(edge, getLabel(ar));
	}

	/**
	 * Extracts the label from a given edge.
	 * 
	 * @param edge
	 *            ONDEXRelation
	 * @return String
	 */
	private String getLabel(ONDEXRelation edge) {
		String label;
		// first try fullname of rt
		RelationType rt = edge.getOfType();
		label = rt.getFullname();
		// else take id of rt
		if (label.trim().length() == 0) {
			label = rt.getId();
		}
		label = makeMaxLength(label);
		return label;
	}

	/**
	 * Makes maximum length texts.
	 * 
	 * @param text
	 *            String to shorten
	 * @return shorten String
	 */
	private String makeMaxLength(String text) {
		if (text.length() > 15) {
			return text.substring(0, 14) + "...";
		} else
			return text;
	}

}
