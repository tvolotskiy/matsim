package playground.manserpa.results;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import playground.manserpa.spatialData.CSVUtils;

public class EvolutionOfPaxOpAndRoutes {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 10;
		
		HashMap<Integer, String> ops = new HashMap<Integer, String>();
		HashMap<Integer, String> routes = new HashMap<Integer, String>();
		HashMap<Integer, String> pax = new HashMap<Integer, String>();
		HashMap<Integer, String> vehicles = new HashMap<Integer, String>();
	
		List<List<Double>> opsMean = new ArrayList<List<Double>>();
		List<List<Double>> routesMean = new ArrayList<List<Double>>();
		List<List<Double>> paxMean = new ArrayList<List<Double>>();
		List<List<Double>> vehiclesMean = new ArrayList<List<Double>>();
	    
		String csvFileOps = "EvolutionOfOperators.csv";
	    FileWriter writerOps = new FileWriter(csvFileOps);
	    
	    CSVUtils.writeLine(writerOps, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500"), ';');
	    
	    String csvFileOpsMean = "EvolutionOfOperatorsMean.csv";
	    FileWriter writerOpsMean = new FileWriter(csvFileOpsMean);
	    
	    CSVUtils.writeLine(writerOpsMean, Arrays.asList("Iteration", "Mean"), ';');
	    
	    String csvFileRoutes = "EvolutionOfRoutes.csv";
	    FileWriter writerRoutes = new FileWriter(csvFileRoutes);
	    
	    CSVUtils.writeLine(writerRoutes, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500"), ';');
	    
	    String csvFileRoutesMean = "EvolutionOfRoutesMean.csv";
	    FileWriter writerRoutesMean = new FileWriter(csvFileRoutesMean);
	    
	    CSVUtils.writeLine(writerRoutesMean, Arrays.asList("Iteration", "Mean"), ';');
	    
	    String csvFilePax = "EvolutionOfPax.csv";
	    FileWriter writerPax = new FileWriter(csvFilePax);
	    
	    CSVUtils.writeLine(writerPax, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500"), ';');
	    
	    String csvFilePaxMean = "EvolutionOfPaxMean.csv";
	    FileWriter writerPaxMean = new FileWriter(csvFilePaxMean);
	    
	    CSVUtils.writeLine(writerPaxMean, Arrays.asList("Iteration", "Mean"), ';');
	    
	    String csvFileVehicles = "EvolutionOfVehicles.csv";
	    FileWriter writerVehicles = new FileWriter(csvFileVehicles);
	    
	    CSVUtils.writeLine(writerVehicles, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500"), ';');
	    
	    String csvFileVehiclesMean = "EvolutionOfVehiclesMean.csv";
	    FileWriter writerVehiclesMean = new FileWriter(csvFileVehiclesMean);
	    
	    CSVUtils.writeLine(writerVehiclesMean, Arrays.asList("Iteration", "Mean"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
        	List<Double> opsRun = new ArrayList<>();
        	List<Double> paxRun = new ArrayList<>();
        	List<Double> routesRun = new ArrayList<>();
        	List<Double> vehiclesRun = new ArrayList<>();
	        
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
		        	
		        	int k = 0;
		            while ((line = br.readLine()) != null) {
		            	String[] stats = line.split("\t");
		            	if (k != 0)	{
		            		if (Integer.parseInt(stats[0]) % 50 == 0 && Integer.parseInt(stats[0]) != 0)	{
		            			ops.put(Integer.parseInt(stats[0]), stats[2]);
		            			routes.put(Integer.parseInt(stats[0]), stats[4]);
		            			pax.put(Integer.parseInt(stats[0]), stats[6]);
		            			vehicles.put(Integer.parseInt(stats[0]), stats[8]);
		            		}
		            		
		            		if (Integer.parseInt(stats[0]) % 2 == 0)	{
		            			opsRun.add(Double.parseDouble(stats[2]));
		            			routesRun.add(Double.parseDouble(stats[4]));
		            			paxRun.add(Double.parseDouble(stats[6]));
		            			vehiclesRun.add(Double.parseDouble(stats[8]));
		            		}
		            	}
		            	k++;
		            }
		            
		            opsMean.add(opsRun);
		            routesMean.add(routesRun);
		            paxMean.add(paxRun);
		            vehiclesMean.add(vehiclesRun);

					CSVUtils.writeLine(writerOps, Arrays.asList(ops.get(50), ops.get(100), ops.get(150), ops.get(200), ops.get(250),
							ops.get(300), ops.get(350), ops.get(400), ops.get(450), ops.get(500)), ';');
	
					
					CSVUtils.writeLine(writerRoutes, Arrays.asList(routes.get(50), routes.get(100), routes.get(150), routes.get(200), routes.get(250),
							routes.get(300), routes.get(350), routes.get(400), routes.get(450), routes.get(500)), ';');
	
					
					CSVUtils.writeLine(writerPax, Arrays.asList(pax.get(50), pax.get(100), pax.get(150), pax.get(200), pax.get(250),
							pax.get(300), pax.get(350), pax.get(400), pax.get(450), pax.get(500)), ';');
					
					CSVUtils.writeLine(writerVehicles, Arrays.asList(vehicles.get(50), vehicles.get(100), vehicles.get(150), vehicles.get(200), vehicles.get(250),
							vehicles.get(300), vehicles.get(350), vehicles.get(400), vehicles.get(450), vehicles.get(500)), ';');
				
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ops.clear();
				routes.clear();
				pax.clear();
				vehicles.clear();
	        }
        }
        System.out.println(paxMean.get(0));
        System.out.println(paxMean.get(1));
        System.out.println(paxMean.get(2));
        
        for (int p = 0; p <= 250; p++)	{
        	List<Double> getMeanOps = new ArrayList<Double>();
            List<Double> getMeanRoutes = new ArrayList<Double>();
            List<Double> getMeanPax = new ArrayList<Double>();
            List<Double> getMeanVehicles = new ArrayList<Double>();
        	
        	for(int i = 0; i < numberOfRuns; i++)	{
        		getMeanOps.add(opsMean.get(i).get(p));
        		getMeanRoutes.add(routesMean.get(i).get(p));
        		getMeanPax.add(paxMean.get(i).get(p));
        		getMeanVehicles.add(vehiclesMean.get(i).get(p));
        	}
        	double totalNumberOps = 0;
        	double totalNumberRoutes = 0;
        	double totalNumberPax = 0;
        	double totalNumberVehicles = 0;
        	
        	for (double op : getMeanOps)	{
        		totalNumberOps += op;
        	}
        	for (double op : getMeanRoutes)	{
        		totalNumberRoutes += op;
        	}
        	for (double op : getMeanPax)	{
        		totalNumberPax += op;
        	}
        	for (double op : getMeanVehicles)	{
        		totalNumberVehicles += op;
        	}
        	
        	double totalMeanOps = totalNumberOps / getMeanOps.size();
        	double totalMeanRoutes = totalNumberRoutes / getMeanRoutes.size();
        	double totalMeanPax = totalNumberPax / getMeanPax.size();
        	double totalMeanVehicles = totalNumberVehicles / getMeanVehicles.size();
        	
        	try {
	        	CSVUtils.writeLine(writerOpsMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanOps)), ';');
	        	CSVUtils.writeLine(writerRoutesMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanRoutes)), ';');
	        	CSVUtils.writeLine(writerPaxMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanPax)), ';');
	        	CSVUtils.writeLine(writerVehiclesMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanVehicles)), ';');
			
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        }
		
        writerOpsMean.flush();
        writerOpsMean.close();
        
        writerRoutesMean.flush();
        writerRoutesMean.close();
        
        writerPaxMean.flush();
        writerPaxMean.close();
        
        writerVehiclesMean.flush();
        writerVehiclesMean.close();
		
		writerOps.flush();
		writerOps.close();
		
		writerRoutes.flush();
		writerRoutes.close();
		
		writerPax.flush();
		writerPax.close();
		
		writerVehicles.flush();
		writerVehicles.close();
	}
}
