package net.sourceforge.ondex.ovtk2.graph;

import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EntityFactory;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.exception.type.NullValueException;

import java.util.Collection;
import java.util.Set;

public class ONDEXJGraphtGraph extends JGraphtGraphAdapter {

    /**
     * wrapped ONDEXGraph
     */
    private final ONDEXGraph og;

    public ONDEXJGraphtGraph(ONDEXGraph graph) {
        this.og = graph;
    }

    @Override
    public ONDEXConcept createConcept(String pid, String annotation, String description, DataSource elementOf, ConceptClass ofType, Collection<EvidenceType> evidence) throws NullValueException, UnsupportedOperationException {
        return og.createConcept(pid, annotation, description, elementOf, ofType, evidence);
    }

    @Override
    public ONDEXRelation createRelation(ONDEXConcept fromConcept, ONDEXConcept toConcept, RelationType ofType, Collection<EvidenceType> evidence) throws NullValueException, UnsupportedOperationException {
        return og.createRelation(fromConcept, toConcept, ofType, evidence);
    }

    @Override
    public boolean deleteConcept(int id) throws UnsupportedOperationException {
        return og.deleteConcept(id);
    }

    @Override
    public boolean deleteRelation(int id) throws UnsupportedOperationException {
        return og.deleteRelation(id);
    }

    @Override
    public boolean deleteRelation(ONDEXConcept fromConcept, ONDEXConcept toConcept, RelationType ofType) throws NullValueException, UnsupportedOperationException {
        return og.deleteRelation(fromConcept, toConcept, ofType);
    }

    @Override
    public Set<ONDEXConcept> getAllTags() {
        return og.getAllTags();
    }

    @Override
    public ONDEXConcept getConcept(int id) {
        return og.getConcept(id);
    }

    @Override
    public Set<ONDEXConcept> getConcepts() {
        return og.getConcepts();
    }

    @Override
    public Set<ONDEXConcept> getConceptsOfAttributeName(AttributeName attributeName) throws NullValueException {
        return og.getConceptsOfAttributeName(attributeName);
    }

    @Override
    public Set<ONDEXConcept> getConceptsOfConceptClass(ConceptClass conceptClass) throws NullValueException {
        return og.getConceptsOfConceptClass(conceptClass);
    }

    @Override
    public Set<ONDEXConcept> getConceptsOfDataSource(DataSource dataSource) throws NullValueException {
        return og.getConceptsOfDataSource(dataSource);
    }

    @Override
    public Set<ONDEXConcept> getConceptsOfEvidenceType(EvidenceType evidenceType) throws NullValueException {
        return og.getConceptsOfEvidenceType(evidenceType);
    }

    @Override
    public Set<ONDEXConcept> getConceptsOfTag(ONDEXConcept concept) throws NullValueException {
        return og.getConceptsOfTag(concept);
    }

    @Override
    public EntityFactory getFactory() {
        return og.getFactory();
    }

    @Override
    public ONDEXGraphMetaData getMetaData() {
        return og.getMetaData();
    }

    @Override
    public String getName() {
        return og.getName();
    }

    @Override
    public ONDEXRelation getRelation(int id) {
        return og.getRelation(id);
    }

    @Override
    public ONDEXRelation getRelation(ONDEXConcept fromConcept, ONDEXConcept toConcept, RelationType ofType) throws NullValueException {
        return og.getRelation(fromConcept, toConcept, ofType);
    }

    @Override
    public Set<ONDEXRelation> getRelations() {
        return og.getRelations();
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfAttributeName(AttributeName attributeName) throws NullValueException {
        return og.getRelationsOfAttributeName(attributeName);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfConcept(ONDEXConcept concept) throws NullValueException {
        return og.getRelationsOfConcept(concept);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfConceptClass(ConceptClass conceptClass) throws NullValueException {
        return og.getRelationsOfConceptClass(conceptClass);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfDataSource(DataSource dataSource) throws NullValueException {
        return og.getRelationsOfDataSource(dataSource);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfEvidenceType(EvidenceType evidenceType) throws NullValueException {
        return og.getRelationsOfEvidenceType(evidenceType);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfRelationType(RelationType relationType) throws NullValueException {
        return og.getRelationsOfRelationType(relationType);
    }

    @Override
    public Set<ONDEXRelation> getRelationsOfTag(ONDEXConcept concept) throws NullValueException {
        return og.getRelationsOfTag(concept);
    }

    @Override
    public boolean isReadOnly() {
        return og.isReadOnly();
    }

    @Override
    public long getSID() {
        return og.getSID();
    }
}
