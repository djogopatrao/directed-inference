package br.org.accamargo.cipe.ontofamily;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

public class InstanceOfClass {

	private static final Logger logger = LogManager
			.getLogger("InstanceOfClass");
	private static OWLOntologyManager m;
	private static OWLDataFactory factory;


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 3) {
			logger.error("Arguments: <pellet|jfact|hermit> <class_iri> <ontology.owl>");
			return;
		}
		int i=0;
		String reasoner_option = args[i++];
		String class_iri = args[i++];
		String ontology_in = args[i++];

		m = OWLManager.createOWLOntologyManager();
		factory = m.getOWLDataFactory();

		logger.info("Started loading ontology file {}", ontology_in);
		OWLOntology input = m.loadOntologyFromOntologyDocument(new File(
				ontology_in));
		logger.info("Finished loading ontology file.");

		OWLOntologyID ontology_uri = input.getOntologyID();
		logger.info("Ontology URI: {}", ontology_uri);

		OWLReasoner reasoner = DirectedInference.initReasoner(reasoner_option, input);

		Iterator<OWLNamedIndividual> all_individuals = input.getIndividualsInSignature().iterator();
		
		// "warm up" the reasoner, retrieving all classes
		logger.info("Warming up reasoner.");
		Iterator<OWLClass> all_classes = input.getClassesInSignature().iterator();
		while( all_classes.hasNext() ) {
			OWLClass my_class = all_classes.next();
			Iterator<Node<OWLNamedIndividual>> my_instances = reasoner.getInstances(my_class, false ).iterator();
			while( my_instances.hasNext() ) {
				m.addAxiom(input, factory.getOWLClassAssertionAxiom(my_class, my_instances.next().getRepresentativeElement()));
			}			
		}
		reasoner.flush();
		logger.info("Reasoner is good to go.");
		
		// try for specified classes		
		OWLClass myOwlClass = factory.getOWLClass(IRI.create(class_iri));
		all_classes = input.getClassesInSignature().iterator();
		all_individuals = input.getIndividualsInSignature().iterator();
		while( all_individuals.hasNext() ) {
			OWLNamedIndividual individual = all_individuals.next();
			if ( reasoner.isEntailed( factory.getOWLClassAssertionAxiom(myOwlClass, individual) ) ) {
				System.out.println( "belong: " + individual.getIRI() + " belong to " + myOwlClass.getIRI() );
			} else {
				System.out.println( "DONT  : " + individual.getIRI() + " does not belong to " + myOwlClass.getIRI() );				
			}
		}


	}


	

}
