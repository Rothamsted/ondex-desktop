package net.sourceforge.ondex.ovtk2.util.graphml;

import org.jgrapht.Graph;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * TODO: may want to use JGrapht export instead...
 * GraphML writer for JUNG graphs based on yFiles GraphML extensions.
 * 
 * @author taubertj
 * 
 * @param <V>
 * @param <E>
 */
public class GraphMLWriter<V, E> {

	// wrapped JUNG graph
	Graph<V, E> graph = null;

	// whether or not graph is directed
	boolean directed;

	// factory to get XMLStreamWriter from
	XMLOutputFactory factory = null;

	// transformer for node IDs
	Function<V, String> vertex_ids;

	// transformer for edge IDs
	Function<E, String> edge_ids;

	// transformer for node labels
	Function<V, String> vertex_labels;

	// transformer for edge labels
	Function<E, String> edge_labels;

	// transformer for node fill colours
	Function<V, Paint> vertex_fill;

	// transformer for edge colours
	Function<E, Paint> edge_colours;

	/**
	 * Constructor expects the JUNG graph to export.
	 * 
	 * @param graph
	 *            JUNG graph
	 */
	public GraphMLWriter(Graph<V, E> graph) {
		this.graph = graph;
		this.factory = XMLOutputFactory.newInstance();

		// simple id generator for vertices
		vertex_ids = new Function<>() {

			// cache for IDs
			Map<V, String> map = new HashMap<>();
			int i = 0;

			public String apply(V v) {
				// generate new id
				if (!map.containsKey(v)) {
					map.put(v, "n" + i);
					i++;
				}

				return map.get(v);
			}
		};

		// simple id generator for edges
		edge_ids = new Function<>() {

			// cache for IDs
			Map<E, String> map = new HashMap<>();
			int i = 0;

			public String apply(E e) {
				// generate new id
				if (!map.containsKey(e)) {
					map.put(e, "e" + i);
					i++;
				}

				return map.get(e);
			}
		};

		// default label transformer
		vertex_labels = Object::toString;

		// default label transformer
		edge_labels = Object::toString;

		// default colour nodes
		vertex_fill = v -> Color.ORANGE;

		// default colour edges
		edge_colours = e -> Color.BLACK;

		// decide if graph is directed
		directed = !(graph.getType().isDirected());
	}

	/**
	 * Sets the transformer for node labels to be used in node graphics.
	 * 
	 * @param t
	 *            Transformer<V, String>
	 */
	public void setVertexLabelTransformer(Function<V, String> t) {
		this.vertex_labels = t;
	}

	/**
	 * Sets the transformer for edge labels to be used in edge graphics.
	 * 
	 * @param t
	 *            Transformer<E, String>
	 */
	public void setEdgeLabelTransformer(Function<E, String> t) {
		this.edge_labels = t;
	}

	/**
	 * Sets the transformer for node fill colour to be used in node graphics.
	 * 
	 * @param t
	 *            Transformer<V, Paint>
	 */
	public void setVertexFillTransformer(Function<V, Paint> t) {
		this.vertex_fill = t;
	}

	/**
	 * Sets the transformer for edge colour to be used in edge graphics.
	 * 
	 * @param t
	 *            Transformer<E, Paint>
	 */
	public void setEdgeColourTransformer(Function<E, Paint> t) {
		this.edge_colours = t;
	}

	/**
	 * Writer to be used for saving GraphML to.
	 * 
	 * @param writer
	 *            Writer
	 * @throws FactoryConfigurationError
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public void save(Writer writer) throws XMLStreamException, FactoryConfigurationError, IOException {

		// new XML stream around writer
		XMLStreamWriter xml = factory.createXMLStreamWriter(writer);

		// XML first definition line
		xml.writeStartDocument();

		// this is the document root element
		xml.writeStartElement("graphml");

		// adds name spaces definitions
		writeNamespaces(xml);

		// define data keys to be used for graphics
		writeDataKeys(xml);

		// starts writing whole graph
		writeGraph(xml);

		// closing tag required?
		xml.writeEndDocument();

		// properly writer
		writer.flush();
		writer.close();
	}

	/**
	 * Writes the graph element and decides the default edge style.
	 * 
	 * @param xml
	 *            XMlStreamWriter
	 * @throws XMLStreamException
	 */
	private void writeGraph(XMLStreamWriter xml) throws XMLStreamException {

		// write graph element
		xml.writeStartElement("graph");
		xml.writeAttribute("id", "G");
		if (directed)
			xml.writeAttribute("edgedefault", "directed");
		else
			xml.writeAttribute("edgedefault", "undirected");

		// write data for nodes
		for (V v : graph.vertexSet()) {
			writeNode(xml, v);
		}

		// write data for edges
		for (E e : graph.edgeSet()) {
			writeEdge(xml, e);
		}

		// close graph element
		xml.writeEndElement();
	}

	/**
	 * Writes a single edge and corresponding graphics information.
	 * 
	 * @param xml
	 *            XMlStreamWriter
	 * @param e
	 *            edge to write
	 * @throws XMLStreamException
	 */
	private void writeEdge(XMLStreamWriter xml, E e) throws XMLStreamException {

		// get edge id
		String id = edge_ids.apply(e);

		// get nodes connected by edge
//		Collection<V> vertices = graph.
//				graph.getIncidentVertices(e);
//
//		 possible hyper edge support
//		Pair<V> endpoints = new Pair<V>(vertices);
		V v1 = graph.getEdgeSource(e);
		V v2 = graph.getEdgeTarget(e);

		// write edge element
		xml.writeStartElement("edge");
		xml.writeAttribute("id", id);

		// add edge type if doesn't match default
//		EdgeType edge_type = graph.getEdgeType(e);
//		if (directed && edge_type == EdgeType.UNDIRECTED)
//			xml.writeAttribute("directed", "false");
//		if (!directed && edge_type == EdgeType.DIRECTED)
//			xml.writeAttribute("directed", "true");

		// write source and target of edge
		xml.writeAttribute("source", vertex_ids.apply(v1));
		xml.writeAttribute("target", vertex_ids.apply(v2));

		// write graphics of edge
		writeEdgeData(xml, e);

		// close edge element
		xml.writeEndElement();
	}

	/**
	 * Proprietary yFiles data element for edge containing graphics
	 * specifications.
	 * 
	 * @param xml
	 *            XMLStreamWriter
	 * @param e
	 *            edge to construct data for
	 * @throws XMLStreamException
	 */
	private void writeEdgeData(XMLStreamWriter xml, E e) throws XMLStreamException {

		// get colour for edge
		Color c = (Color) edge_colours.apply(e);
		String hex = Integer.toHexString(c.getRGB() & 0x00ffffff);

		// write data element
		xml.writeStartElement("data");

		// that's the key for edge graphics
		xml.writeAttribute("key", "d1");

		// write PolyLineEdge element
		xml.writeStartElement("y:PolyLineEdge");

		// write LineStyle for edge
		xml.writeStartElement("y:LineStyle");
		xml.writeAttribute("type", "line");
		xml.writeAttribute("width", "1.0");
		xml.writeAttribute("color", "#" + hex);
		xml.writeEndElement();

		// write Arrows for edge
		xml.writeStartElement("y:Arrows");
		xml.writeAttribute("source", "none");
		xml.writeAttribute("target", "standard");
		xml.writeEndElement();

		// write EdgeLabel for edge
		xml.writeStartElement("y:EdgeLabel");
		xml.writeCharacters(edge_labels.apply(e));
		xml.writeEndElement();

		// write BendStyle for edge
		xml.writeStartElement("y:BendStyle");
		xml.writeAttribute("smoothed", "false");
		xml.writeEndElement();

		// close PolyLineEdge element
		xml.writeEndElement();

		// close data element
		xml.writeEndElement();

	}

	/**
	 * Writes a single node and corresponding graphics information.
	 * 
	 * @param xml
	 *            XMLStreamWriter
	 * @param v
	 *            node to write
	 * @throws XMLStreamException
	 */
	private void writeNode(XMLStreamWriter xml, V v) throws XMLStreamException {

		// get node id
		String id = vertex_ids.apply(v);

		// write node element
		xml.writeStartElement("node");
		xml.writeAttribute("id", id);

		// writes graphics of node
		writeNodeData(xml, v);

		// close node element
		xml.writeEndElement();
	}

	/**
	 * Proprietary yFiles data element for node containing graphics
	 * specifications.
	 * 
	 * @param xml
	 *            XMLStreamWriter
	 * @param v
	 *            node to construct data for
	 * @throws XMLStreamException
	 */
	private void writeNodeData(XMLStreamWriter xml, V v) throws XMLStreamException {

		// get colour for node
		Color c = (Color) vertex_fill.apply(v);
		String hex = Integer.toHexString(c.getRGB() & 0x00ffffff);

		// write data element
		xml.writeStartElement("data");

		// that's the key for node graphics
		xml.writeAttribute("key", "d0");

		// write ShapeNode element
		xml.writeStartElement("y:ShapeNode");

		// write Fill for node
		xml.writeStartElement("y:Fill");
		xml.writeAttribute("color", "#" + hex);
		xml.writeAttribute("transparent", "false");
		xml.writeEndElement();

		// write NodeLabel for node
		xml.writeStartElement("y:NodeLabel");
		xml.writeCharacters(vertex_labels.apply(v));
		xml.writeEndElement();

		// close ShapeNode element
		xml.writeEndElement();

		// close data element
		xml.writeEndElement();
	}

	/**
	 * Defines the two proprietary data keys required for capturing graphics
	 * information in yFiles.
	 * 
	 * @param xml
	 *            XMLStreamWriter
	 * @throws XMLStreamException
	 */
	private void writeDataKeys(XMLStreamWriter xml) throws XMLStreamException {

		// first element is for nodes
		xml.writeStartElement("key");
		xml.writeAttribute("id", "d0");
		xml.writeAttribute("for", "node");
		xml.writeAttribute("yfiles.type", "nodegraphics");
		xml.writeEndElement();

		// second element is for edges
		xml.writeStartElement("key");
		xml.writeAttribute("id", "d1");
		xml.writeAttribute("for", "edge");
		xml.writeAttribute("yfiles.type", "edgegraphics");
		xml.writeEndElement();

	}

	/**
	 * Necessary name spaces used for GraphML and proprietary extensions.
	 * 
	 * @param xml
	 *            XMLStreamWriter
	 * @throws XMLStreamException
	 */
	private void writeNamespaces(XMLStreamWriter xml) throws XMLStreamException {
		// default name space is GraphML
		xml.writeDefaultNamespace("http://graphml.graphdrawing.org/xmlns");
		xml.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		// this is a bit strange, but works
		xml.writeAttribute("xsi:schemaLocation", "http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd");
		// proprietary extensions of yFiles
		xml.writeNamespace("y", "http://www.yworks.com/xml/graphml");
	}
}
