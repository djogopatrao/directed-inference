package br.org.accamargo.cipe.ontofamily;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindswap.pellet.RBox;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import uk.ac.manchester.cs.jfact.JFactFactory;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class DirectedInference {

	private static final Logger logger = LogManager
			.getLogger("DirectedInference");
	private static OWLOntologyManager m;
	private static OWLDataFactory factory;
	private static boolean flagExtractSameIndividuals;
	private static boolean flagExtractClassIndividuals;
	private static boolean flagExtractObjectProperties;
	private static boolean flagExtractDataProperties;
	private static String instanceRegex = ".*";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 4 && args.length != 6) {
			logger.error("Arguments: <pellet|jfact|hermit> <all|[same|class|object|data],...> [--filter-instances <regexp>] <ontology.owl> <outputinferences.owl> ");
			return;
		}


		try {
			int i = 0;
			String reasoner_option = args[i++];
			parseExtractOption(args[i++]);
			
			if ( args[i].equals("--filter-instances") ) {
				i++;
				instanceRegex  = args[i++];
			};		
			
			String ontology_in = args[i++];
			String output_file = args[i++];

			String new_ontology_uri = "http://inferences/"; // TODO wut
			String definite_ontology_uri = "http://inferences/definitivo/"; // TODO wut
			OWLReasoner reasoner = null;		

			init();

			logger.info("Started loading ontology file {}", ontology_in);
			OWLOntology input = m.loadOntologyFromOntologyDocument(new File(
					ontology_in));
			logger.info("Finished loading ontology file.");

			OWLOntologyID ontology_uri = input.getOntologyID();
			logger.info("Ontology URI: {}", ontology_uri);

			reasoner = initReasoner(reasoner_option, input);
			
			Set<OWLNamedIndividual> all_individuals = filterIndividuals( input
					.getIndividualsInSignature() );
 
			logger.info("Starting inferences extraction");
			logger.info("Starting phase 1...");
			OWLOntology output = extractAll(all_individuals, input, reasoner,
					new_ontology_uri);
			
			logger.info("Ending extraction");

			logger.info("Saving data to file {}", output_file);
			FileOutputStream f = new FileOutputStream(new File(output_file));
			m.saveOntology(output, new StreamDocumentTarget(f));

		} catch (Exception e) {

			logger.error("Exception triggered: {}", e.getMessage());
			logger.error("Stack trace:\n{}", e.getStackTrace().toString());
		}
		logger.info("Finished.");

	}

	private static Set<OWLNamedIndividual> filterIndividuals(
			Set<OWLNamedIndividual> individualsInSignature) {

		Set<OWLNamedIndividual> filter = new HashSet<OWLNamedIndividual>();
		Iterator<OWLNamedIndividual> list = individualsInSignature.iterator();

		logger.info("Starting filtering - " + individualsInSignature.size()  + " instances.");
		logger.info("Applying filter regexp '"+instanceRegex+"'");
		
		while( list.hasNext() ) {
			OWLNamedIndividual i = list.next();
			if ( i.asOWLNamedIndividual().getIRI().toString().matches(instanceRegex) ) {
				filter.add(i);
				logger.debug("Filtering IN:"+i.asOWLNamedIndividual().getIRI().toString());
			} else {
				logger.debug("Filtering OUT:"+i.asOWLNamedIndividual().getIRI().toString());
			}
		}
		logger.info("Ending filtering - " + filter.size()  + " instances.");
	
		return filter;
	}

	private static void parseExtractOption(String argument) throws Exception {
		if ( argument.equals("all") ) {
			flagExtractSameIndividuals=true;
			flagExtractClassIndividuals=true;
			flagExtractObjectProperties=true;
			flagExtractDataProperties=true;
		} else {
			String[] list = argument.split(",");
			for (String arg : list) {
				if( arg.equals("same")) {
					flagExtractSameIndividuals = true;
					flagExtractClassIndividuals= true;
				} else if( arg.equals("class")) {
				} else if( arg.equals("object")) {
					flagExtractObjectProperties = true;
				} else if( arg.equals("data")) {
					flagExtractDataProperties = true;
				} else
					throw new Exception("Invalid argument '"+arg+"'.");
			}
		}
		if( flagExtractSameIndividuals )
			logger.info("Will extract sameAs axioms");
		if( flagExtractClassIndividuals )
			logger.info("Will extract instances of classes");
		if( flagExtractObjectProperties )
			logger.info("Will extract object properties");
		if( flagExtractDataProperties )
			logger.info("Will extract data properties");
	}

	public static void init() {
		m = OWLManager.createOWLOntologyManager();
		factory = m.getOWLDataFactory();
	}

	public static OWLOntology extractAll(
			Set<OWLNamedIndividual> all_individuals, OWLOntology input,
			OWLReasoner reasoner, String new_ontology_uri)
			throws OWLOntologyCreationException {

		OWLOntology output = m.createOntology(IRI.create(new_ontology_uri));
				
		if ( flagExtractSameIndividuals )
			extractSameIndividuals(all_individuals, input, output, reasoner);

		if ( flagExtractClassIndividuals)
			extractClassIndividuals(all_individuals, input, output, reasoner);

		if ( flagExtractObjectProperties )
			extractObjectProperties(all_individuals, input, output, reasoner);

		if ( flagExtractDataProperties )
			extractDataProperties(all_individuals, input, output, reasoner);

		return output;
	}

	public static OWLOntology extractAll2(
			Set<OWLNamedIndividual> all_individuals, OWLOntology input,
			OWLReasoner reasoner, String new_ontology_uri)
			throws OWLOntologyCreationException {

		OWLOntology output = m.createOntology(IRI.create(new_ontology_uri));
				
		if ( flagExtractClassIndividuals)
			extractClassIndividuals(all_individuals, input, output, reasoner);

//		if ( flagExtractObjectProperties )
//			extractObjectProperties(all_individuals, input, output, reasoner);
//
//		if ( flagExtractDataProperties )
//			extractDataProperties(all_individuals, input, output, reasoner);

		return output;
	}

	
	public static OWLReasoner initReasoner(String reasonerName, OWLOntology o)
			throws Exception {
		// individual node policy set BY_NAME allows getInstance to return
		// SAMEAS individuals
		// belonging to one class.
		OWLReasoner reasoner;
		if (reasonerName.equals("pellet")) {
			logger.info("Starting pellet reasoner");
			OWLReasonerConfiguration config = new SimpleConfiguration(
					new NullReasonerProgressMonitor(),
					org.mindswap.pellet.PelletOptions.SILENT_UNDEFINED_ENTITY_HANDLING ? FreshEntityPolicy.ALLOW
							: FreshEntityPolicy.DISALLOW, 0,
					IndividualNodeSetPolicy.BY_NAME); //
			reasoner = PelletReasonerFactory.getInstance().createReasoner(o,
					config);
			logger.info("Finished pellet startup");
		} else if (reasonerName.equals("jfact")) {
			logger.info("Starting jfact reasoner");
			JFactFactory jFactReasonerFactory = new JFactFactory();
			reasoner = jFactReasonerFactory.createReasoner(o);
			logger.info("Finished jfact startup");
		} else if (reasonerName.equals("hermit")) {
			logger.info("Starting hermit reasoner");
			reasoner = new org.semanticweb.HermiT.Reasoner(o);
			logger.info("Finished hermit startup");
		} else {
			throw new Exception("Unrecognized reasoner :" + reasonerName);
		}

		logger.debug("Individual node set policy set:"
				+ reasoner.getIndividualNodeSetPolicy());
		return reasoner;

	}

	private static void extractClassIndividuals(
			Set<OWLNamedIndividual> all_individuals, OWLOntology o,
			OWLOntology ont, OWLReasoner reasoner) {

		logger.info("Starting getClassesInSignature - get classes defined on the ontology");
		Iterator<OWLClass> all_types = o.getClassesInSignature().iterator();
		logger.info("Ending getClassesInSignature");

		logger.info("Starting getInstancesByClass - getting instances for each class");
		while (all_types.hasNext()) {
			OWLClass my_class = all_types.next();
			
			logger.debug("Getting instances for class {} ", my_class.toString());
			Iterator<Node<OWLNamedIndividual>> class_instances = reasoner
					.getInstances(my_class, false).iterator();
			
			while (class_instances.hasNext()) {
				m.applyChange(new AddAxiom(ont, factory
						.getOWLClassAssertionAxiom(my_class, class_instances
								.next().getRepresentativeElement())));
			}
		}
		logger.info("Ending getInstances");

	}

	private static void extractObjectProperties(
			Set<OWLNamedIndividual> all_individuals, OWLOntology o,
			OWLOntology ont, OWLReasoner reasoner) {

		logger.info("Getting all defined object properties");
		Iterator<OWLObjectProperty> properties = o
				.getObjectPropertiesInSignature().iterator();

		logger.info("Starting getInstancesByProperty - Getting instances related per property");
		while (properties.hasNext()) {

			OWLObjectPropertyExpression my_property = properties.next();
			Iterator<OWLNamedIndividual> instances = all_individuals.iterator();

			while (instances.hasNext()) {
				OWLNamedIndividual my_instance = instances.next();
				logger.debug("Getting objects for {} {}",
						my_instance.toString(), my_property.toString());
				Iterator<Node<OWLNamedIndividual>> objects_prop = reasoner
						.getObjectPropertyValues(my_instance, my_property)
						.iterator();

				while (objects_prop.hasNext()) {
					m.applyChange(new AddAxiom(ont, factory
							.getOWLObjectPropertyAssertionAxiom(my_property,
									my_instance, objects_prop.next()
											.getRepresentativeElement())));
				}
			}

		}
		logger.info("Ending getInstancesByProperty");

	}

	private static void extractSameIndividuals(
			Set<OWLNamedIndividual> all_individuals, OWLOntology o,
			OWLOntology ont, OWLReasoner reasoner) {

		logger.info("Starting sameAs - Getting SameAs relation.");
		Iterator<OWLNamedIndividual> instances = all_individuals.iterator();
		while (instances.hasNext()) {
			OWLNamedIndividual my_instance = instances.next();
			logger.debug("Extracting SameAs for instance {}.", my_instance);
			Set<OWLNamedIndividual> same_individuals = reasoner
					.getSameIndividuals(my_instance).getEntities();
			m.applyChange(new AddAxiom(ont, factory
					.getOWLSameIndividualAxiom(same_individuals)));
		}
		logger.info("Ending sameAs");

	}

	private static void extractDataProperties(
			Set<OWLNamedIndividual> all_individuals, OWLOntology o,
			OWLOntology ont, OWLReasoner reasoner) {

		logger.info("Getting all defined data properties");
		Iterator<OWLDataProperty> properties = o.getDataPropertiesInSignature()
				.iterator();

		logger.info("Starting extraction of data properties per instance");
		while (properties.hasNext()) {

			OWLDataProperty my_property = properties.next();
			Iterator<OWLNamedIndividual> instances = all_individuals.iterator();

			while (instances.hasNext()) {
				OWLNamedIndividual my_instance = instances.next();
				logger.debug("Getting data properties for {} {}",
						my_instance.toString(), my_property.toString());
				Iterator<OWLLiteral> objects_prop = reasoner
						.getDataPropertyValues(my_instance, my_property)
						.iterator();

				while (objects_prop.hasNext()) {
					m.applyChange(new AddAxiom(ont, factory
							.getOWLDataPropertyAssertionAxiom(my_property,
									my_instance, objects_prop.next())));
				}
			}

		}
		logger.info("Ending extraction of data properties per instance");
	}

}
