package org.ontoenrich.tfm;


import java.util.ArrayList;

import org.ontoenrich.tfm.OWLOntologyReuse_JSONOuput.ImportedMetrics;
import org.ontoenrich.tfm.OWLOntologyReuse_JSONOuput.OntologyMetrics;
import java.io.*;
import java.net.*;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLOntology;

public class OWLOntologyReuseMetric {
	
	private OWLOntology owlOntology = null;
	
	public OWLOntologyReuseMetric (OWLOntology owlOntology ) {
		this.owlOntology = owlOntology;
	}
	


	@SuppressWarnings("deprecation")
    public OWLOntologyReuse_JSONOuput calculateMetric() throws Exception {
        
        String ontologyIRI = owlOntology.getOntologyID().getOntologyIRI().orElseThrow(() -> new IllegalStateException("No se encontró la URI de la ontología")).toString();
        
        // Obtener el título de la ontología
        String ontologyTitle = findOntologyTitle(owlOntology);
        
        
        File onto = descargarOntologia(ontologyIRI);
        
        double weight = onto.length() / (1024.0 * 1024.0);
        
        // Crear un objeto OntologyMetrics con tus métricas
        OntologyMetrics metrics = new OntologyMetrics(owlOntology.getAxiomCount(), owlOntology.getLogicalAxiomCount(),
                owlOntology.getClassesInSignature().size(), owlOntology.objectPropertiesInSignature().count(),
                owlOntology.dataPropertiesInSignature().count(), owlOntology.individualsInSignature().count(),
                owlOntology.annotationPropertiesInSignature().count(), weight);
        
        OWLOntologyReuse_JSONOuput resultMetric = new OWLOntologyReuse_JSONOuput(ontologyIRI, ontologyTitle, metrics,
                new ArrayList<ImportedMetrics>());
        
        // IMPORTS
        ArrayList<OWLOntology> imports = new ArrayList<OWLOntology>();
        owlOntology.imports().forEach(imports::add);
        for (OWLOntology importedOntology : imports) {
            String ontologyImported = importedOntology.getOntologyID().getOntologyIRI()
                    .orElseThrow(() -> new IllegalStateException("No se encontró la URI de la ontología")).toString();
            File onto2 = descargarOntologia(ontologyImported);
            
            double weight2 = onto2.length() / (1024.0 * 1024.0);
            
            resultMetric.getImports().add(new ImportedMetrics(ontologyImported,
                    new OntologyMetrics(importedOntology.getAxiomCount(), importedOntology.getLogicalAxiomCount(),
                            importedOntology.getClassesInSignature().size(),
                            importedOntology.objectPropertiesInSignature().count(),
                            importedOntology.dataPropertiesInSignature().count(),
                            importedOntology.individualsInSignature().count(),
                            importedOntology.annotationPropertiesInSignature().count(), weight2)));
        }
        
        System.out.println();
        
        return resultMetric;
    }
	
	private String findOntologyTitle(OWLOntology ontology) {
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
	
    private File descargarOntologia(String ontologyIRI) throws IOException {
        // Crear una URL con la dirección de la ontología
        URL url = new URL(ontologyIRI);

        // Establecer una conexión con la URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Configurar la conexión para permitir la lectura de datos
        connection.setRequestMethod("GET");

        // Manejar redireccionamiento (código 3xx)
        int responseCode = connection.getResponseCode();
        if (responseCode >= 300 && responseCode < 400) {
            String newLocation = connection.getHeaderField("Location");
            if (newLocation != null) {
                url = new URL(newLocation);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
            }
        }

        // Obtener el stream de entrada para leer los datos del archivo
        InputStream inputStream = connection.getInputStream();

        // Crear un archivo para escribir los datos descargados
        File archivoTemporal = File.createTempFile("ontologia", ".owl");

        // Crear un stream de salida para escribir los datos en el archivo
        FileOutputStream outputStream = new FileOutputStream(archivoTemporal);

        // Leer los datos del stream de entrada y escribirlos en el archivo
        byte[] buffer = new byte[1024];
        int longitud;
        while ((longitud = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, longitud);
        }

        // Cerrar los streams
        outputStream.close();
        inputStream.close();

        return archivoTemporal;
    }
	

    
}
