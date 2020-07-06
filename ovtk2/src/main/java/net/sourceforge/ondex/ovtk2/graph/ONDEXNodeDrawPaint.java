package net.sourceforge.ondex.ovtk2.graph;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.sourceforge.ondex.core.ONDEXConcept;

/**
 * Provides a transformation from a given ONDEXConcept to a Color.
 * 
 * @author taubertj
 * @author Matthew Pocock
 */
public class ONDEXNodeDrawPaint implements Function<ONDEXConcept, Paint> {

	/**
	 * Different colour selection strategies
	 * 
	 * @author taubertj
	 * 
	 */
	public enum NodeDrawPaintSelection {
		MANUAL, NONE
	}

	/**
	 * contains mapping to paint
	 */
	private final Map<ONDEXConcept, Paint> colors;

	/**
	 * contains mapping to alpha values
	 */
	private final Map<ONDEXConcept, Integer> alphas;

	/**
	 * current colour selection
	 */
	private NodeDrawPaintSelection strategy;

	/**
	 * Initialises the colours for the nodes in the graph.
	 * 
	 */
	public ONDEXNodeDrawPaint() {
		this.colors = new HashMap<>();
		this.alphas = new HashMap<>();
		this.strategy = NodeDrawPaintSelection.NONE;
	}

	/**
	 * Returning current DrawPaintSelection.
	 * 
	 * @return current DrawPaintSelection
	 */
	public NodeDrawPaintSelection getDrawPaintSelection() {
		return strategy;
	}

	/**
	 * Sets the DrawPaintSelection to use for the nodes in the graph.
	 * 
	 * @param s
	 *            NodeDrawPaintSelection to use
	 */
	public void setDrawPaintSelection(NodeDrawPaintSelection s) {
		this.strategy = s;
	}

	/**
	 * Returns result of transformation.
	 * 
	 * @param node
	 *            ONDEXConcept
	 * @return Color
	 */
	@Override
	public Paint apply(ONDEXConcept node) {
		if (!colors.containsKey(node))
			updateColor(node);
		return colors.getOrDefault(node, Color.black);
	}

	/**
	 * Returns alpha value for node or null.
	 * 
	 * @param node
	 * @return
	 */
	public Integer transformAlpha(ONDEXConcept node) {
		if (alphas.containsKey(node))
			return alphas.getOrDefault(node, 255);
		else
			return null;
	}

	/**
	 * Update all colours from the graph.
	 */
	public void updateAll() {
		if (strategy != NodeDrawPaintSelection.MANUAL)
			colors.clear();
	}

	/**
	 * Update the colour of a given node.
	 * 
	 * @param node
	 *            ONDEXConcept
	 */
	public void updateColor(ONDEXConcept node) {
		Paint paint = null;
		switch (strategy) {
		case NONE:
			paint = Color.BLACK;
			break;
		case MANUAL:
			break;
		default:
			paint = Color.BLACK;
			break;
		}

		// Explicitly set alpha channel of colour
		if (paint instanceof Color && alphas.containsKey(node)) {
			Color color = (Color) paint;
			paint = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphas.get(node));
		}
		updateColor(node, paint);
	}

	/**
	 * Update the Paint of a given node with a given Paint.
	 * 
	 * @param node
	 *            ONDEXConcept
	 * @param paint
	 *            Paint
	 */
	public void updateColor(ONDEXConcept node, Paint paint) {
		colors.put(node, paint);
	}

	/**
	 * Update the alpha value of a given node.
	 * 
	 * @param node
	 *            ONDEXConcept
	 * @param alpha
	 *            Integer
	 */
	public void updateAlpha(ONDEXConcept node, Integer alpha) {
		alphas.put(node, alpha);
	}
}
