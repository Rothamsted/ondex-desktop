package net.sourceforge.ondex.ovtk2.layout;

import javax.swing.JPanel;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.ovtk2.ui.OVTK2PropertiesAggregator;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationViewer;
import org.jungrapht.visualization.layout.algorithms.AbstractIterativeLayoutAlgorithm;
import org.jungrapht.visualization.layout.algorithms.LayoutAlgorithm;
import org.jungrapht.visualization.layout.model.AbstractLayoutModel;
import org.jungrapht.visualization.layout.model.LayoutModel;

/**
 * Abstract class for all OVTK2 Layouter.
 * 
 * @author taubertj
 * 
 */
public abstract class OVTK2Layouter  implements LayoutAlgorithm<ONDEXConcept> {

	// current VisualizationViewer<ONDEXConcept, ONDEXRelation>
	protected VisualizationViewer<ONDEXConcept, ONDEXRelation> viewer;

	protected LayoutModel<ONDEXConcept> layoutModel;
	protected Graph<ONDEXConcept, ONDEXRelation> graph;

	/**
	 * Constructor sets internal variables from given OVTK2PropertiesAggregator.
	 * 
	 * @param viewer
	 *            OVTK2PropertiesAggregator
	 */
	public OVTK2Layouter(OVTK2PropertiesAggregator viewer) {
		this.viewer = viewer.getVisualizationViewer();
		this.graph = viewer.getONDEXJUNGGraph();
	}

	@Override
	public void visit(LayoutModel<ONDEXConcept> layoutModel) {
		this.layoutModel = layoutModel;
		this.graph = layoutModel.getGraph();
	}

	/**
	 * Prevent changing the graph belonging to this layout.
	 * 
	 * @param graph
	 *            Graph<ONDEXConcept, ONDEXRelation>
	 */
	public void setGraph(Graph<ONDEXConcept, ONDEXRelation> graph) {
		throw new IllegalArgumentException("Operation not supported");
	}

	/**
	 * Setting a new viewer is allowed.
	 * 
	 * @param viewer
	 *            OVTK2PropertiesAggregator
	 */
	public void setViewer(OVTK2PropertiesAggregator viewer) {
		layoutModel.setGraph(viewer.getONDEXJUNGGraph());
		this.viewer = viewer.getVisualizationViewer();
	}

	/**
	 * Returns layout option producer for gadget.
	 * 
	 * @return JPanel
	 */
	public abstract JPanel getOptionPanel();

	/**
	 * Clean up of layout resources to avoid memory leaks
	 */
	public void cleanUp() {

	}
}
