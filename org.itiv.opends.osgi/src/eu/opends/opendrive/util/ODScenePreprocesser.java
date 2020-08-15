package eu.opends.opendrive.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import  org.jsoup.nodes.Element;

/*
 * This auxiliary class pre-processes a raw scene (.scene) data and remove or modify element tags 
 * having specific type that are error-prone in OpenDS.
 * @input: relative path to .scene File that describes the 3D model in a Driving Task (Assign to variable "file")
 * @output: A processed .scene file. (xml elements with specific tags itself or its parents would be removed from original .scene)
 */
public class ODScenePreprocesser {

	public static void main(String [] arc) throws IOException {

		/*
		 * "file": Path to .scene file to be trimmed.
		 */
		File file = new File("assets\\DrivingTasks\\Projects\\Karlsruhe\\KITTI16\\Scene\\KITTI16.scene");
		FileInputStream fis = new FileInputStream(file);
		Document doc = Jsoup.parse(fis, null, "", Parser.xmlParser()); 

		//remote all 3D road models, surfaceAreas and SurfaceParking
		//Elements railRemover = doc.select("node[name^=Rail]");
		Elements roadRemover = doc.select("node[name^=Road]");
		Elements surfaceAreaRemover = doc.select("node[name^=SurfaceArea]");
		Elements surfaceParkingRemover = doc.select("node[name^=SurfaceParking]");
		//Elements roadJunctionRemover = doc.select("node[name^=RoadJunction]");  
		//Elements roadConnectorRemover = doc.select("node[name^=RoadConnector]");
		/*
		for(Element e: railRemover) {
			e.remove();
		}
		*/
		for(Element e: roadRemover) {
			e.remove();
		}
		for(Element e: surfaceAreaRemover) {
			e.remove();
		}	
		for(Element e: surfaceParkingRemover) {
			e.remove();
		}	


		try (PrintWriter out2 = new PrintWriter("assets\\DrivingTasks\\Projects\\Karlsruhe\\test.scene")) {
		    out2.println(doc.toString());
		}
		fis.close();
		
	}
}
