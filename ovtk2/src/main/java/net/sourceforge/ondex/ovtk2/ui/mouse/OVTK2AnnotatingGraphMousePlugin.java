package net.sourceforge.ondex.ovtk2.ui.mouse;

import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.annotations.AnnotatingGraphMousePlugin;
import org.jungrapht.visualization.annotations.AnnotationManager;

/**
 * Delegating class to gain access to protected data structures.
 * 
 * @author taubertj
 * 
 */
public class OVTK2AnnotatingGraphMousePlugin extends AnnotatingGraphMousePlugin<ONDEXConcept, ONDEXRelation> {

	public OVTK2AnnotatingGraphMousePlugin(RenderContext<ONDEXConcept, ONDEXRelation> rc) {
		super(rc);
	}

	/**
	 * Makes the current annotation manager accessible to the rest of the
	 * system.
	 * 
	 * @return AnnotationManager
	 */
	public AnnotationManager getAnnotationMananger() {
		return this.annotationManager;
	}

}
