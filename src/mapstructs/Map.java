package mapstructs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import annexFunctions.arrayFunctions;
import annexFunctions.fileFunctions;
import annexFunctions.stringFunctions;
import net.sf.javaml.clustering.mcl.MarkovClustering;
import net.sf.javaml.clustering.mcl.SparseMatrix;


public class Map {
	
	final static int MARGIN = 7;
	final static int CHAR_WIDTH = 6;
	
	private int radius = 0;
	
	private ArrayList<Cluster> clusters;
	private ArrayList<ClusterConnection> clConnections;
	private ArrayList<Concept> concepts;
	private ArrayList<Connection> connections;
	
	private Concept topicCnc = null;
	
	/**
	 * 	Constructs a Map object given you already have all the elements built. 
	 * 	Almost never useful
	 */
	public Map(ArrayList<Cluster> clusters, 
			   ArrayList<ClusterConnection> clConnections, 
			   ArrayList<Concept> concepts,
			   ArrayList<Connection> connections) {
		super();
		this.clusters = clusters;
		this.clConnections = clConnections;
		this.concepts = concepts;
		this.connections = connections;
	}
	
	/**
	 * 	Constructs a Map object given a folder of .cxl files
	 * 
	 * 	@param	folder	the pathname of the folder of .cxl files
	 * 	@throws	IOException	if the pathname doesn't exist or isn't a folder
	 */
	public Map(File directoryPath) throws IOException {
		this.concepts = new ArrayList<Concept>();
		this.connections = new ArrayList<Connection>();
		this.clusters = new ArrayList<Cluster>();
		this.clConnections = new ArrayList<ClusterConnection>();
		
		Path fileName; String cxl; String cncString; String cnnString;
		
		String[] contents = directoryPath.list();
		
		for(String file : contents) {
			System.out.println(file);
			if(file.endsWith(".cxl")) {
				fileName = Path.of(directoryPath + "/" + file);
				cxl = Files.readString(fileName);
				
				cncString = fileFunctions.cncStrip(cxl);
				cnnString = fileFunctions.cnnStrip(cxl);
				
				this.cncReader(cncString);
				this.cnnReader(cnnString);
			}
		}
	}
	
	/**
	 * Execute the collective concept map process
	 * @param output	File to place the finished .cxl file
	 * @param pGamma	Inflation value for Markov Clustering
	 * @throws IOException
	 */
	public void execute(File output, double pGamma) throws IOException {
		
		this.cluBuilder(pGamma);
		
		this.clcBuilder();
		
		this.positionCalculator();
		
		String guts = this.mapCxl()+"\n"+
					  this.cluCxl()+"\n"+this.clcCxl()+"\n"+
					  this.cluCxlStyle()+"\n"+this.clcCxlStyle();
		
		File template = new File("Template.cxl");
		
		fileFunctions.writeCollectiveMap(template, output, guts);
	}
	
	/**
	 * Tests if a topic is in the map's concept list
	 * @param topic	topic to test
	 * @return		True if the topic is in the list, false otherwise
	 */
	public boolean inMap(String topic) {
		for(Concept cnc : concepts)
			if(cnc.getName().equalsIgnoreCase(topic)) {
				topicCnc = cnc;
				return true;
			}
		return false;
	}
	
	/**
	 * Turns string of .cxl concepts into Concept objects and adds them to concepts
	 * @param concepts	string of .cxl concepts, created by cncStrip()
	 */
	private void cncReader(String concepts) {
		String id; String name; String[] names;
		
		while(concepts.indexOf("<concept") >= 0) {
			concepts = concepts.substring(concepts.indexOf("<concept")+8);
			id = concepts.substring(concepts.indexOf("id=")+4, concepts.indexOf("\" "));
			name = concepts.substring(concepts.indexOf("label=")+7, concepts.indexOf("\"/"));
			names = stringFunctions.splitCnc(name);
			if(!Concept.includes(this.concepts, name, id))
				this.concepts.add(new Concept(name, id));
		}
	}
	
	/**
	 * Turns string of .cxl connections into Connection objects and adds them to connections.
	 * Must have already ran cncReader() or have a filled concepts object to work
	 * @param connections	string of .cxl connections, created by cnnStrip()
	 */
	private void cnnReader(String connections) {
		String id; String from; String to;
		Concept fromCnc; Concept toCnc;
		
		while(connections.indexOf("<connection") >= 0) {
			connections = connections.substring(connections.indexOf("<connection")+11);
			id = connections.substring(connections.indexOf("id=")+4, connections.indexOf("\" f"));
			from = connections.substring(connections.indexOf("from-id=")+9, 
										 connections.indexOf("\" t"));
			to = connections.substring(connections.indexOf("to-id=")+7, 
									   connections.indexOf("\"/"));
			fromCnc = this.concepts.get(Concept.indexOf(this.concepts, from));
			toCnc = this.concepts.get(Concept.indexOf(this.concepts, to));
			if(!Connection.includes(this.connections, fromCnc, toCnc))
				this.connections.add(new Connection(fromCnc, toCnc, id));
		}
	}

	/**
	 * Clusters concepts based on popularity algorithm
	 * Asks user for concept map topic and starting number of nodes
	 */
	private void cluBuilder(double pGamma) {
		
		double[][] cncGrid = this.cncGrid();
		
		MarkovClustering mcl = new MarkovClustering();
		SparseMatrix matrix = new SparseMatrix(cncGrid);
		matrix = mcl.run(matrix, 0.01, pGamma, 1, 0.01);
		
		cncGrid = matrix.getDense();
		
		cncGrid = arrayFunctions.transposeMatrix(cncGrid);
		
		Cluster temp = null;
		for(int i = 0; i<cncGrid.length; i++)
		{
			temp = null;
			for(int j = 0; j<cncGrid[i].length; j++)
				if(cncGrid[i][j] > 0.1) {
					if(temp == null) {
						temp = new Cluster(concepts.get(i));
						clusters.add(temp);
						if(i!=j)
							temp.addConcept(concepts.get(j));
					}
					else if(i!=j)
						temp.addConcept(concepts.get(j));		
				}
		}
					
		
		
	}
	
	/**
	 * Makes a grid of integers, each box records the count of connections 
	 * between the x and y elements
	 * @return	2-dimensional integer array
	 */
	private double[][] cncGrid() {
		double[][] grid = new double[concepts.size()][concepts.size()];
		for(int i = 0; i<grid.length; i++) {
			if(concepts.get(i).equals(topicCnc))
				continue;
			for(int j = 0; j<grid[i].length; j++) {
				if(concepts.get(j).equals(topicCnc))
					continue;
				for(Connection cnn : connections)
					if((cnn.getFrom().equals(concepts.get(i)) && cnn.getTo().equals(concepts.get(j))) ||
					   (cnn.getTo().equals(concepts.get(i)) && cnn.getFrom().equals(concepts.get(j)))) {
						grid[i][j] = cnn.getCount();
						break;
					}
			}
		}
		
		return grid;
	}
	
	/**
	 * Fills clConnections and each Clusters' internalConnections.
	 * Must have filled clusters object to work
	 */
	private void clcBuilder() {
		Cluster a; Cluster b;
		for(Connection cnc : connections) {
			a = cnc.getFrom().getCluster(); b = cnc.getTo().getCluster();
			if(a.equals(b))
				a.addInternalConnection(cnc);
			else
				if(!ClusterConnection.includes(clConnections, cnc))
					this.clConnections.add(new ClusterConnection(cnc,"id"));
		}
		
		for(ClusterConnection clc : clConnections) {
			clc.getFrom().incrementEdgeCount(clc.getCount());
			clc.getTo().incrementEdgeCount(clc.getCount());
		}
	}
	
	/**
	 * Calculates the radius of the map for the .cxl file
	 */
	private void positionCalculator() {
		int longest = 0;
		for(Cluster clu : clusters)
			if(clu.getName().length() > longest)
				longest = clu.getName().length();
		
		radius = ((Map.MARGIN*2 + Map.CHAR_WIDTH*longest)*clusters.size())/4;
		
		if(radius<50)
			radius = 50;
	}
	
	/**
	 * Formats the .cxl map line
	 */
	private String mapCxl() {
		return "    <map width=\""+radius*3+"\" height=\""+radius*2.5+"\">";
	}
	
	/**
	 * Formats the .cxl concept block
	 */
	private String cluCxl() {
		String combine = "\t<concept-list>";
		for(Cluster clu : clusters)
			combine = combine.concat("\n\t    "+clu.toCxl());
		return combine.concat("\n\t</concept-list>");
	}
	
	/**
	 * Formats the .cxl concept appearance block
	 */
	private String cluCxlStyle() {
		String combine = "\t<concept-appearance-list>";
		for(int i = 0; i<clusters.size(); i++)
			combine = combine.concat("\n\t    "+clusters.get(i).toCxlStyle(radius,i,clusters.size()));
		return combine.concat("\n\t</concept-appearance-list>");
	}
	
	/**
	 * Formats the .cxl connection block
	 */
	private String clcCxl() {
		String combine = "\t<connection-list>";
		for(ClusterConnection clc : clConnections)
			combine = combine.concat("\n\t    "+clc.toCxl());
		return combine.concat("\n\t</connection-list>");
	}
	
	/**
	 * Formats the .cxl connection appearance block
	 */
	private String clcCxlStyle() {
		String combine = "\t<connection-appearance-list>";
		for(ClusterConnection clc : clConnections)
			combine = combine.concat("\n\t    "+clc.toCxlStyle());
		return combine.concat("\n\t</connection-appearance-list>");
	}

	/**
	 * @return	ArrayList of Cluster objects
	 */
	public ArrayList<Cluster> getClusters() {
		return clusters;
	}

	/**
	 * @return ArrayList of ClusterConnection objects
	 */
	public ArrayList<ClusterConnection> getClConnections() {
		return clConnections;
	}

	/**
	 * @return ArrayList of Concept objects
	 */
	public ArrayList<Concept> getConcepts() {
		return concepts;
	}

	/**
	 * @return ArrayList of Connection objects
	 */
	public ArrayList<Connection> getConnections() {
		return connections;
	}
	
	/**
	 * @return formated string of clusters
	 */
	public String printClusters() {
		String combined = "";
		for(Cluster clu : clusters)
			combined = combined.concat(clu.toString()).concat("\n");
		
		return combined.trim();
	}
	
	/**
	 * @return formated string of clConnections
	 */
	public String printClConections() {
		String combined = "";
		for(ClusterConnection clc : clConnections)
			combined = combined.concat(clc.getFrom().getName()+"\t"
									   +clc.getTo().getName()+"\t"
									   +clc.getCount()+"\n");
		
		return combined.trim();
	}
	
	/**
	 * @return formated string of concepts
	 */
	public String printConcepts() {
		String combined = "";
		for(Concept cnc : concepts)
			combined = combined.concat(cnc.getName()+"\t"+cnc.getCount()+"\n");
		
		return combined.trim();
	}
	
	/**
	 * @return formated string of connections
	 */
	public String printConnections() {
		String combined = "";
		for(Connection cnn : connections)
			combined = combined.concat(cnn.getFrom().getName()+"\t"
									   +cnn.getTo().getName()+"\t"
									   +cnn.getCount()+"\n");
		
		return combined.trim();
	}

}
