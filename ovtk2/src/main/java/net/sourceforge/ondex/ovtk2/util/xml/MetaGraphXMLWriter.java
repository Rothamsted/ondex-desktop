package net.sourceforge.ondex.ovtk2.util.xml;

import java.awt.geom.Point2D;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamWriter2;

import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaConcept;
import net.sourceforge.ondex.ovtk2.metagraph.ONDEXMetaRelation;
import net.sourceforge.ondex.ovtk2.ui.OVTK2MetaGraph;
import org.jungrapht.visualization.layout.model.LayoutModel;
import org.jungrapht.visualization.layout.model.Point;

/**
 * Turns MetaGraph visual attributes into XML.
 * 
 * @author taubertj
 * 
 */
public class MetaGraphXMLWriter {

	public static final String FONTSIZE = "fontsize";

	public static final String EDGESIZE = "edgesize";

	public static final String NODESIZE = "nodesize";

	public static final String EDGELABELS = "edgelabels";

	public static final String NODELABELS = "nodelabels";

	public static final String ID = "id";

	public static final String LAYOUT = "layout";

	public static final String POSITION = "position";

	public static final String X = "x";

	public static final String Y = "y";

	/**
	 * Writes attributes of a given OVTK2MetaGraph to XML
	 * 
	 * @param xmlw
	 * @param meta
	 * @throws XMLStreamException
	 */
	public static void write(XMLStreamWriter2 xmlw, OVTK2MetaGraph meta) throws XMLStreamException {

		xmlw.writeStartElement(LAYOUT);

		// write layout coordinates
		LayoutModel<ONDEXMetaConcept> layout = meta.getViewer().getMetaGraphPanel().getVisualizationViewer().getVisualizationModel().getLayoutModel();
		for (ONDEXMetaConcept mc : layout.getGraph().vertexSet()) {
			Point p = layout.apply(mc);
			xmlw.writeStartElement(POSITION);
			xmlw.writeAttribute(ID, mc.getConceptClass().getId());
			xmlw.writeStartElement(X);
			xmlw.writeDouble(p.x);
			xmlw.writeEndElement();
			xmlw.writeStartElement(Y);
			xmlw.writeDouble(p.y);
			xmlw.writeEndElement();
			xmlw.writeEndElement();
		}

		// write node label visible
		xmlw.writeStartElement(NODELABELS);
		xmlw.writeBoolean(meta.isShowNodeLabels());
		xmlw.writeEndElement();

		// write edge label visible
		xmlw.writeStartElement(EDGELABELS);
		xmlw.writeBoolean(meta.isShowEdgeLabels());
		xmlw.writeEndElement();

		// write node size
		xmlw.writeStartElement(NODESIZE);
		xmlw.writeInt(meta.getNodeSize());
		xmlw.writeEndElement();

		// write edge size
		xmlw.writeStartElement(EDGESIZE);
		xmlw.writeInt(meta.getEdgeSize());
		xmlw.writeEndElement();

		// write font size
		xmlw.writeStartElement(FONTSIZE);
		xmlw.writeInt(meta.getFontSize());
		xmlw.writeEndElement();

		xmlw.writeEndElement();
	}

}
