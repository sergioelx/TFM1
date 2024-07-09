package org.ontoenrich.tfm;

import java.util.List;

public class OWLOntologyReuse_JSONOuput {

    private String idOntology;
    private String ontologyTitle;
    private OntologyMetrics ontologyMetrics;
    private List<ImportedMetrics> imports;

    public OWLOntologyReuse_JSONOuput(String idOntology, String ontologyTitle, OntologyMetrics ontologyMetrics, List<ImportedMetrics> imports) {
        this.idOntology = idOntology;
        this.ontologyTitle = ontologyTitle;
        this.ontologyMetrics = ontologyMetrics;
        this.imports = imports;
    }
    
    
    // Getter for idOntology
    public String getIdOntology() {
        return idOntology;
    }
    
	public void setIdOntology(String idOntology) {
		this.idOntology = idOntology;
	}

    
    // Getter for imports
    public List<ImportedMetrics> getImports() {
        return imports;
    }
    
	public void setImports(List<ImportedMetrics> imports) {
		this.imports = imports;
	}

	public OntologyMetrics getOntologyMetrics() {
		return ontologyMetrics;
	}


	public void setOntologyMetrics(OntologyMetrics ontologyMetrics) {
		this.ontologyMetrics = ontologyMetrics;
	}



	public String getOntologyTitle() {
		return ontologyTitle;
	}


	public void setOntologyTitle(String ontologyTitle) {
		this.ontologyTitle = ontologyTitle;
	}



	public static class OntologyMetrics {
	    private int axiomCount;
	    private int declarationAxiomCount;
	    private int classCount;
	    private long objectPropertyCount;
	    private long dataPropertyCount;
	    private long individualCount;
	    private long annotationPropertyCount;
	    private double weight;

		public OntologyMetrics(int axiomCount, int declarationAxiomCount, int classCount,
				long objectPropertyCount, long dataPropertyCount, long individualCount,
				long annotationPropertyCount, double weight) {
			
			this.axiomCount = axiomCount;
			this.declarationAxiomCount = declarationAxiomCount;
			this.classCount = classCount;
			this.objectPropertyCount = objectPropertyCount;
			this.dataPropertyCount = dataPropertyCount;
			this.individualCount = individualCount;
			this.annotationPropertyCount = annotationPropertyCount;
			this.weight = weight;
}
	    
	    public int getAxiomCount() {
			return axiomCount;
		}

		public void setAxiomCount(int axiomCount) {
			this.axiomCount = axiomCount;
		}

		public int getDeclarationAxiomCount() {
			return declarationAxiomCount;
		}

		public void setDeclarationAxiomCount(int declarationAxiomCount) {
			this.declarationAxiomCount = declarationAxiomCount;
		}

		public long getClassCount() {
			return classCount;
		}

		public void setClassCount(int classCount) {
			this.classCount = classCount;
		}

		public long getObjectPropertyCount() {
			return objectPropertyCount;
		}

		public void setObjectPropertyCount(int objectPropertyCount) {
			this.objectPropertyCount = objectPropertyCount;
		}

		public long getDataPropertyCount() {
			return dataPropertyCount;
		}

		public void setDataPropertyCount(int dataPropertyCount) {
			this.dataPropertyCount = dataPropertyCount;
		}

		public long getIndividualCount() {
			return individualCount;
		}

		public void setIndividualCount(int individualCount) {
			this.individualCount = individualCount;
		}

		public long getAnnotationPropertyCount() {
			return annotationPropertyCount;
		}

		public void setAnnotationPropertyCount(int annotationPropertyCount) {
			this.annotationPropertyCount = annotationPropertyCount;
		}

		public double getWeight() {
			return weight;
		}

		public void setWeight(long weight) {
			this.weight = weight;
		}


	}

	public static class ImportedMetrics {
		private String idOntology;
	    private OntologyMetrics ontologyMetrics;
	    
	      
		public ImportedMetrics(String idOntology, OntologyMetrics ontologyMetrics) {
			this.idOntology = idOntology;
			this.ontologyMetrics = ontologyMetrics;
		}
		
		
		public String getIdOntology() {
			return idOntology;
		}
		public void setIdOntology(String idOntology) {
			this.idOntology = idOntology;
		}
		public OntologyMetrics getOntologyMetrics() {
			return ontologyMetrics;
		}
		public void setOntologyMetrics(OntologyMetrics ontologyMetrics) {
			this.ontologyMetrics = ontologyMetrics;
		}
	}

}
