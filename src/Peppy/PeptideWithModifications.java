package Peppy;

public class PeptideWithModifications extends Peptide {
	
	private double [] modifications;
	
	public PeptideWithModifications(String sequence, double [] modifications) {
		super(sequence);
		this.modifications = modifications;
		for (double mod: modifications) {
			mass += mod;
		}
	}

	public double[] getModifications() {
		return modifications;
	}

}
