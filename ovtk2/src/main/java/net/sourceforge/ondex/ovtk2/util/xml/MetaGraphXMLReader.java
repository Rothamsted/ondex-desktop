package net.sourceforge.ondex.ovtk2.util.xml;

import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConcept;
import net.sourceforge.ondex.ovtk2.ui.OVTK2MetaGraph;
import org.codehaus.stax2.XMLStreamReader2;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.awt.geom.Point2D;

/**
 * Translates XML settings for MetaGraph appearance.
 * 
 * @author taubertj
 * 
 */
public class MetaGraphXMLReader {

	/**
	 * Reads the XML stream, de-serialises settings and sets appearance.
	 * 
	 * @param xmlr
	 * @param meta
	 *            MetaGraph to get modified
	 * @throws XMLStreamException
	 */
	public static void read(XMLStreamReader2 xmlr, OVTK2MetaGraph meta) throws XMLStreamException {

		// layout to modify
		LayoutModel<ONDEXMetaConcept> layout = meta.getViewer().getMetaGraphPanel().getVisualizationViewer().getVisualizationModel().getLayoutModel();


		// iterate over XML
		while (xmlr.hasNext()) {

			// get next event
			int event = xmlr.next();

			// check for start of new element
			if (event == XMLStreamConstants.START_ELEMENT) {

				String element = xmlr.getLocalName();

				// layout position
				if (element.equals(MetaGraphXMLWriter.POSITION)) {
					String id = xmlr.getAttributeValue(0);
					xmlr.nextTag();
					double x = xmlr.getElementAsDouble();
					xmlr.nextTag();
					double y = xmlr.getElementAsDouble();
					Point p = Point.of(x, y);
					for (ONDEXMetaConcept mc : layout.getGraph().vertexSet()) {
						if (mc.getConceptClass().getId().equals(id)) {
							layout.set(mc, p);
							break;
						}
					}
				}

				// node labels visible
				else if (element.equals(MetaGraphXMLWriter.NODELABELS)) {
					meta.showNodeLabels(xmlr.getElementAsBoolean());
				}

				// edge labels visible
				else if (element.equals(MetaGraphXMLWriter.EDGELABELS)) {
					meta.showEdgeLabels(xmlr.getElementAsBoolean());
				}

				// node size
				else if (element.equals(MetaGraphXMLWriter.NODESIZE)) {
					meta.setNodeSize(xmlr.getElementAsInt());
				}

				// edge size
				else if (element.equals(MetaGraphXMLWriter.EDGESIZE)) {
					meta.setEdgeSize(xmlr.getElementAsInt());
				}

				// font size
				else if (element.equals(MetaGraphXMLWriter.FONTSIZE)) {
					meta.setFontSize(xmlr.getElementAsInt());
				}
			}
		}
	};

}
