package net.sourceforge.ondex.ovtk2.metagraph;

import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.ovtk2.graph.ONDEXJUNGGraph;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Legend;
import net.sourceforge.ondex.ovtk2.ui.OVTK2Viewer;
import net.sourceforge.ondex.ovtk2.ui.menu.actions.ViewMenuAction;
import net.sourceforge.ondex.ovtk2.ui.popup.MetaConceptMenu.MetaConceptVisibilityItem;
import net.sourceforge.ondex.ovtk2.ui.popup.MetaRelationMenu.MetaRelationVisibilityItem;
import org.jgrapht.GraphType;
import org.jgrapht.graph.AbstractGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jungrapht.visualization.layout.algorithms.util.Pair;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represents a metagraph view consisting of ConceptClasses and RelationType ids
 * for a wrapped AbstractONDEXGraph.
 * 
 * @author taubertj
 * 
 */
public class ONDEXMetaGraph extends AbstractGraph<ONDEXMetaConcept, ONDEXMetaRelation> implements ActionListener {

	/**
	 * generated
	 */
	private static final long serialVersionUID = 6960385035687981002L;

	// list for EventListeners
	protected EventListenerList listenerList = new EventListenerList();

	// the main graph viewer
	private OVTK2Viewer mainviewer;

	// the jung graph belonging to the aog.
	private ONDEXJUNGGraph graph = null;

	// Map of vertices to Pair of adjacency sets {incoming, outgoing}
	private Map<ONDEXMetaConcept, Pair<Set<ONDEXMetaRelation>>> vertices = new HashMap<>();

	// Map of edges to incident vertex pairs
	private Map<ONDEXMetaRelation, Pair<ONDEXMetaConcept>> edges = new HashMap<>();

	// lazy change event
	private ChangeEvent event = null;

	/**
	 * Whether or not to update the legend synchronously
	 */
	public boolean updateLegend = true;

	/**
	 * Sets wrapped AbstractONDEXGraph.
	 * 
	 * @param graph
	 *            wrapped AbstractONDEXGraph
	 */
	public ONDEXMetaGraph(ONDEXJUNGGraph graph, OVTK2Viewer mainviewer) {
		this.graph = graph;
		this.mainviewer = mainviewer;

		updateMetaData();
	}

	/**
	 * Updates the current hold meta data
	 * 
	 */
	public void updateMetaData() {
		edges.clear();
		vertices.clear();

		// sync existing concept classes/relation types
		for (ConceptClass cc : graph.getMetaData().getConceptClasses()) {
			if (graph.getConceptsOfConceptClass(cc).size() > 0)
				createVertexForConceptClass(cc);
		}
		for (RelationType rt : graph.getMetaData().getRelationTypes()) {
			if (graph.getRelationsOfRelationType(rt).size() > 0)
				createEdgeForRelationType(rt);
		}

		// refresh possible meta data legend
		if (ViewMenuAction.isLegendShown())
			ViewMenuAction.getLegend().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OVTK2Legend.REFRESH));
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();

		if (arg0.getSource() instanceof MetaConceptVisibilityItem || arg0.getSource() instanceof ONDEXMetaConcept) {

			ONDEXMetaConcept mc;
			if (arg0.getSource() instanceof ONDEXMetaConcept)
				mc = (ONDEXMetaConcept) arg0.getSource();
			else
				mc = ((MetaConceptVisibilityItem) arg0.getSource()).getMetaConcept();

			if (cmd.equals("show")) {

				// show concepts
				for (ONDEXConcept c : mc.getConcepts()) {
					graph.setVisibility(c, true);
				}

				for (ONDEXConcept c : mc.getConcepts()) {
					// make relations visible as per OVTK-295
					for (ONDEXRelation r : graph.getRelationsOfConcept(c)) {
						if (graph.isVisible(r.getToConcept()) && graph.isVisible(r.getFromConcept()))
							graph.setVisibility(r, true);
					}
				}
			} else if (cmd.equals("hide")) {

				// hide all connected relations first, for safety.
				for (ONDEXMetaRelation incidentMR : getIncidentEdges(mc)) {
					ActionEvent eventDummy = new ActionEvent(incidentMR, ActionEvent.ACTION_PERFORMED, "hide");
					actionPerformed(eventDummy);
				}

				// hide concepts
				for (ONDEXConcept c : mc.getConcepts()) {
					graph.setVisibility(c, false);
				}
			}
			// TODO: Update colour interface
			mainviewer.getVisualizationViewer().getVisualizationModel().getModelChangeSupport().fireModelChanged();
		} else if (arg0.getSource() instanceof MetaRelationVisibilityItem || arg0.getSource() instanceof ONDEXMetaRelation) {

			ONDEXMetaRelation mr;
			if (arg0.getSource() instanceof ONDEXMetaRelation)
				mr = (ONDEXMetaRelation) arg0.getSource();
			else
				mr = ((MetaRelationVisibilityItem) arg0.getSource()).getMetaRelation();

			if (cmd.equals("show")) {

				// show relations
				for (ONDEXRelation r : mr.getRelations()) {
					if (!graph.isVisible(r)) {
						graph.setVisibility(r.getFromConcept(), true);
						graph.setVisibility(r.getToConcept(), true);
						graph.setVisibility(r, true);
					}
				}
			} else if (cmd.equals("hide")) {

				// hide relations
				for (ONDEXRelation r : mr.getRelations()) {
					graph.setVisibility(r, false);
				}
			}
			// TODO: Update colour interface
			mainviewer.getVisualizationViewer().getVisualizationModel().getModelChangeSupport().fireModelChanged();
		}

		// repaint ourself, just in case
		mainviewer.getMetaGraphPanel().repaint();

		// refresh possible meta data legend
		if (updateLegend && ViewMenuAction.isLegendShown())
			ViewMenuAction.getLegend().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OVTK2Legend.REFRESH));
	}

	private void createEdgeForRelationType(RelationType rt) {
		// collect information on pairs of concept classes
		Set<Pair<ConceptClass>> betweenCC = new HashSet<>();
		for (ONDEXRelation r : graph.getRelationsOfRelationType(rt)) {
			ConceptClass from = r.getFromConcept().getOfType();
			ConceptClass to = r.getToConcept().getOfType();
			betweenCC.add(Pair.of(from, to));
		}
		// create meta relation for each pair
		for (Pair<ConceptClass> ccPair : betweenCC) {
			ONDEXMetaConcept from = new ONDEXMetaConcept(graph, ccPair.first);
			ONDEXMetaConcept to = new ONDEXMetaConcept(graph, ccPair.second);
			this.addEdge(new ONDEXMetaRelation(graph, rt, ccPair), Pair.of(from, to));
		}
		this.fireStateChange();
	}

	private void createVertexForConceptClass(ConceptClass cc) {
		this.addVertex(new ONDEXMetaConcept(graph, cc));
		this.fireStateChange();
	}

	/**
	 * Adds a ChangeListener to the model.
	 * 
	 * @param l
	 *            ChangeListener
	 */
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	protected Pair<ONDEXMetaConcept> getValidatedEndpoints(ONDEXMetaRelation edge, Pair<? extends ONDEXMetaConcept> endpoints)
	{
		if (edge == null)
			throw new IllegalArgumentException("input edge may not be null");

		if (endpoints == null)
			throw new IllegalArgumentException("endpoints may not be null");

		Pair<ONDEXMetaConcept> new_endpoints = Pair.of(endpoints.first, endpoints.second);
		if (containsEdge(edge))
		{
			Pair<ONDEXMetaConcept> existing_endpoints = getEndpoints(edge);
			if (!existing_endpoints.equals(new_endpoints)) {
				throw new IllegalArgumentException("edge " + edge +
						" already exists in this graph with endpoints " + existing_endpoints +
						" and cannot be added with endpoints " + endpoints);
			} else {
				return null;
			}
		}
		return new_endpoints;
	}
	/**
	 * Adds a new ONDEXMetaRelation to the graph given by a Pair of endpoints
	 * ONDEXMetaConcepts.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @param endpoints
	 *            Pair of ONDEXMetaConcepts
	 * @return true if successful
	 */
	public boolean addEdge(ONDEXMetaRelation edge, Pair<? extends ONDEXMetaConcept> endpoints) {
		Pair<ONDEXMetaConcept> new_endpoints = getValidatedEndpoints(edge, endpoints);
		if (new_endpoints == null)
			return false;

		edges.put(edge, new_endpoints);

		ONDEXMetaConcept source = new_endpoints.first;
		ONDEXMetaConcept dest = new_endpoints.second;

		if (!vertices.containsKey(source))
			this.addVertex(source);

		if (!vertices.containsKey(dest))
			this.addVertex(dest);

		getIncoming_internal(dest).add(edge);
		getOutgoing_internal(source).add(edge);

		return true;
	}

	@Override
	public Set<ONDEXMetaRelation> getAllEdges(ONDEXMetaConcept sourceVertex, ONDEXMetaConcept targetVertex) {
		return findAllEdgesBetween(sourceVertex, targetVertex);
	}

	@Override
	public ONDEXMetaRelation getEdge(ONDEXMetaConcept sourceVertex, ONDEXMetaConcept targetVertex) {
		return findEdge(sourceVertex, targetVertex);
	}

	@Override
	public Supplier<ONDEXMetaConcept> getVertexSupplier() {
		return null;
	}

	@Override
	public Supplier<ONDEXMetaRelation> getEdgeSupplier() {
		return null;
	}

	@Override
	public ONDEXMetaRelation addEdge(ONDEXMetaConcept sourceVertex, ONDEXMetaConcept targetVertex) {
		return null;
	}

	@Override
	public boolean addEdge(ONDEXMetaConcept sourceVertex, ONDEXMetaConcept targetVertex, ONDEXMetaRelation ondexMetaRelation) {
		return false;
	}

	@Override
	public ONDEXMetaConcept addVertex() {
		return null;
	}

	/**
	 * Adds a new ONDEXMetaConcept to the graph.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept
	 * @return true if successful
	 */
	public boolean addVertex(ONDEXMetaConcept vertex) {
		if (vertex == null) {
			throw new IllegalArgumentException("vertex may not be null");
		}
		if (!vertices.containsKey(vertex)) {
			vertices.put(vertex, Pair.of(new HashSet<>(), new HashSet<>()));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns whether or not an ONDEXMetaRelation is contained in the wrapped
	 * AbstractONDEXGraph.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return true if edge contained in graph
	 */
	public boolean containsEdge(ONDEXMetaRelation edge) {
		return edges.containsKey(edge);
	}

	/**
	 * Returns whether or not an ONDEXMetaConcept is contained in the wrapped
	 * AbstractONDEXGraph.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return true if vertex contained in graph
	 */
	public boolean containsVertex(ONDEXMetaConcept vertex) {
		return vertices.containsKey(vertex);
	}

	@Override
	public Set<ONDEXMetaRelation> edgeSet() {
		return edges.keySet();
	}

	@Override
	public int degreeOf(ONDEXMetaConcept vertex) {
		return this.getIncidentEdges(vertex).size();
	}

	@Override
	public Set<ONDEXMetaRelation> edgesOf(ONDEXMetaConcept vertex) {
		return new HashSet<>(this.getIncidentEdges(vertex));
	}

	@Override
	public int inDegreeOf(ONDEXMetaConcept vertex) {
		return this.incomingEdgesOf(vertex).size();
	}

	@Override
	public Set<ONDEXMetaRelation> incomingEdgesOf(ONDEXMetaConcept vertex) {
		Set<ONDEXMetaRelation> incoming = new HashSet<>();
		edgesOf(vertex).forEach(e -> {
			ONDEXMetaConcept source = getSource(e);
			if (source != vertex) {
				incoming.add(e);
			}
		});
		return incoming;
	}

	@Override
	public int outDegreeOf(ONDEXMetaConcept vertex) {
		return this.outgoingEdgesOf(vertex).size();
	}

	@Override
	public Set<ONDEXMetaRelation> outgoingEdgesOf(ONDEXMetaConcept vertex) {
		Set<ONDEXMetaRelation> outgoing = new HashSet<>();
		edgesOf(vertex).forEach(e -> {
			ONDEXMetaConcept source = getDest(e);
			if (source != vertex) {
				outgoing.add(e);
			}
		});
		return outgoing;
	}

	@Override
	public ONDEXMetaRelation removeEdge(ONDEXMetaConcept sourceVertex, ONDEXMetaConcept targetVertex) {
		return null;
	}

	/**
	 * Returns the edge between two ONDEXMetaConcepts if there is one.
	 * 
	 * @param v1
	 *            ONDEXMetaConcept
	 * @param v2
	 *            ONDEXMetaConcept
	 * @return ONDEXMetaRelation if existing
	 */
	public ONDEXMetaRelation findEdge(ONDEXMetaConcept v1, ONDEXMetaConcept v2) {
		for (ONDEXMetaRelation edge : getOutgoing_internal(v1))
			if (this.getDest(edge).equals(v2))
				return edge;
		return null;
	}

	/**
	 * Returns all edges between two ONDEXMetaConcepts.
	 * this is surely wrong, as it returns at most only one edge (tan)
	 * @param v1
	 *            ONDEXMetaConcept
	 * @param v2
	 *            ONDEXMetaConcept
	 * @return all ONDEXEdges
	 */
	public Collection<ONDEXMetaRelation> findEdgeSet(ONDEXMetaConcept v1, ONDEXMetaConcept v2) {
		ArrayList<ONDEXMetaRelation> edge_collection = new ArrayList<ONDEXMetaRelation>(1);
		if (!containsVertex(v1) || !containsVertex(v2))
			return edge_collection;
		ONDEXMetaRelation e = findEdge(v1, v2);
		if (e == null)
			return edge_collection;
		edge_collection.add(e);
		return edge_collection;
	}

	public Set<ONDEXMetaRelation> findAllEdgesBetween(ONDEXMetaConcept source, ONDEXMetaConcept target) {
		Set<ONDEXMetaRelation> set = new HashSet<>();
		for (ONDEXMetaRelation edge : getOutgoing_internal(source))
			if (this.getDest(edge).equals(target))
				set.add(edge);
		for(ONDEXMetaRelation edge : getIncoming_internal(target))
			if (this.getSource(edge).equals(source))
				set.add(edge);

		return set;
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 */
	protected void fireStateChange() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				// Lazily create the event:
				if (event == null)
					event = new ChangeEvent(this);
				((ChangeListener) listeners[i + 1]).stateChanged(event);
			}
		}
	}

	/**
	 * Returns the destination of the edge (via toConcept) representing
	 * RelationType.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return ONDEXMetaConcept representing ConceptClass
	 */
	public ONDEXMetaConcept getDest(ONDEXMetaRelation edge) {
		return this.getEndpoints(edge).second;
	}

	/**
	 * Returns the number of RelationTypes contained in the wrapped
	 * AbstractONDEXGraph.
	 * 
	 * @return int count of RelationTypes
	 */
	public int getEdgeCount() {
		return edges.size();
	}

	/**
	 * Returns a list of ONDEXMetaRelations representing RelationTypes in the
	 * wrapped AbstractONDEXGraph.
	 * 
	 * @return Collection<ONDEXMetaRelation>
	 */
	public Collection<ONDEXMetaRelation> getEdges() {
		return Collections.unmodifiableCollection(edges.keySet());
	}

	/**
	 * Returns the two endpoints of an ONDEXMetaRelation (via fromConcept and
	 * toConcept).
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return Pair<ONDEXMetaConcept>
	 */
	public Pair<ONDEXMetaConcept> getEndpoints(ONDEXMetaRelation edge) {
		return edges.get(edge);
	}

	/**
	 * Returns all ONDEXMetaRelations representing RelationTypes belonging to
	 * the given ONDEXMetaConcept.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<ONDEXMetaRelation>
	 */
	public Collection<ONDEXMetaRelation> getIncidentEdges(ONDEXMetaConcept vertex) {
		Collection<ONDEXMetaRelation> incident = new HashSet<>();
		incident.addAll(getIncoming_internal(vertex));
		incident.addAll(getOutgoing_internal(vertex));
		return incident;
	}

	/**
	 * Internal use, incoming edges.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept
	 * @return Collection<ONDEXMetaRelation>
	 */
	protected Collection<ONDEXMetaRelation> getIncoming_internal(ONDEXMetaConcept vertex) {
		return vertices.get(vertex).first;
	}

	/**
	 * Returns all ONDEXMetaRelations representing RelationTypes that have the
	 * given ONDEXMetaConcept as destination.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<ONDEXMetaRelation>
	 */
	public Collection<ONDEXMetaRelation> getInEdges(ONDEXMetaConcept vertex) {
		return Collections.unmodifiableCollection(getIncoming_internal(vertex));
	}

	/**
	 * Returns all neighbor ONDEXMetaConcepts representing ConceptClasses for
	 * the given ONDEXMetaConcept.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<OndexNode>
	 */
	public Collection<ONDEXMetaConcept> getNeighbors(ONDEXMetaConcept vertex) {
		Collection<ONDEXMetaConcept> neighbors = new HashSet<>();
		for (ONDEXMetaRelation edge : getIncoming_internal(vertex))
			neighbors.add(this.getSource(edge));
		for (ONDEXMetaRelation edge : getOutgoing_internal(vertex))
			neighbors.add(this.getDest(edge));
		return Collections.unmodifiableCollection(neighbors);
	}

	/**
	 * Returns all ONDEXMetaRelations representing RelationTypes that have the
	 * given ONDEXMetaConcept as source.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<ONDEXMetaRelation>
	 */
	public Collection<ONDEXMetaRelation> getOutEdges(ONDEXMetaConcept vertex) {
		return Collections.unmodifiableCollection(getOutgoing_internal(vertex));
	}

	/**
	 * Internal use, outgoing edges.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept
	 * @return Collection<ONDEXMetaRelation>
	 */
	protected Collection<ONDEXMetaRelation> getOutgoing_internal(ONDEXMetaConcept vertex) {
		return vertices.get(vertex).second;
	}

	/**
	 * Returns ONDEXMetaConcepts representing ConceptClasses that are
	 * predecessors (via toConcept) for the given ONDEXMetaConcept.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<ONDEXMetaConcept>
	 */
	public Collection<ONDEXMetaConcept> getPredecessors(ONDEXMetaConcept vertex) {
		Set<ONDEXMetaConcept> preds = new HashSet<>();
		for (ONDEXMetaRelation edge : getIncoming_internal(vertex))
			preds.add(this.getSource(edge));
		return Collections.unmodifiableCollection(preds);
	}

	/**
	 * Returns the source of the edge (via fromConcept) representing
	 * RelationType.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return ONDEXMetaConcept representing ConceptClass
	 */
	public ONDEXMetaConcept getSource(ONDEXMetaRelation edge) {
		return this.getEndpoints(edge).first;
	}

	/**
	 * Returns ONDEXMetaConcepts representing ConceptClasses that are successors
	 * (via fromConcept) for the given ONDEXMetaConcept.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @return Collection<ONDEXMetaConcept>
	 */
	public Collection<ONDEXMetaConcept> getSuccessors(ONDEXMetaConcept vertex) {
		Set<ONDEXMetaConcept> succs = new HashSet<>();
		for (ONDEXMetaRelation edge : getOutgoing_internal(vertex))
			succs.add(this.getDest(edge));
		return Collections.unmodifiableCollection(succs);
	}

	/**
	 * Returns the number of ConceptClasses contained in the wrapped
	 * AbstractONDEXGraph.
	 * 
	 * @return int count of ConceptClasses
	 */
	public int getVertexCount() {
		return vertices.size();
	}

	/**
	 * Returns a list of ONDEXMetaConcepts representing ConceptClasses in the
	 * wrapped AbstractONDEXGraph.
	 * 
	 * @return Collection<ONDEXMetaConcept>
	 */
	public Collection<ONDEXMetaConcept> getVertices() {
		return Collections.unmodifiableCollection(vertices.keySet());
	}

	/**
	 * Returns whether or not a vertex is the destination of an edge.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return is destination
	 */
	public boolean isDest(ONDEXMetaConcept vertex, ONDEXMetaRelation edge) {
		return vertex.equals(this.getEndpoints(edge).second);
	}

	/**
	 * Returns whether or not a vertex is the source of an edge.
	 * 
	 * @param vertex
	 *            ONDEXMetaConcept representing ConceptClass
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return is source
	 */
	public boolean isSource(ONDEXMetaConcept vertex, ONDEXMetaRelation edge) {
		return vertex.equals(this.getEndpoints(edge).first);
	}

	/**
	 * Removes a ChangeListener from the model.
	 * 
	 * @param l
	 *            ChangeListener
	 */
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}

	/**
	 * Removes an RelationType specified by the ONDEXMetaRelation from the
	 * ONDEXMetaGraph.
	 * 
	 * @param edge
	 *            ONDEXMetaRelation representing RelationType
	 * @return false if edge not contained in graph
	 */
	public boolean removeEdge(ONDEXMetaRelation edge) {
		if (!containsEdge(edge))
			return false;

		Pair<ONDEXMetaConcept> endpoints = this.getEndpoints(edge);
		ONDEXMetaConcept source = endpoints.first;
		ONDEXMetaConcept dest = endpoints.second;

		// remove edge from incident vertices' adjacency sets
		getOutgoing_internal(source).remove(edge);
		getIncoming_internal(dest).remove(edge);

		edges.remove(edge);
		return true;
	}

	/**
	 * Removes an ConceptClass specified by the ONDEXMetaConcept from the
	 * ONDEXMetaGraph together with all its RelationTypes.
	 * 
	 * @param vertex
	 *            ONDEXVertex representing ConceptClass
	 * @return false if vertex not contained in graph
	 */
	public boolean removeVertex(ONDEXMetaConcept vertex) {
		if (!containsVertex(vertex))
			return false;

		// copy to avoid concurrent modification in removeEdge
		Set<ONDEXMetaRelation> incident = new HashSet<ONDEXMetaRelation>(getIncoming_internal(vertex));
		incident.addAll(getOutgoing_internal(vertex));

		for (ONDEXMetaRelation edge : incident)
			removeEdge(edge);

		vertices.remove(vertex);

		return true;
	}

	@Override
	public Set<ONDEXMetaConcept> vertexSet() {
		return vertices.keySet();
	}

	@Override
	public ONDEXMetaConcept getEdgeSource(ONDEXMetaRelation ondexMetaRelation) {
		return this.getSource(ondexMetaRelation);
	}

	@Override
	public ONDEXMetaConcept getEdgeTarget(ONDEXMetaRelation ondexMetaRelation) {
		return getDest(ondexMetaRelation);
	}

	@Override
	public GraphType getType() {
		return DefaultGraphType.directedMultigraph();
	}

	@Override
	public double getEdgeWeight(ONDEXMetaRelation ondexMetaRelation) {
		return 0;
	}

	@Override
	public void setEdgeWeight(ONDEXMetaRelation ondexMetaRelation, double weight) {

	}

}