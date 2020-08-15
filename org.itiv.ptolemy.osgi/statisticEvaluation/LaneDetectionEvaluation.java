package statisticEvaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import laneDetection.LaneLine;
import laneDetection.LaneLine.WrongParameterException;
import ptolemy.actor.TypedAtomicActor;


public class LaneDetectionEvaluation extends TypedAtomicActor{
	ArrayList <Frames> frameList;
	private String file;
	public static final double [] REVERSED_WINDOW_WIDTH = new double[] {350, 176.85, 111.8, 77.5, 59, 47.3, 39.3, 33.4, 28.9};	//
	public static final double [] REVERSED_WINDOW_HEIGHT = new double[] {102, 47, 26, 18, 12, 8, 7 ,6 ,4};	//
	public static double [] TOLERANCE_RADIUS = new double[] {Math.sqrt(350*102/Math.PI), Math.sqrt(176.85*47/Math.PI), Math.sqrt(111.8*26/Math.PI)
			, Math.sqrt(77.5*18/Math.PI), Math.sqrt(59*12/Math.PI), Math.sqrt(47.3*8/Math.PI), Math.sqrt(39.3*7/Math.PI), Math.sqrt(33.4*6/Math.PI)
			, Math.sqrt(28.9*4/Math.PI)};
	
	int accumulatedTP;
	int accumulatedFP;
	int accumulatedFN;
	int frameNumber;
	
	public LaneDetectionEvaluation(String f) {
		this.file = f;
		accumulatedTP = 0;
		accumulatedFP = 0;
		accumulatedFN = 0;
		frameNumber = 0;
	}
	
	public void initialize(int rows, Mat src) {
		ArrayList <String> listOfLines = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String temp;
			listOfLines = new ArrayList<String>();
			while((temp = br.readLine())!= null){
				listOfLines.add(temp);
			}
		}catch(IOException i) {}
		
		frameList = new ArrayList<>();	
		int ptsInLine = 0;
		for(int i = 0 ; i <listOfLines.size(); i++) {
			List <String> tempstr = new ArrayList<String>();
			if(i==0) {
				ptsInLine = Integer.parseInt(listOfLines.get(0));	//denote number of points in each lane
			}else {
				String [] tmpUntrimmed =  listOfLines.get(i).split(",");
				String [] tmpTrimmed = new String [tmpUntrimmed.length];
				for (int t = 0 ; t < tmpUntrimmed.length; t++) {
					tmpTrimmed[t] = tmpUntrimmed[t].split("\\.")[0];
					//System.out.println("S = "+tmpTrimmed[t]);
				}
				tempstr = Arrays.asList(tmpTrimmed);
				
				setLaneDescription(ptsInLine, tempstr, i-1, rows, src);
			}
			System.out.println();
		}
	}
	
	private void setLaneDescription(int numLanePoints, List <String> tempstr, int frameNumber, int rows, Mat src) {
		Mat dst = src;
		//ArrayList <LaneLine> ls = new ArrayList<>();
		frameList.add(new Frames(new ArrayList<LaneLine>()));
		if(2 == numLanePoints) {
			for(int j = 0 ; j <tempstr.size(); j+=4) {	//4 = 2 * 2 (x,y)*(two points per lane)
				Point n1 = new Point (Integer.parseInt(tempstr.get(j)), Integer.parseInt(tempstr.get(j+1))-rows-10 );
				Point n2 = new Point (Integer.parseInt(tempstr.get(j+2)), Integer.parseInt(tempstr.get(j+3))-rows-10 );
				//ls.add(new LaneLine(n1, n2));
				frameList.get(frameNumber).laneList.add(new LaneLine(n1, n2));
				//System.out.print("1n2= "+n1.x+","+n1.y+"["+n2.x+","+n2.y);
			}
		}else if(5 == numLanePoints) {
			//if no ground truth for this frame exists
			if(tempstr.size() < 10) {
				return;
			}
			for(int j = 0 ; j <tempstr.size(); j+=10) {	//10 = 5 * 2 (x,y)*(two points per lane)
				Point n1 = new Point (Integer.parseInt(tempstr.get(j)), Integer.parseInt(tempstr.get(j+1))-rows-10);
				Point n2 = new Point (Integer.parseInt(tempstr.get(j+2)), Integer.parseInt(tempstr.get(j+3))-rows-10);
				Point n3 = new Point (Integer.parseInt(tempstr.get(j+4)), Integer.parseInt(tempstr.get(j+5))-rows-10);
				Point n4 = new Point (Integer.parseInt(tempstr.get(j+6)), Integer.parseInt(tempstr.get(j+7))-rows-10);
				Point n5 = new Point (Integer.parseInt(tempstr.get(j+8)), Integer.parseInt(tempstr.get(j+9))-rows-10);
				ArrayList<Point> ll = new ArrayList<Point>();
				ll.add(n1);
				ll.add(n2);
				ll.add(n3);
				ll.add(n4);
				ll.add(n5);
				/*
				for(Point p: ll) {
					Imgproc.circle (
							dst,                 //Matrix obj of the image
					         p,    //Center of the circle
					         2,                    //Radius
					         new Scalar(25, 2, 255),  //Scalar object for color
					         5                      //Thickness of the circle
					 );	 
				}
				*/
				//HighGui.imshow("dststst", dst);
				//HighGui.waitKey();
				//sort by point.y
				ll.sort(new Comparator<Point>()
				{
				    @Override
				    public int compare(Point o1, Point o2)
				    {
				        return (new Double(o1.y).compareTo(o2.y));
				    }
				});
	
				try {
					frameList.get(frameNumber).laneList.add(new LaneLine(ll));
				} catch (WrongParameterException e) {
					e.printStackTrace();
				}
				
			}
			
		}else {
			System.err.println("Unknown size of point list for a lane!");
			System.exit(0);
		}
	}
	
	public List <LaneLine> evaluate(List <LaneLine> realLanesRaw, List <LaneLine> detectedLanes) {
		int [] detectedPointAssigned;		// To which real lanes are individual detected points assigned?
		int [] realLaneAssigned;			// Accumulated assigned detected points for each real lanes.

		List <LaneLine> realLanes = new ArrayList<LaneLine>();
		
		if(realLanesRaw.size() != 2)
			return new ArrayList<LaneLine>();

		/*
		 *  special cases: (if no lanes detected; if fewer/more number of detected lanes than real lanes; 
		 */
		if(0 == detectedLanes.size()) {}
		else if(detectedLanes.size() < realLanesRaw.size()) {
			for(int i = 0 ; i < realLanesRaw.size(); i++) {
				realLanes.add(realLanesRaw.get(i).generateRepresentativePoints(detectedLanes.get(0)));
			}			
		}else {
			for(int i = 0 ; i < realLanesRaw.size(); i++) {
				realLanes.add(realLanesRaw.get(i).generateRepresentativePoints(detectedLanes.get(i)));
			}			
		}
		
		/*
		 * Calculation
		 */
		for(int j = 0 ; j < detectedLanes.size(); j++) {
			LaneLine detectedL = detectedLanes.get(j);
			detectedPointAssigned = new int [detectedL.size()];
			realLaneAssigned = new int[realLanes.size()];
			
			for(int i = 0 ; i<detectedPointAssigned.length; i++) {
				detectedPointAssigned[i] = -1;
			}
			
			for(Point p : detectedL.getPoints()) {
				for(int i = 0; i < realLanes.size(); i++) {
					if(arePointsNear(p, realLanes.get(i).getPoints().get(detectedL.getPoints().indexOf(p)), detectedL.getPoints().indexOf(p))){
						detectedPointAssigned[detectedL.getPoints().indexOf(p)] = i;
						break;
					}
				}
			}
			
			for(int i : detectedPointAssigned) {
				if(-1 != i) {
					realLaneAssigned[i] += 1;
					//System.out.println("One vote for lane "+ i);					
				}
			}
			
			int max = 0;
			int maxIndex = -1;
			for(int i=0; i<realLaneAssigned.length; i++) {
				if (realLaneAssigned[i] >= max) {
					maxIndex = i;
					max = realLaneAssigned[i];
				}
			}
			if(max >= 5 && !realLanes.get(maxIndex).isAssigned()) {
				System.out.println("Detection "+ j +" is assigned to lane "+ maxIndex+" with "+max+" votes.");
				realLanes.get(maxIndex).assign();
				detectedLanes.get(j).assign();
			}else if(max>=3) {
				detectedLanes.remove(detectedLanes.get(j));
			}
		}
		
		/*
		 * Summary:
		 * For all detected real lanes, they are assigned to true positive.
		 * For all unassigned real lanes, they contribute to the false negative.
		 * For all unassigned detected lanes, they contribute to false positive.
		 */
		for(LaneLine ll : realLanes) {
			if(ll.isAssigned()) {
				accumulatedTP++;
			}else
				accumulatedFN++;
		}
		for(LaneLine ll : detectedLanes) {
			if(ll.isAssigned()) {}
			else
				accumulatedFP++;
		}
		
		frameNumber++;
	
		System.out.println("Accumulated [FrameNumber,TP,FP,FN]= "+frameNumber+","+accumulatedTP+","+accumulatedFP+","+accumulatedFN);
		return realLanes;
	}
	
	private boolean arePointsNear(Point p1, Point p2, int index) {
		boolean isNear = false;
		if(Math.sqrt( (Math.pow((p1.x-p2.x), 2)) + (Math.pow((p1.y-p2.y), 2)) ) <= TOLERANCE_RADIUS[index]){
			isNear = true;
		}
		//System.out.println(p1+" vs "+p2 +"are close enough? "+isNear);
		return isNear;
	}
	
	/*
	 * Draw ground truth (green), FP detected lane markings (white) and TP detected points (blue)
	 */
	public Mat drawLaneComparison(Mat src, Mat invPers,  List <LaneLine> realLanes, List <LaneLine> detectedLanes) {
		Mat dst = src; 
		
		for(int j = 0 ; j < detectedLanes.size(); j++) {
			LaneLine detectedL = detectedLanes.get(j);
			for(Point p : detectedL.getPoints()) {
				for(int i = 0; i < realLanes.size(); i++) {
					Imgproc.circle (
							dst,                 //Matrix obj of the image
							realLanes.get(i).getPoints().get(detectedL.getPoints().indexOf(p)),    //Center of the circle
					         2,                    //Radius
					         new Scalar(5, 252, 5),  //Scalar object for color
					         3                      //Thickness of the circle
					 );	 		
					if(arePointsNear(p, realLanes.get(i).getPoints().get(detectedL.getPoints().indexOf(p)), detectedL.getPoints().indexOf(p))){
						Imgproc.circle (
								dst,                 //Matrix obj of the image
						         p,    //Center of the circle
						         2,                    //Radius
						         new Scalar(255, 25, 5),  //Scalar object for color
						         5                      //Thickness of the circle
						 );	 					 
						break;	
					}else {
						Imgproc.circle (
								dst,                 //Matrix obj of the image
						         p,    //Center of the circle
						         2,                    //Radius
						         new Scalar(255, 255, 255),  //Scalar object for color
						         5                      //Thickness of the circle
						 );	 
					}
				}
			}
		}
		//HighGui.imshow("dds", dst);
		//HighGui.waitKey();
		return dst;
	}
	
	/*
	 * 
	 */
	public ArrayList<LaneLine> getLanesInFrame(int frameNumber, List <LaneLine> detectedLanes, Mat src) {
		ArrayList <LaneLine> dst = new ArrayList<LaneLine>();
		Frames tmpFrmList = frameList.get(frameNumber); 
		
		for(LaneLine ll : tmpFrmList.laneList) {
			/*for(Point p: ll.getPoints()) {
				Imgproc.circle (
						src,                 	//Matrix obj of the image
						p, 						//Center of the circle
				         2,                    //Radius
				         new Scalar(5, 252, 5),  //Scalar object for color
				         3                      //Thickness of the circle
				 );	
			}*/
			dst.add(ll.generateRepresentativePoints());
		}
		
		return dst;
	}
	
	
	private class Frames{
		ArrayList <LaneLine> laneList;
		Frames(){
		}
		Frames(ArrayList <LaneLine> ll){
			laneList = ll;	
		}
		public void appendLane (ArrayList <LaneLine> ll) {
			laneList.addAll(ll);
		}
	}
}
