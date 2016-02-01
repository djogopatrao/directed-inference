package br.org.accamargo.cipe.ontofamily;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.HermiT.blocking.SetFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import br.org.accamargo.cipe.ontofamily.DirectedInference;

public class DirectedInferenceTest {

	@Test
	public void ExtractAllTest() throws Exception {

		// String[] reasoners = { "pellet", "jfact" };

		// initialization (quite lengthy, isn't it)
		ClassLoader classloader = Thread.currentThread()
				.getContextClassLoader();
		IRI ontology_file = IRI.create(classloader.getResource("test.owl"));

		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = m.getOWLDataFactory();
		OWLOntology input = m.loadOntologyFromOntologyDocument(ontology_file);

		DirectedInference.init();

		String r = "pellet";

		OWLReasoner reasoner = DirectedInference.initReasoner(r, input);

		Set<OWLNamedIndividual> all_individuals = input
				.getIndividualsInSignature();

		OWLOntology output = DirectedInference.extractAll(all_individuals,
				input, reasoner, "http://inferences/" + r);

		String prefix = input.getOntologyID().getOntologyIRI().toString() + "#";

		// shortcuts
		OWLClass otherClass = factory.getOWLClass(IRI.create(prefix
				+ "OtherClass"));
		OWLClass parent = factory.getOWLClass(IRI.create(prefix + "Parent"));
		OWLClass subclass = factory
				.getOWLClass(IRI.create(prefix + "Subclass"));
		OWLNamedIndividual i1 = factory.getOWLNamedIndividual(IRI.create(prefix
				+ "i1"));
		OWLNamedIndividual i2 = factory.getOWLNamedIndividual(IRI.create(prefix
				+ "i2"));
		OWLNamedIndividual i3 = factory.getOWLNamedIndividual(IRI.create(prefix
				+ "i3"));

		OWLObjectProperty subprop1 = factory.getOWLObjectProperty(IRI
				.create(prefix + "subprop1"));
		OWLObjectProperty prop1 = factory.getOWLObjectProperty(IRI
				.create(prefix + "prop1"));
		OWLObjectProperty prop2 = factory.getOWLObjectProperty(IRI
				.create(prefix + "prop2"));
		OWLObjectProperty prop3 = factory.getOWLObjectProperty(IRI
				.create(prefix + "prop3"));

		// inference teste: general class axiom
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				otherClass, i1)));

		// inference teste: subclass
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				parent, i1)));
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				parent, i2)));

		// inference teste: subclass2
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				subclass, i1)));
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				subclass, i2)));

		// inference teste: sameAs class (swrl rule datatype property)
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				parent, i3)));
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				subclass, i3)));
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				otherClass, i3)));
		assertTrue(output.containsAxiom(factory.getOWLClassAssertionAxiom(
				subclass, i3)));
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(prop1, i3, i2)));
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(subprop1, i3, i2)));
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(prop2, i2, i3)));

		// inference prop1: subproperty
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(prop1, i1, i2)));

		// inference prop3: swrl rule with object property
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(prop3, i1, i2)));

		// inference prop2: inverse property
		assertTrue(output.containsAxiom(factory
				.getOWLObjectPropertyAssertionAxiom(prop2, i2, i1)));
	}

}
