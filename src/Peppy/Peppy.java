package Peppy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import Reports.HTMLReporter;
import Reports.SpectrumReport;
import Reports.TextReporter;

/**
 * Peppy
 * Designed with the following goals:
 * 1) More simple code to promote open source development
 * 2) Easy proteogenomic mapping
 * 3) Efficient multi-threading
 * 
 * Initial publication:
 * "Peppy: proteogenomic search software."
 * Risk BA, Spitzer WJ, Giddings MC. Journal of proteome research (April 24, 2013). 
 * PMID: 23614390
 * 
 * Copyright 2013, Brian Risk
 * 
 * 
 * @author Brian Risk
 *
 */
public class Peppy {

	/* track how much memory we have used */
	static MemoryUsage memoryUsage;
	static long maxMemoryUsed = 0;
	static String propertyFileString = null;


	private static boolean verbose = true;


	public static void main(String [] args) {
		/* set up initial state */
		init(args);

		/*  hello! */
		printGreeting();

		/* do the work */
		runJobs(args);
		//		runDirectory("/Volumes/Research/Breast-converted/", "/Volumes/Research/CPTAC-Breast/jobs/first.txt", args);
		//		moonShot(args);
		//		moonShotTest(args);

		/* i'm finished! */
		printFarewell();
	}


	public static void runDirectory(String directoryString, String jobFile, String [] args) {
		init(args);
		init(jobFile);

		File topDir = new File(directoryString);
		File [] directoriesToPotentiallyProcess = topDir.listFiles();

		//listing existing reports so we don't re-run something we've already done
		File [] existingReports = Properties.reportDirectory.listFiles();

		//where we store the list of all directories to process
		ArrayList<File> directories = new ArrayList<File>();

		for(File directory: directoriesToPotentiallyProcess) {
			if (directory.isFile()) continue;
			if (directory.isHidden()) continue;
			boolean existsAsReport = false;
			if (existingReports != null) {
				for (File existingReport: existingReports) {
					if (existingReport.getName().startsWith(directory.getName())) {
						existsAsReport = true;
						break;
					}
				}
			}
			if (existsAsReport) continue;

			//if we have passed all of the above tests, the directory should be processed
			directories.add(directory);

		}

		U.p("Multi-directory jobs");
		U.p("found this many directories: " + directories.size());
		U.p();

		for(File directory: directories) {	
			U.p("running for: " + directory.getName());
			init(args);
			init(jobFile);
			Properties.spectraDirectoryOrFile = directory;
			Properties.spectraDirectoryOrFileList = new ArrayList<File>();
			Properties.spectraDirectoryOrFileList.add(directory);

			/* try running Peppy */
			try {
				runPeppy(null);
			}
			catch (Exception e) {
				U.p("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				e.printStackTrace();
				U.p("\r\r");
				U.p("An error occurred. Continuing with the next job...\r\r");
			}
			U.p();
		}


	}

	/**
	 * Code to automate moon shot analysis
	 * @param args
	 */
	public static void moonShotTest(String [] args) {
		U.p ("Moon Shot");

		//		File drive = new File("/Volumes/New Volume");
		//		File [] experimentFolders = drive.listFiles();
		//		for (File experimentFolder: experimentFolders) {
		//			if (!experimentFolder.getName().startsWith("3_")) continue;
		//			if (!experimentFolder.isHidden() && experimentFolder.isDirectory() && !experimentFolder.getName().startsWith("$")) {
		//				File [] spectrumFiles = experimentFolder.listFiles();
		//				for (File spectrumFile: spectrumFiles) {
		//					File spectrumFile = new File("/Volumes/New Volume/3_IPAS388_H1651/LO2_IP0388_TCE_SG66to68.mgf");
		//
		//
		/* 
		 * this initializes with the default "properties.txt" 
		 * provides a base so common properties don't need to be
		 * redefined
		 */
		init(args);

		//					if (!name.endsWith(".mgf")) continue;
		Properties.spectraDirectoryOrFileList.add(new File("/Volumes/New Volume/3_IPAS388_H1651/LO2_IP0388_TCE_SG69to77.mgf"));
		Properties.spectraDirectoryOrFileList.add(new File("/Volumes/New Volume/3_IPAS388_H1651/LO2_IP0388_TCE_SG66to68.mgf"));

		propertyFileString = "two" + ".txt";
		U.p("running job " + propertyFileString);


		/* 
		 * All properties override those defined in "properties.txt"
		 */
		Properties.smartTolerances = false;
		Properties.precursorTolerance = 14;
		Properties.fragmentTolerance = 500;
		Properties.modC = 57.02146;
		Properties.modK = 6.020129;
		Properties.peptideMassMinimum = 750;
		Properties.peptideMassMaximum = 3800;

		//as we are directly setting modification properties, we need to directly init the acid masses
		AminoAcids.init();

		Properties.sequenceDirectoryOrFileList.add(new File("/Users/risk2/PeppyData/public/sequences/protein/UniProt-HUMAN-20130918.fasta"));
		Properties.isSequenceFileDNAList.add(false);

		try {
			runPeppy(null);
		}
		catch (Exception e) {
			U.p("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			e.printStackTrace();
			U.p("\r\r");
			U.p("An error occurred. Continuing with the next job...\r\r");
		}
		U.p();
		//				}
		//			}
		//		}
	}



	/**
	 * Code to automate moon shot analysis
	 * @param args
	 */
	public static void moonShot(String [] args) {
		String [] compartments = {"tce", "surface", "media", "nuclear"};
		U.p ("Moon Shot");

		File drive = new File("/Volumes/New Volume");
		File [] experimentFolders = drive.listFiles();
		for (File experimentFolder: experimentFolders) {
			if (!experimentFolder.isHidden() && experimentFolder.isDirectory() && !experimentFolder.getName().startsWith("$")) {
				String experimentName = experimentFolder.getName().toLowerCase();
				for(String compartment: compartments) {

					String jobName = experimentName + "_" + compartment;

					//continue if we've already done this analysis
					File existingReportFolder = new File("reports/" + jobName);
					if (existingReportFolder.exists()) continue;

					propertyFileString =  jobName + ".txt";
					U.p("running job " + propertyFileString);

					/* 
					 * this initializes with the default "properties.txt" 
					 * provides a base so common properties don't need to be
					 * redefined
					 */
					init(args);

					// assemble all spectrum files for this compartment;
					File [] spectrumFiles = experimentFolder.listFiles();
					for (File spectrumFile: spectrumFiles) {
						String name = spectrumFile.getName().toLowerCase();
						if (!name.endsWith(".mgf")) continue;
						if (name.indexOf(compartment) != -1) {
							Properties.spectraDirectoryOrFileList.add(spectrumFile);
						}
					}
					if (Properties.spectraDirectoryOrFileList.size() == 0) continue;

					/* 
					 * All properties override those defined in "properties.txt"
					 */
					Properties.smartTolerances = false;
					Properties.precursorTolerance = 14;
					Properties.fragmentTolerance = 500;
					Properties.modC = 57.02146;
					Properties.modK = 6.020129;
					Properties.peptideMassMinimum = 750;
					Properties.peptideMassMaximum = 3800;

					//as we are directly setting modification properties, we need to directly init the acid masses
					AminoAcids.init();

					Properties.sequenceDirectoryOrFileList.add(new File("/Users/risk2/PeppyData/public/sequences/protein/UniProt-HUMAN-20130918.fasta"));
					Properties.isSequenceFileDNAList.add(false);
					//					Properties.sequenceDirectoryOrFileList.add(new File("/Users/risk2/PeppyData/public/sequences/dna/HG19"));
					//					Properties.isSequenceFileDNAList.add(true);
					Properties.sequenceDirectoryOrFileList.add(new File("/Users/risk2/PeppyData/Hanash/sequences/regions/lung28"));
					Properties.isSequenceFileDNAList.add(false);

					try {
						runPeppy(null);
					}
					catch (Exception e) {
						U.p("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						e.printStackTrace();
						U.p("\r\r");
						U.p("An error occurred. Continuing with the next job...\r\r");
					}
					U.p();
				}
			}
		}
	}

	public static void runPeppy(String [] args) {
		/* funnel search is meaningless without an established FDR */
		if (Properties.simpleSearch || Properties.maximumFDR < 0) {
			runSimplePeppy(args);
		} else {
			runFunnelPeppy(args);
		}
	}


	/**
	 * A straight-forward search.  Developed initial for PepArML use.
	 * @param args
	 */
	public static void runSimplePeppy(String [] args) {
		U.startStopwatch();

		/* where we store the report */
		File mainReportDir = createReportDirectory();

		/* Get references to our sequence files -- no nucleotide data is loaded at this point */
		ArrayList<Sequence> sequences = Sequence.loadSequenceFiles(Properties.sequenceDirectoryOrFile);

		/* load spectra */
		ArrayList<Spectrum> spectra = SpectrumLoader.loadSpectra();
		int originalSpectraSize = spectra.size();
		U.p("loaded " + originalSpectraSize + " spectra");

		runSearch(spectra, sequences, mainReportDir);

		U.stopStopwatch();
	}

	/**
	 * Stripped down but does FDR
	 * @param args
	 */
	public static void runBasicPeppy(String [] args) {
		U.startStopwatch();

		/* where we store the report */
		File mainReportDir = createReportDirectory();

		/* Get references to our sequence files -- no nucleotide data is loaded at this point */
		ArrayList<Sequence> sequences = Sequence.loadSequenceFiles(Properties.sequenceDirectoryOrFile);

		/* load spectra */
		ArrayList<Spectrum> spectra = SpectrumLoader.loadSpectra();
		int originalSpectraSize = spectra.size();
		U.p("loaded " + originalSpectraSize + " spectra");

		FDR fdr = new FDR(spectra);
		int numberFound = fdr.getCutoffIndex(0.01);
		double percentFound = (double) numberFound / spectra.size();
		U.p("Found at 1%FDR: " + numberFound + " (" + Properties.percentFormat.format(percentFound) + ")");

		U.stopStopwatch();
	}


	/**
	 * There may be a multiplicity of sequence and spectral directories.
	 * This iterates through them in every combination.
	 * @param args
	 */
	public static void runFunnelPeppy(String [] args) {
		U.startStopwatch();

		if (verbose) U.p("spectral set count: " + Properties.spectraDirectoryOrFileList.size());
		if (verbose) U.p("sequence set count: " + Properties.sequenceDirectoryOrFileList.size());

		/* check if our directories exist (no typos...) */
		boolean fileDoesNotExist = false;
		for (File spectraDirectoryOrFile: Properties.spectraDirectoryOrFileList) {
			if (!spectraDirectoryOrFile.exists()) {
				U.p("ERROR!  This spectrum file does not exist: " + spectraDirectoryOrFile.getAbsolutePath());
				fileDoesNotExist = true;
			}
		}
		for (File sequenceDirectoryOrFile: Properties.sequenceDirectoryOrFileList) {
			if (!sequenceDirectoryOrFile.exists()) {
				U.p("ERROR!  This sequence file does not exist: " + sequenceDirectoryOrFile.getAbsolutePath());
				fileDoesNotExist = true;
			}
		}
		if (fileDoesNotExist) throw new Error("Search file not found");

		try {


			File mainReportDir = createReportDirectory();
			File savedSpectraDir = new File(mainReportDir, "spectra");
			savedSpectraDir.mkdirs();

			/* if there re multiple jobs, the latter varimod settings will persist.
			 * we reset those to normal, modification-less parameters
			 */
			Properties.scoringMethodName = "Peppy.Match_IMP";
			Properties.matchConstructor = new MatchConstructor(Properties.scoringMethodName);
			Properties.searchModifications = false;

			ArrayList<Spectrum> spectra = SpectrumLoader.loadSpectra();
			int originalSpectraSize = spectra.size();
			if (verbose) U.p("loaded " + originalSpectraSize + " spectra.");

			/* create a spectrum report */
			SpectrumReport.spectrumReport(spectra, mainReportDir);

			/* this will maintain our list of score cutoffs */
			PrintWriter metricsReport = new PrintWriter(new FileWriter (new File(mainReportDir, "metrics.txt")));
			metricsReport.println("spectral count: " + originalSpectraSize);
			metricsReport.println();

			/* we are going to combine all returned match sets here */
			ArrayList<Match> allMatchesForSpectralSet = new ArrayList<Match>();

			/* group spectra that have been identified  so that
			 * we can track those that have been identified in a 
			 * previous level */
			Hashtable<Integer, Integer> spectrumIDs = new Hashtable<Integer, Integer>(spectra.size());

			/* iterate through all of our peptide sources */
			for (int sequenceIndex = 0; sequenceIndex < Properties.sequenceDirectoryOrFileList.size(); sequenceIndex++) {

				/* if we are out of spectra (not likely), get out of this loop */
				if (spectra.size() == 0) break;

				/* set up our sequence data.
				 * Setting this property will also affect the FDR calculation */
				Properties.sequenceDirectoryOrFile = Properties.sequenceDirectoryOrFileList.get(sequenceIndex);
				Properties.isSequenceFileDNA = Properties.isSequenceFileDNAList.get(sequenceIndex);
				ArrayList<Sequence> sequences = Sequence.loadSequenceFiles(Properties.sequenceDirectoryOrFile);		


				if (verbose) U.p("performing FDR analysis with " + Properties.sequenceDirectoryOrFile.getName());
				FDR fdr = new FDR(spectra);

				/* special things to do if this is the first sequence */
				if (sequenceIndex == 0 ) {

					/* getting optimal precursor and fragment tolerances */
					OptimalTolerances optimalTolerances = new OptimalTolerances(fdr);
					optimalTolerances.createReport(mainReportDir);


					/* Find optimal tolerance settings if this is the first sequence */
					if (Properties.smartTolerances) {

						/*
						 * abort if we have not found a sufficient percentage
						 */
						if (Properties.maximumFDR >= 0) {
							int numberFound = fdr.getCutoffIndex(Properties.maximumFDR);
							double percentFound = (double) numberFound / spectra.size();
							//								if (percentFound < 0.05) {
							//									U.p("ERROR: only " + Properties.percentFormat.format(percentFound) + " (" + numberFound + ") spectra identified.");
							//									return;
							//								} else {
							//									U.p("initial FDR found " + Properties.percentFormat.format(percentFound));
							//								}
							if (verbose) U.p("initial FDR found " + numberFound + "(" + Properties.percentFormat.format(percentFound) + ")");
						}


						/*
						 * If we found a valid FDR, then adjust parameters and reanalyze
						 */
						if (optimalTolerances.isValid()) {

							Properties.precursorTolerance = optimalTolerances.getOptimalPrecursorError();
							if (verbose) U.p ("optimal precursor tolerance is: " + optimalTolerances.getOptimalPrecursorError());
							metricsReport.println("optimal precursor is: " + optimalTolerances.getOptimalPrecursorError());

							Properties.fragmentTolerance = optimalTolerances.getOptimalFragmentError();
							if (verbose) U.p ("optimal fragment tolerance is: " + optimalTolerances.getOptimalFragmentError());
							metricsReport.println("optimal fragment is: " + optimalTolerances.getOptimalFragmentError());

							Properties.peptideMassMinimum = optimalTolerances.getMinimumMass();
							if (verbose) U.p ("optimal minimum mass is: " + optimalTolerances.getMinimumMass());
							metricsReport.println("optimal minimum mass is: " + optimalTolerances.getMinimumMass());

							Properties.peptideMassMaximum = optimalTolerances.getMaximumMass();
							if (verbose) U.p ("optimal maximum mass is: " + optimalTolerances.getMaximumMass());
							metricsReport.println("optimal maximum mass is: " + optimalTolerances.getMaximumMass());

							Properties.minimumNumberOfPeaksForAValidSpectrum = optimalTolerances.getMinimumNumberOfPeaks();
							if (verbose) U.p ("optimal minimum number of peaks is: " + optimalTolerances.getMinimumNumberOfPeaks());
							metricsReport.println("optimal minimum number of peaks is: " + optimalTolerances.getMinimumNumberOfPeaks());

							if (verbose) U.p("average fragment error: " + optimalTolerances.getMeanFragmentError());
							if (verbose) U.p("average precursor error: " + optimalTolerances.getMeanPrecursorError());


							/* remove spectra that don't conform to optimal parameters */
							boolean remove;
							Spectrum spectrumToExamine;
							for (int spectrumIndex = 0; spectrumIndex < spectra.size(); spectrumIndex++) {
								spectrumToExamine = spectra.get(spectrumIndex);
								remove = false;
								if (spectrumToExamine.getMass() < Properties.peptideMassMinimum) remove = true;
								if (spectrumToExamine.getMass() > Properties.peptideMassMaximum) remove = true;
								if (spectrumToExamine.getPeaks().size() < Properties.minimumNumberOfPeaksForAValidSpectrum) remove = true;
								if (remove) {
									spectra.remove(spectrumIndex);
									spectrumIndex--;
								}
							}

							if (verbose) U.p ("new spectral set size: " + spectra.size());
							metricsReport.println("new spectral set size: " + spectra.size());

							metricsReport.println();

							/* Now that we have different fragment tolerance, we have different coverage */
							for (Spectrum spectrum: spectra) {
								spectrum.recalculateCoverage();
							}

							/* second-pass FDR analysis */
							if (verbose) U.p("performing second FDR with new tolerances...");
							fdr = new FDR(spectra);
						}

					}
				}


				/* Calculate score threshold with FDR. 
				 * maximumFDR may be negative, indicating we won't use FDR to calculate score cutoff */
				if (Properties.maximumFDR >= 0) {
					double potentialMinimumScore = fdr.getScoreThreshold(Properties.maximumFDR);

					/* if we will not find any matches with confidence, skip this round */
					//NOTE:  in the event of "continue" it will produce no report.  Look out for this when assembling reports!
					if (potentialMinimumScore < 0) continue;
					Properties.minimumScore = potentialMinimumScore;
				}

				/* create spectra-based containers for matches */
				ArrayList<MatchesSpectrum> matchesSpectra;
				ArrayList<Match> matches = null;

				/* if we used all of our spectra to calculate FDR, we take advantage of that
				 * and harvest those results for these results.  No need to do the work twice!
				 * 
				 * Note:  decoys are not getting in.  They are removed in the
				 * getMatchesFromSpectraMatches method
				 */
				if (fdr.usedFullSetOfSpectra) {
					matchesSpectra = fdr.getSpectraMatches();
					matches = getMatchesFromSpectraMatches(matchesSpectra);
				} else {
					matchesSpectra = new ArrayList<MatchesSpectrum>(spectra.size());
					for (Spectrum spectrum: spectra) {
						matchesSpectra.add(new MatchesSpectrum(spectrum));
					}
					if (verbose) U.p("getting the matches");
					matches = getMatches(sequences, matchesSpectra);
				}

				/* we found no matches */
				if (matches == null) continue;

				/* if matches are from nucleotide searches, then save the spectra */
				if (Properties.isSequenceFileDNA) {
					for (Match match: matches) {
						match.getSpectrum().saveDTA(savedSpectraDir);
					}
				}

				/* label the identified peptides according to our sequence */
				for (Match match: matches) {
					match.getPeptide().setTrackingIdentifier(sequenceIndex);
				}

				/* add these matches to our large pile of all matches for this spectral set */
				allMatchesForSpectralSet.addAll(matches);

				/* group spectra that have been identified */
				int identifiedSpectraCount = 0;
				for (Match match: matches) {
					if (spectrumIDs.get(match.getSpectrum().getId()) == null) {
						identifiedSpectraCount++;
						spectrumIDs.put(match.getSpectrum().getId(), match.getSpectrum().getId());
					}

				}
				U.p(identifiedSpectraCount + " spectra identified at this step");

				/* create the directory where we will hold this report */
				String reportDirName = sequenceIndex + " " + Properties.sequenceDirectoryOrFile.getName();
				File reportDir = new File (mainReportDir, reportDirName);
				reportDir.mkdirs();



				/* remove all spectra that appear in our matches */
				if (Properties.maximumFDR >= 0) {
					ArrayList<Spectrum> reducedSpectra = new ArrayList<Spectrum>(spectra.size() - spectrumIDs.size());
					for (Spectrum spectrum: spectra) {
						if (spectrumIDs.get(spectrum.getId()) == null) {
							reducedSpectra.add(spectrum);
						}
					}
					spectra = reducedSpectra;
				}


				/* generate reports */
				double precentReduction =  ((double)spectrumIDs.size() / originalSpectraSize);
				U.p("spectra identified " + Properties.percentFormat.format(precentReduction));
				metricsReport.println(reportDirName);
				metricsReport.println("score cutoff: " + Properties.minimumScore);
				metricsReport.println("spectra identified: " + spectrumIDs.size() + " (" +  Properties.percentFormat.format(precentReduction) + ")");
				metricsReport.println();
				metricsReport.flush();
				fdr.saveReport(reportDir);
				createReports(matches, reportDir);

			}

			/* MODIFICATONS
			 * 
			 * Now that we have collected all of our matches from the first pass,
			 * we can collect all the found peptides and perform varible-mod searches on them
			 */

			/* first, we collect all of the peptides found */
			if (Properties.searchModifications) {
				Hashtable<String, Peptide> peptideHash = new Hashtable<String, Peptide>();
				for (Match match: allMatchesForSpectralSet) {
					peptideHash.put(match.getPeptide().getAcidSequenceString(), match.getPeptide());
				}
				ArrayList<Peptide> peptidesFound = new ArrayList<Peptide>(peptideHash.values());

				/* a big IF -- were there any peptides at all identified from above.  We hope so!  */
				if (peptidesFound.size() > 0) {

					Collections.sort(peptidesFound);

					/* set our scoring method to vari-mod */
					Properties.scoringMethodName = "Peppy.Match_IMP_VariMod";
					Properties.matchConstructor = new MatchConstructor(Properties.scoringMethodName);
					Properties.searchModifications = true;

					/* use this peptide database to perform FDR */
					if (verbose) U.p("performing FDR analysis for modificaitons using found peptides");
					FDR fdr = new FDR(spectra, peptidesFound);

					/* Calculate score threshold with FDR. 
					 * maximumFDR may be negative, indicating we won't use FDR to calculate score cutoff */
					if (Properties.maximumFDR >= 0) {
						double potentialMinimumScore = fdr.getScoreThreshold(Properties.maximumFDR);

						/* if we will not find any matches with confidence, skip this round */
						//NOTE:  in the event of "continue" it will produce no report.  Look out for this when assembling reports!
						if (potentialMinimumScore < 0) return;
						Properties.minimumScore = potentialMinimumScore;
					}

					/* create spectra-based containers for matches */
					ArrayList<MatchesSpectrum> matchesSpectra;					
					ArrayList<Match> matches = null;
					if (fdr.usedFullSetOfSpectra) {
						matchesSpectra = fdr.getSpectraMatches();
						matches = getMatchesFromSpectraMatches(matchesSpectra);
					} else {
						matchesSpectra = new ArrayList<MatchesSpectrum>(spectra.size());
						for (Spectrum spectrum: spectra) {
							matchesSpectra.add(new MatchesSpectrum(spectrum));
						}
						if (verbose) U.p("getting the matches");
						matches = getMatchesWithPeptides(peptidesFound, matchesSpectra);
					}

					/*
					 * THIS WAS COPIED DIRECTLY FROM ABOVE
					 */

					/* add these matches to our large pile of all matches for this spectral set */
					allMatchesForSpectralSet.addAll(matches);

					/* group spectra that have been identified */
					int identifiedSpectraCount = 0;
					for (Match match: matches) {
						if (spectrumIDs.get(match.getSpectrum().getId()) == null) {
							identifiedSpectraCount++;
							spectrumIDs.put(match.getSpectrum().getId(), match.getSpectrum().getId());
						}

					}
					U.p(identifiedSpectraCount + " spectra identified at this step");

					/* create the directory where we will hold this report */
					String reportDirName = Properties.sequenceDirectoryOrFileList.size() + " - varimod";
					File reportDir = new File (mainReportDir, reportDirName);
					reportDir.mkdirs();

					/* generate reports */
					double precentReduction =  ((double)spectrumIDs.size() / originalSpectraSize);
					U.p("spectra identified " + Properties.percentFormat.format(precentReduction));
					metricsReport.println(reportDirName);
					metricsReport.println("score cutoff: " + Properties.minimumScore);
					metricsReport.println("spectra identified: " + spectrumIDs.size() + " (" +  Properties.percentFormat.format(precentReduction) + ")");
					metricsReport.println();
					metricsReport.flush();
					fdr.saveReport(reportDir);
					createReports(matches, reportDir);


				} /* end modifications if */
			}

			metricsReport.flush();
			metricsReport.close();




			/* create graphic reports */
			//			BestMatches bm = new BestMatches(mainReportDir, ResultsCategory.DNA, null);
			//			ArrayList<BestMatches> bmArray = new ArrayList<BestMatches>();
			//			bmArray.add(bm);
			//			BestMatches.createUnifiedSamplesReport(bmArray, "peptideSequence", mainReportDir);

		} catch (IOException e) {
			e.printStackTrace();
		}

		U.stopStopwatch();
	}


	/**
	 * A "search" is one set of spectra on one peptide database
	 * @param spectra
	 * @param sequences
	 */
	public static ArrayList<Match> runSearch(ArrayList<Spectrum> spectra, ArrayList<Sequence> sequences, File reportDir) {

		//save our properties
		Properties.generatePropertiesFile(reportDir);


		/* set up where we will hold all of the matches for our spectra */
		ArrayList<MatchesSpectrum> spectraMatches = new ArrayList<MatchesSpectrum>(spectra.size());
		for (Spectrum spectrum: spectra) {
			MatchesSpectrum matchesSpectrum = new MatchesSpectrum(spectrum);

			/* keep only best matches */
			matchesSpectrum.setWhatToKeep(Matches.KEEP_ONLY_BEST_MATCHES);

			spectraMatches.add(matchesSpectrum);
		}

		//initialize our ArrayList of matches
		ArrayList<Match> matches = null;

		if (Properties.useSpliceVariants) {
			//gets the first nucleotide sequence in the first sequence file
			Sequence_DNA sequenceFile = (Sequence_DNA) sequences.get(0);
			RNA_Sequence rna = new RNA_Sequence(sequenceFile, sequenceFile.getNucleotideSequences().get(0), Properties.sequenceRegionStart, Properties.sequenceRegionStop);

			U.p("digesting with splice variants...");
			RNA_Digestor rnaDigestor = new RNA_Digestor(rna);
			ArrayList<Peptide> peptides  = rnaDigestor.getPeptides();
			U.p("generated " + peptides.size() + " peptides");

			U.p("getting matches...");
			//TODO get rid of this getMatches function when this is overhauled
			matches = getMatchesWithPeptides(peptides, spectraMatches);

		} else {	
			if (Properties.useSequenceRegion) {
				U.p("digesting part of sequence");
				ArrayList<Sequence> oneSequenceList = new ArrayList<Sequence>();
				oneSequenceList.add(sequences.get(0));
				sequences = oneSequenceList;
			}

			/* nothing weird.  Just do a normal search */
			matches = getMatches(sequences, spectraMatches);

		}

		createReports(matches, reportDir);

		return matches;

	}




	public static void runJobs(String [] args) {
		/* see if we have jobs in the jobs folder */
		File jobsDir = new File("jobs");
		File[] potentialJobsFiles = jobsDir.listFiles();
		ArrayList<File> jobFiles = new ArrayList<File>();
		if (potentialJobsFiles != null) {
			for (int i = 0; i < potentialJobsFiles.length; i++) {
				if (potentialJobsFiles[i].getName().toLowerCase().endsWith(".txt")) {
					jobFiles.add(potentialJobsFiles[i]);
				}	
			}
		}

		/* run the jobs */
		if (jobFiles.size() == 0) {
			U.p("no jobs in jobs folder.  running according to main properties file");
			runPeppy(null);
		} else {
			U.p();
			U.p("running " + jobFiles.size() + " jobs");
			U.p();
			for (int i = 0; i < jobFiles.size(); i++) {
				U.p("running job " + (i + 1) + "; " + jobFiles.get(i).getName());
				propertyFileString = jobFiles.get(i).getName();

				/* 
				 * this initializes with the default "properties.txt" 
				 * provides a base so common properties don't need to be
				 * redefined
				 */
				init(args);

				/* 
				 * All properties defined in this file override those defined in "properties.txt"
				 */
				init(jobFiles.get(i).getAbsolutePath());

				/* try running Peppy */
				try {
					runPeppy(null);
				}
				catch (Exception e) {
					U.p("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					e.printStackTrace();
					U.p("\r\r");
					U.p("An error occurred. Continuing with the next job...\r\r");
				}
				U.p();
			}
		}
	}



	public static void init(String propertiesFile) {

		/* check to see if this version is too old */
		//		if (Properties.expires && System.currentTimeMillis() > expiration) {
		//			try {
		//				throw (new Exception("unsynchronized resource"));
		//			} catch (Exception e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//			System.exit(0);
		//		}

		/* this allows us to do our graphics */
		System.setProperty("java.awt.headless", "true");

		Properties.loadProperties(propertiesFile);

		/*
		 * AminoAcids needs initializing as some of the properties
		 * may be fixed modification masses
		 */
		AminoAcids.init();

		/* track memory */
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		memoryUsage = mbean.getHeapMemoryUsage();
	}


	public static void init(String [] args) {
		/*
		 * Clears properties from previous job
		 */
		Properties.init();

		if (args.length == 0) {
			/* load in the default file */
			init("properties.txt");
		} else {
			init(args[0]);
			U.p("Initializing with properties file: " + args[0]);
		}
	}


	/**
	 * This is the heart of Peppy where the grand symphony takes place.
	 * 
	 * @param sequences our list of sequences where we will be getting our peptides
	 * @param spectra our list of spectra
	 * @param isReverse if we are doing a normal, forwards search or if this is a null, reverse search
	 * @return
	 */
	private static ArrayList<Match> getMatches(
			ArrayList<Sequence> sequences, 
			ArrayList<MatchesSpectrum> spectraMatches, 
			boolean isReverse) {

		/* track which sequence we are examining */
		int sequenceIndex = 0;

		/* loops until we have gone through all of our sequences */
		while (true) {
			/* check our memory situation */
			if (maxMemoryUsed < memoryUsage.getUsed()) {
				maxMemoryUsed = memoryUsage.getUsed();
			}

			/* Extract a decent size of peptides.  Sequences may be short, so this
			 * goes through each sequence and extracts peptides until a desired
			 * threshold has been been reached.
			 */

			/*  Initialize our list of peptides */
			ArrayList<Peptide> peptides = new ArrayList<Peptide>(Properties.desiredPeptideDatabaseSize);
			peptides.ensureCapacity(Properties.desiredPeptideDatabaseSize);

			/* This is where we get a chunk of peptides */
			ArrayList<Peptide> peptideSegment = new ArrayList<Peptide>();

			/* if we use a region, extract that region, else go through all sequences getting a chunk at a time. */
			if (Properties.useSequenceRegion) {

				/* collect peptides */
				peptideSegment = sequences.get(sequenceIndex).extractMorePeptides(isReverse);
				peptides.addAll(peptideSegment);

				/* reset the sequence so that the next batch of spectra can scan it */
				sequences.get(sequenceIndex).reset();

			} else {
				while (peptides.size() < Properties.desiredPeptideDatabaseSize) {

					/* clear previous chunk of peptides and reclaim memory */
					if (peptideSegment != null) {	
						peptideSegment.clear();
						System.gc();
					}	

					/* collect peptides */
					peptideSegment = sequences.get(sequenceIndex).extractMorePeptides(isReverse);

					/* advance to the next sequence if we don't have any more peptides in this sequence */
					if (peptideSegment == null) {
						sequences.get(sequenceIndex).reset();
						sequenceIndex++;

						/* add peptides to the main list if we have some to add */
					} else {
						peptides.addAll(peptideSegment);
					}

					/* if we have advanced past the last sequence, then exit this loop */
					if (sequenceIndex == sequences.size()) {
						break;
					}

				}
			}


			/* checks to see if we have any peptides in this chunk */
			if (peptides.size() > 0) {

				/* sort our peptide list by mass */
				Collections.sort(peptides);

				/* find the matches for this chunk of peptides */
				(new ScoringServer(peptides, spectraMatches)).findMatches();

				/* free up memory */
				peptides.clear();
				System.gc();

			}

			/* break if we have covered our last sequence or if we are only using a region */
			if (sequenceIndex == sequences.size() || Properties.useSequenceRegion) {
				break;
			}

		}


		return getMatchesFromSpectraMatches(spectraMatches);
	}

	/**
	 * Does a normal search, then does multipass
	 * @param sequences
	 * @param spectra
	 * @return
	 */
	public static ArrayList<Match> getMatches(ArrayList<Sequence> sequences, ArrayList<MatchesSpectrum> spectraMatches) {

		/* performing our normal search */
		ArrayList<Match> matches = getMatches(sequences, spectraMatches, false);

		/* performing the multipass search */
		if (Properties.multipass && Properties.isSequenceFileDNA) {
			matches = multipass(matches, sequences, spectraMatches);
		}
		return matches;
	}

	/**
	 * Gets matches where a list of peptides is already derived
	 * @param peptides
	 * @param spectra
	 * @param sequence_DNA
	 * @return
	 */
	public static ArrayList<Match> getMatchesWithPeptides(
			ArrayList<Peptide> peptides, 
			ArrayList<MatchesSpectrum> spectraMatches) {

		//This is where the bulk of the processing in long jobs takes
		(new ScoringServer(peptides, spectraMatches)).findMatches();

		/* set e values */
		//		for (MatchesSpectrum spectrumMatches: spectraMatches) {
		//			spectrumMatches.calculateEValues();
		//		}

		ArrayList<Match> matches = getMatchesFromSpectraMatches(spectraMatches);

		return matches;
	}


	public static ArrayList<Match> getMatchesFromSpectraMatches(ArrayList<MatchesSpectrum> spectraMatches) {

		/* calculate size of combined matches */
		int size = 0;
		for (MatchesSpectrum spectrumMatches: spectraMatches) {
			/* make sure there are matches for this spectrum */
			if (spectrumMatches.getMatches().size() == 0) continue;

			/* don't add if score is less than minimum allowed. */
			if (spectrumMatches.getMatches().get(0).getScore() < Properties.minimumScore) continue;

			size += spectrumMatches.getMatches().size();
		}

		/* combine matches into result and return */
		ArrayList<Match> out = new ArrayList<Match>(size);
		for (MatchesSpectrum spectrumMatches: spectraMatches) {
			/* make sure there are matches for this spectrum */
			if (spectrumMatches.getMatches().size() == 0) continue;

			/* don't add if score is less than minimum allowed. */
			if (spectrumMatches.getMatches().get(0).getScore() < Properties.minimumScore) continue;

			/* add only the target matches */
			for (Match match: spectrumMatches.getMatches()) {

				/* skip decoy matches */
				if (match.getPeptide().isDecoy()) continue;

				out.add(match);
			}

		}
		return out;
	}



	public static ArrayList<Match> getDecoyMatches(ArrayList<Sequence> sequences, ArrayList<MatchesSpectrum> spectraMatches) {

		/* performing our normal search specifying "true" for isReverse */
		ArrayList<Match> matches = getMatches(sequences, spectraMatches, true);

		/* performing the multipass search */
		if (Properties.multipass && Properties.isSequenceFileDNA) {
			matches = multipass(matches, sequences, spectraMatches);
		}

		return matches;
	}

	public static ArrayList<Match> reduceMatchesToOnePerSpectrum(ArrayList<Match> matches) {
		Hashtable<String, Match> oneMatchPerSpectrum = new Hashtable<String, Match>(matches.size());
		for (Match match: matches) {
			Match existing = oneMatchPerSpectrum.get(match.getSpectrum().getMD5());
			if (existing == null) {
				oneMatchPerSpectrum.put(match.getSpectrum().getMD5(), match);
			} else {
				if (match.getScore() > existing.getScore()) {
					oneMatchPerSpectrum.put(match.getSpectrum().getMD5(), match);
				}
			}
		}
		ArrayList<Match> out = new ArrayList<Match>(oneMatchPerSpectrum.size());
		Enumeration<Match> e = oneMatchPerSpectrum.elements();
		while (e.hasMoreElements()) out.add(e.nextElement());
		return out;
	}



	private static ArrayList<Match> multipass(ArrayList<Match> matches, ArrayList<Sequence> sequences, ArrayList<MatchesSpectrum> spectraMatches) {
		/* regions */
		Regions regions = new Regions(matches, sequences);
		U.p("we found this many regions: " + regions.getRegions().size());

		if (regions.getRegions().size() > 0) {

			U.p("performing localized PTM search");
			Properties.matchConstructor = new MatchConstructor("Peppy.Match_IMP_VariMod");
			Properties.searchModifications = true;

			/* first we need to level the playing field and score the original matches as modification matches */
			//TODO: this can be parallelized
			ArrayList<Match> rescoredMatches = new ArrayList<Match>(matches.size());
			for (Match match: matches) {
				rescoredMatches.add(Properties.matchConstructor.createMatch(match.getSpectrumMatches(), match.getPeptide()));
			}
			matches = rescoredMatches;

			/* generating peptides in the regions */
			ArrayList<Peptide> peptidesInRegions = getPeptidesInRegions(regions.getRegions(), sequences, false);
			U.p("there are this many peptides in the regions: " + peptidesInRegions.size());

			/* getting matches to the peptides in the regions */
			ArrayList<Match> modMatches = getMatchesWithPeptides(peptidesInRegions, spectraMatches);

			/* make room for the modified matches and add them to our main set of matches */
			int matchesNotFoundInUnmodifiedSearchCount = 0;
			for (Match match: modMatches) {
				if (match.isFromModificationSearches()) matchesNotFoundInUnmodifiedSearchCount++;
			}
			matches.ensureCapacity(matches.size() + matchesNotFoundInUnmodifiedSearchCount);
			for (Match match: modMatches) {
				if (match.isFromModificationSearches()) matches.add(match);
			}

			/* return things to the way they were */
			Properties.matchConstructor = new MatchConstructor("Peppy.Match_IMP");
			Properties.searchModifications = false;
		}

		return matches;
	}


	//TODO this could probably be made much, much faster
	private static ArrayList<Peptide> getPeptidesInRegions(ArrayList<Region> regions, ArrayList<Sequence> sequences, boolean isReverse) {
		/* our return */
		ArrayList<Peptide> out = new ArrayList<Peptide>();

		/* determine how many regions we will use to generate our new set of peptides */
		int numberOfRegionsToKeep = Properties.numberOfRegionsToKeep;
		if (numberOfRegionsToKeep > regions.size()) numberOfRegionsToKeep = regions.size();

		/* get peptides along regions */
		Region region;
		for (Sequence sequence: sequences) {
			Sequence_DNA dnaSequence = (Sequence_DNA) sequence;
			for (int regionIndex = 0; regionIndex < numberOfRegionsToKeep; regionIndex++) {
				region = regions.get(regionIndex);
				if (dnaSequence.equals(region.getSequence())) {

					//TODO get rid of this 500
					out.addAll(dnaSequence.extractPeptidesFromRegion(region.getStartLocation() - 200, region.getStopLocation() + 200, isReverse));
				}
			}
			sequence.reset();
		}
		Collections.sort(out);
		return out;
	}

	private static File createReportDirectory() {
		/* create new report directory */
		String reportDirString;
		if (propertyFileString != null) {
			reportDirString = U.getFileNameWithoutSuffix(new File(propertyFileString));
		} else {
			reportDirString = Properties.spectraDirectoryOrFile.getName() + "_" + System.currentTimeMillis();
		}
		File mainReportDir = new File(Properties.reportDirectory, reportDirString);
		mainReportDir.mkdirs();
		return mainReportDir;
	}


	private static void createReports(ArrayList<Match> matches, File reportDir) {
		Properties.generatePropertiesFile(reportDir);
		U.p("creating text reports");
		TextReporter textReport = new TextReporter(matches, reportDir);
		textReport.generateFullReport();

		if (Properties.createHTMLReport) {
			U.p("creating HTML reports");
			HTMLReporter report = new HTMLReporter(matches, reportDir);
			report.generateFullReport();
		}	
	}

	protected static void printGreeting() {
		U.p("Welcome to Peppy(TM)");
		U.p("Protein identification / proteogenomic software.");
		U.p("Copyright 2014 by Brian Risk");
		U.p();

		/* print some system statistics */
		U.p("number of processors available: " + Runtime.getRuntime().availableProcessors());
		U.p("number of threads allowed: " + Properties.numberOfThreads);
		U.p("max available memory: " + (double) memoryUsage.getMax() / (1024 * 1024 * 1024) + " gigabytes");
		U.p();

		//		U.p("This program comes with ABSOLUTELY NO WARRANTY;");

	}

	protected static void printFarewell() {
		U.p("done");
	}



}
