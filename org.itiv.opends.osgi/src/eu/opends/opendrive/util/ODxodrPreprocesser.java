package eu.opends.opendrive.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/*
 * This auxiliary class pre-processes a raw OpenDRIVE (.xodr) data and remove or modify element tags 
 * having specific type that are error-prone in OpenDS.
 * @input: relative path to .xodr File that describes the road in a Driving Task (Assign to variable "file")
 * @output: A processed .xodr file.
 */
public class ODxodrPreprocesser {

	public static void main(String [] args) throws IOException {
		
		File file = new File("assets\\DrivingTasks\\Projects\\Karlsruhe\\KITTI16\\KITTI16.xodr");
		FileInputStream fis = new FileInputStream(file);
		
		Document doc = Jsoup.parse(fis, null, "", Parser.xmlParser()); 

		/*
		 * Select all elements to be removed, which are those with the tag "lane" having a type of "none" and "tram", 
		 * because in OpenDS these types are not recognized.
		 */
		Elements LaneTypeChanger = doc.select("lane[type^=none]");
		Elements tramLaneDeleter = doc.select("lane[type^=tram]");
	
	
		/*
		 * Turn all tram lanes to driving lane. 
		 */
		for(Element e: LaneTypeChanger) {
			e.attr("type", "driving");
		}
		
		for(Element e: tramLaneDeleter) {
			e.parent().remove();
			//System.out.println(e);
		}
		/*
		Elements NoNameRoadChanger = doc.select("road[name=\"\"]");
		Elements NoNameLaneDeleter = doc.select("lane[name=\"\"]");

		for(Element e: NoNameRoadChanger) {
			e.remove();
			//System.out.println(e);
		}
		for(Element e: NoNameLaneDeleter) {
			e.remove();
			//System.out.println(e);
		}
		*/
		
		try (PrintWriter out2 = new PrintWriter("assets\\DrivingTasks\\Projects\\Karlsruhe\\test.xodr")) {
		    out2.println(doc.toString());
		}
		fis.close();
	}
}
