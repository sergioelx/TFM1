package org.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ontoenrich.tfm.OWLOntologyReuseMetric;
import org.ontoenrich.tfm.OWLOntologyReuse_JSONOuput;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class RunExperiment {

    public static void main(String[] args) throws MalformedURLException, IOException {
        if (args.length != 2) {
            System.out.println("Debe proporcionar dos argumentos: (1) 'url' o 'file' y (2) la URL o la ruta del archivo de la ontología.");
            return;
        }

        String inputType = args[0];
        String inputPath = args[1];

        long inicio = System.currentTimeMillis();

        try {
            // STEP 1: create the IRI of the ontology
            IRI ontologyIri = null;
            if ("url".equalsIgnoreCase(inputType)) {
                ontologyIri = IRI.create(new URL(inputPath));
            } else if ("file".equalsIgnoreCase(inputType)) {
                ontologyIri = IRI.create(new File(inputPath));
            } else {
                System.out.println("Tipo de entrada no válido. Debe ser 'url' o 'file'.");
                return;
            }

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            System.out.println("Hora actual: " + dateFormat.format(date));
            System.out.println();

            // STEP 2: load the ontology
            final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            System.out.println("Loading ontology from " + inputType + ": " + inputPath + "...");

            if (ontologyIri != null) {
                final OWLOntology owlOntology;
                if ("url".equalsIgnoreCase(inputType)) {
                    owlOntology = manager.loadOntologyFromOntologyDocument(ontologyIri);
                } else {
                    owlOntology = manager.loadOntologyFromOntologyDocument(new FileInputStream(new File(inputPath)));
                }

                // STEP 3: create the object and calculate de REUSE metric
                OWLOntologyReuseMetric reuseMetric = new OWLOntologyReuseMetric(owlOntology);
                OWLOntologyReuse_JSONOuput jsonResultObject = reuseMetric.calculateMetric();
                System.out.println(jsonResultObject);

                // PRINT JSON
                File outputFile = new File("output.json"); // Specify the file path here
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(outputFile, jsonResultObject);
                System.out.println("JSON written to: " + outputFile.getAbsolutePath());

            } else {
                System.out.println("Warning: no IRI found.");
            }
        } catch (Exception e) {
            System.out.println("ERROR: excepcion capturada. Mensaje: " + e.getMessage());
        }

        long fin = System.currentTimeMillis();
        double tiempo = (double) ((fin - inicio) / 1000);
        System.out.println(tiempo + " segundos");
        System.out.println();
    }

    @SuppressWarnings("deprecation")
    private static Map<String, URL> getOBOFoundryCorpus() throws MalformedURLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(new URL("https://obofoundry.org/registry/ontologies.jsonld"));
        Iterator<JsonNode> itOntologies = json.get("ontologies").iterator();
        Map<String, URL> result = new HashMap<String, URL>();
        while (itOntologies.hasNext()) {
            JsonNode ontology = itOntologies.next();
            JsonNode ontologyId = ontology.get("id");
            JsonNode ontologyPurl = ontology.get("ontology_purl");
            if (ontologyPurl != null) {
                result.put(ontologyId.textValue(), new URL(ontologyPurl.textValue()));
            } else {
                result.put(ontologyId.textValue(), null);
            }
        }
        return result;
    }
    
    private static String findOntologyTitle(OWLOntology ontology) {
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
            OWLAnnotationProperty property = annotation.getProperty();
            OWLAnnotationValue value = annotation.getValue();

            if (property.getIRI().toString().endsWith("/title")) {
                String valueString = value.toString();
                int idx = valueString.indexOf("^^");
                if (idx != -1) {
                    valueString = valueString.substring(1, idx); // Eliminar las comillas al inicio
                }
                // Eliminar la comilla al final, si existe
                if (valueString.endsWith("\"")) {
                    valueString = valueString.substring(0, valueString.length() - 1);
                }
                return valueString;
            }
        }
        return "Título no encontrado";
    }

}
