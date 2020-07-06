package net.sourceforge.ondex.ovtk2.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.map.LazyMap;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXRelation;
import org.jgrapht.Graph;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.AbstractGraph;
import org.jungrapht.visualization.layout.algorithms.util.Pair;

/**
 * Adapter to JUNG graph library adding additional visibility constrains.
 * 
 * @author taubertj
 * 
 */
public abstract class JUNGGraphAdapter extends AbstractGraph<ONDEXConcept, ONDEXRelation> implements Graph<ONDEXConcept, ONDEXRelation>, ONDEXGraph {

	/**
	 * generated
	 */
	private static final long serialVersionUID = -7067410390456773322L;

	/**
	 * show a vertex or not, index by ONDEXConcept id
	 */
	private Map<Integer, Boolean> vertex_visibility = LazyMap.decorate(new HashMap<Integer, Boolean>(), new Factory<Boolean>() {
		@Override
		public Boolean create() {
			return Boolean.FALSE;
		}
	});

	/**
	 * show an edge or not, index by ONDEXRelation id
	 */
	private Map<Integer, Boolean> edge_visibility = LazyMap.decorate(new HashMap<Integer, Boolean>(), new Factory<Boolean>() {
		@Override
		public Boolean create() {
			return Boolean.FALSE;
		}
	});

	/**
	 * keep last visibility state across all nodes and edges
	 */
	private Map<ONDEXEntity, Boolean> lastState = new HashMap<ONDEXEntity, Boolean>();

	/**
	 * Mapping of ONDEXRelation id to visibility (true/false).
	 * 
	 * @return Map<Integer, Boolean>
	 */
	public Map<Integer, Boolean> getEdges_visibility() {
		return edge_visibility;
	}

	/**
	 * Mapping of ONDEXConcept id to visibility (true/false).
	 * 
	 * @return Map<Integer, Boolean>
	 */
	public Map<Integer, Boolean> getVertices_visibility() {
		return vertex_visibility;
	}

	/**
	 * Returns visibility for a given node.
	 * 
	 * @param vertex
	 *            ONDEXConcept
	 * @return is visible
	 */
	public boolean isVisible(ONDEXConcept vertex) {
		if (vertex == null)
			return false;
		return vertex_visibility.get(vertex.getId());
	}

	/**
	 * Returns visibility for a given edge.
	 * 
	 * @param edge
	 *            ONDEXRelation
	 * @return is visible
	 */
	public boolean isVisible(ONDEXRelation edge) {
		if (edge == null)
			return false;
		return edge_visibility.get(edge.getId()) && vertex_visibility.get(edge.getFromConcept().getId()) && vertex_visibility.get(edge.getToConcept().getId());
	}

	/**
	 * Sets everything in the ONDEXGraph visible
	 */
	public void setEverythingVisible() {
		for (ONDEXConcept c : getConcepts()) {
			setVisibility(c, true);
		}
		for (ONDEXRelation r : getRelations()) {
			setVisibility(r, true);
		}
	}

	/**
	 * Sets visibility of a given ONDEXRelation.
	 * 
	 * @param edge
	 *            ONDEXRelation
	 * @param visible
	 *            visibility flag
	 */
	public void setVisibility(ONDEXRelation edge, boolean visible) {
		edge_visibility.put(edge.getId(), visible);

		synchronized (sgListeners) {
			for (SparseONDEXListener sgl : sgListeners) {
				if (visible)
					sgl.edgeShow(edge);
				else
					sgl.edgeHide(edge);
			}
		}
	}

	/**
	 * Convenience method to accept list over vertices or edges.
	 * 
	 * @param entities
	 *            List
	 * @param visible
	 *            visibility flag
	 */
	public void setVisibility(Iterable<? extends ONDEXEntity> entities, boolean visible) {
		for (ONDEXEntity entity : entities) {
			if (entity instanceof ONDEXConcept) {
				setVisibility((ONDEXConcept) entity, visible);
			} else if (entity instanceof ONDEXRelation) {
				setVisibility((ONDEXRelation) entity, visible);
			} else {
				throw new IllegalArgumentException("List contains wrong type of ONDEXEntity.");
			}
		}
	}

	/**
	 * Sets visibility of a given ONDEXConcept.
	 * 
	 * @param vertex
	 *            ONDEXConcept
	 * @param visible
	 *            visibility flag
	 */
	public void setVisibility(ONDEXConcept vertex, boolean visible) {

		// make sure connected edges disappear too
		if (!visible) {
			Collection<ONDEXRelation> incident = getIncidentEdges(vertex);
			if (incident != null) {
				synchronized (sgListeners) {
					for (ONDEXRelation anIncident : incident) {
						edge_visibility.put(anIncident.getId(), visible);

						for (SparseONDEXListener sgl : sgListeners) {
							if (visible)
								sgl.edgeShow(anIncident);
							else
								sgl.edgeHide(anIncident);
						}
					}
				}
			}
		}

		vertex_visibility.put(vertex.getId(), visible);

		synchronized (sgListeners) {
			for (SparseONDEXListener sgl : sgListeners) {
				if (visible)
					sgl.vertexShow(vertex);
				else
					sgl.vertexHide(vertex);
			}
		}
	}

	/**
	 * Checks whether there is a last state.
	 * 
	 * @return last state present?
	 */
	public boolean hasLastState() {
		return lastState.size() > 0;
	}

	/**
	 * Saves current visibility setting as last state.
	 */
	public void updateLastState() {
		lastState.clear();
		for (ONDEXConcept c : getConcepts()) {
			lastState.put(c, isVisible(c));
		}
		for (ONDEXRelation r : getRelations()) {
			lastState.put(r, isVisible(r));
		}
	}

	/**
	 * Restores the last visibility state if there is one.
	 */
	public void restoreLastState() {
		if (lastState.size() > 0) {
			for (ONDEXEntity o : lastState.keySet()) {
				if (o instanceof ONDEXConcept)
					setVisibility((ONDEXConcept) o, lastState.get(o));
				else
					setVisibility((ONDEXRelation) o, lastState.get(o));
			}
		}
	}

	@Override
	public boolean addEdge(ONDEXConcept sourceVertex, ONDEXConcept targetVertex, ONDEXRelation ondexRelation) {
		throw new IllegalArgumentException("Method not supported.");
	}

	@Override
	public boolean addVertex(ONDEXConcept vertex) {
		throw new IllegalArgumentException("Method not supported.");
	}

	@Override
	public boolean containsEdge(ONDEXRelation edge) {
		return getRelation(edge.getId()) != null && isVisible(edge);
	}

	@Override
	public boolean containsVertex(ONDEXConcept vertex) {
		return getConcept(vertex.getId()) != null && isVisible(vertex);
	}

//	@Override
//	public EdgeType getDefaultEdgeType() {
//		return EdgeType.DIRECTED;
//	}

//	@Override
	public ONDEXConcept getDest(ONDEXRelation edge) {
		if (!containsEdge(edge))
			return null;

		return getEndpoints(edge).second;
//				getEndpoints(edge).getSecond();
	}

//	@Override
	public int getEdgeCount() {
		return edgeSet().size();
	}

//	@Override
//	public int getEdgeCount(EdgeType edge_type) {
//		if (edge_type == EdgeType.DIRECTED)
//			return getEdgeCount();
//		else
//			return 0;
//	}

//	@Override
	public Collection<ONDEXRelation> getEdges() {
		Collection<ONDEXRelation> edges = new ArrayList<>();
		for (ONDEXRelation edge : getRelations())
			if (containsEdge(edge))
				edges.add(edge);
		return Collections.unmodifiableCollection(edges);
	}

//	@Override
//	public Collection<ONDEXRelation> getEdges(EdgeType edgeType) {
//		if (edgeType == EdgeType.DIRECTED)
//			return getEdges();
//		else
//			return null;
//	}
//
//	@Override
//	public EdgeType getEdgeType(ONDEXRelation edge) {
//		if (containsEdge(edge))
//			return EdgeType.DIRECTED;
//		else
//			return null;
//	}

//	@Override
	public Pair<ONDEXConcept> getEndpoints(ONDEXRelation edge) {
		if (!containsEdge(edge))
			return null;

		Pair<ONDEXConcept> pair = Pair.of(edge.getFromConcept(), edge.getToConcept());
		return pair;
	}

//	@Override
	public Collection<ONDEXRelation> getIncidentEdges(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXRelation> incident = new HashSet<ONDEXRelation>();
		for (ONDEXRelation edge : getRelationsOfConcept(vertex))
			if (containsEdge(edge))
				incident.add(edge);
		return Collections.unmodifiableCollection(incident);
	}

//	@Override
	public Collection<ONDEXRelation> getInEdges(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXRelation> in = new HashSet<ONDEXRelation>();
		for (ONDEXRelation edge : getRelationsOfConcept(vertex))
			if (containsEdge(edge) && edge.getToConcept().equals(vertex))
				in.add(edge);
		return Collections.unmodifiableCollection(in);
	}

//	@Override
	public Collection<ONDEXConcept> getNeighbors(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXConcept> neighbors = new HashSet<ONDEXConcept>();
		for (ONDEXRelation edge : getInEdges(vertex))
			neighbors.add(getSource(edge));
		for (ONDEXRelation edge : getOutEdges(vertex))
			neighbors.add(getDest(edge));
		return Collections.unmodifiableCollection(neighbors);
	}

//	@Override
	public Collection<ONDEXRelation> getOutEdges(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXRelation> out = new HashSet<>();
		for (ONDEXRelation edge : getRelationsOfConcept(vertex))
			if (containsEdge(edge) && edge.getFromConcept().equals(vertex))
				out.add(edge);
		return Collections.unmodifiableCollection(out);
	}

//	@Override
	public Collection<ONDEXConcept> getPredecessors(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXConcept> preds = new HashSet<>();
		for (ONDEXRelation edge : getInEdges(vertex))
			preds.add(getSource(edge));
		return Collections.unmodifiableCollection(preds);
	}

//	@Override
	public ONDEXConcept getSource(ONDEXRelation edge) {
		if (!containsEdge(edge))
			return null;

		return getEndpoints(edge).first;
	}

//	@Override
	public Collection<ONDEXConcept> getSuccessors(ONDEXConcept vertex) {
		if (!containsVertex(vertex))
			return null;

		Collection<ONDEXConcept> succs = new HashSet<>();
		for (ONDEXRelation edge : getOutEdges(vertex))
			succs.add(getDest(edge));
		return Collections.unmodifiableCollection(succs);
	}

//	@Override
	public int getVertexCount() {
		return getVertices().size();
	}

//	@Override
	public Collection<ONDEXConcept> getVertices() {
		Collection<ONDEXConcept> vertices = new ArrayList<>();
		for (ONDEXConcept vertex : getConcepts())
			if (containsVertex(vertex))
				vertices.add(vertex);
		return Collections.unmodifiableCollection(vertices);
	}

//	@Override
	public boolean isDest(ONDEXConcept vertex, ONDEXRelation edge) {
		if (!containsVertex(vertex) || !containsEdge(edge))
			return false;

		ONDEXConcept dest = getDest(edge);
		if (dest != null)
			return dest.equals(vertex);
		else
			return false;
	}

//	@Override
	public boolean isSource(ONDEXConcept vertex, ONDEXRelation edge) {
		if (!containsVertex(vertex) || !containsEdge(edge))
			return false;

		ONDEXConcept source = getSource(edge);
		if (source != null)
			return source.equals(vertex);
		else
			return false;
	}

	@Override
	public boolean removeEdge(ONDEXRelation edge) {
		throw new IllegalArgumentException("Method not supported.");
	}

	@Override
	public boolean removeVertex(ONDEXConcept vertex) {
		throw new IllegalArgumentException("Method not supported.");
	}

	/**
	 * used for updating counts in the meta graph
	 */
	protected final List<SparseONDEXListener> sgListeners = new ArrayList<SparseONDEXListener>();

	/**
	 * Adds a given listener to the list of listeners.
	 * 
	 * @param sparseONDEXListener
	 */
	public void registerSparseONDEXListener(SparseONDEXListener sparseONDEXListener) {
		synchronized (sgListeners) {
			sgListeners.add(sparseONDEXListener);
		}
	}

	/**
	 * Removes a given listener from the list of listeners.
	 * 
	 * @param sparseONDEXListener
	 */
	public void deregisterSparseONDEXListener(SparseONDEXListener sparseONDEXListener) {
		synchronized (sgListeners) {
			sgListeners.remove(sparseONDEXListener);
		}
	}

	/**
	 * Listener for capturing node and edge visibility events.
	 * 
	 * @author Matthew Pocock
	 * 
	 */
	public interface SparseONDEXListener {

		/**
		 * Fired when a node is made visible.
		 * 
		 * @param vertex
		 *            which ONDEXConcept is visible
		 */
		public void vertexShow(ONDEXConcept vertex);

		/**
		 * Fired when a node is made in-visible.
		 * 
		 * @param vertex
		 *            which ONDEXConcept is in-visible
		 */
		public void vertexHide(ONDEXConcept vertex);

		/**
		 * Fired when an edge is made visible.
		 * 
		 * @param edge
		 *            which ONDEXRelation is visible
		 */
		public void edgeShow(ONDEXRelation edge);

		/**
		 * Fired when an edge is made in-visible
		 * 
		 * @param edge
		 *            which ONDEXRelation is in-visible
		 */
		public void edgeHide(ONDEXRelation edge);
	}

	public ONDEXConcept getOpposite(ONDEXConcept vertex, ONDEXRelation edge)
	{
		Pair<ONDEXConcept> incident = this.getEndpoints(edge);
		ONDEXConcept first = incident.first;
		ONDEXConcept second = incident.second;
		if (vertex.equals(first))
			return second;
		else if (vertex.equals(second))
			return first;
		else
			throw new IllegalArgumentException(vertex + " is not incident to " + edge + " in this graph");
	}
	public ONDEXRelation findEdge(ONDEXConcept v1, ONDEXConcept v2)
	{
		for (ONDEXRelation e : getOutEdges(v1))
		{
			if (getOpposite(v1, e).equals(v2))
				return e;
		}
		return null;
	}


	public Collection<ONDEXRelation> findEdgeSet(ONDEXConcept v1, ONDEXConcept v2)
	{
		if (!getVertices().contains(v1))
			throw new IllegalArgumentException(v1 + " is not an element of this graph");

		if (!getVertices().contains(v2))
			throw new IllegalArgumentException(v2 + " is not an element of this graph");

		Collection<ONDEXRelation> edges = new ArrayList<ONDEXRelation>();
		for (ONDEXRelation e : getOutEdges(v1))
		{
			if (getOpposite(v1, e).equals(v2))
				edges.add(e);
		}
		return Collections.unmodifiableCollection(edges);
	}

}
