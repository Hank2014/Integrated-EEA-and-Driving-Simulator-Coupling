package ptolemy.actor.lib.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.optimization.fitting.WeightedObservedPoint;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.terraswarm.gdp.evbuffer_ptr._internal_struct;

import laneDetection.LaneLine;
import ptolemy.actor.lib.SubMatrix;



public class ImageUtils {
	static float [] histogramArray;
	private static LaneLine egoLanel;
	private static LaneLine egoLaner;
	public static Mat _invIPMMatrice;
	public static Mat _iPMMatrice;
	public static List <LaneLine> totLanes;
	
	/** Convert an AWT Image object to a BufferedImage.
	 *  @param in An AWT Image object.
	 *  @return a BufferedImage.
	 */
	 public static BufferedImage getRenderedImage(Image in) {
	    BufferedImage out = new BufferedImage(in.getWidth(null),
	            in.getHeight(null), BufferedImage.TYPE_INT_RGB);
	    Graphics2D g2 = out.createGraphics();
	    g2.drawImage(in, 0, 0, null);
	    g2.dispose();
	    return out;       
	}
	  
	 /**
	   * Converts/writes a BufferedImage into a Mat.
	   * 
	   * @param BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	   * @return matrix Mat of type CV_8UC3 or CV_8UC1
	   */
	 public static Mat img2Mat(BufferedImage in)
	 {
	  Mat out;
      byte[] data;
      int h = in.getHeight();
      int w = in.getWidth();

      out = new Mat(h, w, CvType.CV_8UC3);
      data = new byte[w * h * (int)out.elemSize()];
      int[] dataBuff = in.getRGB(0, 0, w, h, null, 0, w);
      for(int i = 0; i < dataBuff.length; i++)
      {
      	data[i*3] = (byte) ((dataBuff[i] >> 16));
      	data[i*3 + 1] = (byte) ((dataBuff[i] >> 8));
      	data[i*3 + 2] = (byte) ((dataBuff[i] >> 0));
      }

       out.put(0, 0, data);
       return out;
	 }
	 
	 /**
	   * Converts/writes a Mat into a BufferedImage.
	   * 
	   * @param matrix Mat of type CV_8UC3 or CV_8UC1
	   * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
	   */
	  public static BufferedImage matToBufferedImage(Mat matrix) {
	      int cols = matrix.cols();
	      int rows = matrix.rows();
	      int elemSize = (int)matrix.elemSize();
	      byte[] data = new byte[cols * rows * elemSize];
	      int type;

	      matrix.get(0, 0, data);

	      switch (matrix.channels()) {
	          case 1:
	              type = BufferedImage.TYPE_BYTE_GRAY;
	              break;

	          case 3: 
	              type = BufferedImage.TYPE_3BYTE_BGR;

	              // bgr to rgb
	              byte b;
	              for(int i=0; i<data.length; i=i+3) {
	                  b = data[i];
	                  data[i] = data[i+2];
	                  data[i+2] = b;
	              }
	              break;

	          default:
	              return null;
	      }

	      BufferedImage image = new BufferedImage(cols, rows, type);
	      image.getRaster().setDataElements(0, 0, cols, rows, data);

	      return image;
	  } 	
	
	  /*
	   * first preprocess the image and then perform houghTransform
	   * @param Mat src image
	   * @return Mat dst[2]: dst[0] = src overlapped with founded lines 
	   * 					 and dst[1] = founded lines
	   */
	public static Mat[] houghTransform(Mat src) {
		Mat temp = new Mat(), cdstP = new Mat();	
		Mat [] dst = new Mat[2];

		temp = ROIfilter(preProcessing(src));

		Imgproc.cvtColor(temp, cdstP, Imgproc.COLOR_GRAY2RGB);
		
	    // Probabilistic Line Transform
	    Mat linesP = new Mat(); // will hold the results of the detection
	    Imgproc.HoughLinesP(temp, linesP, 1, Math.PI/180, 50, 10, 50); // runs the actual detection
	   
	    for (int x = 0; x < linesP.rows(); x++) {
	        double[] l = linesP.get(x, 0);
	        Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255,0,255), 2, Imgproc.LINE_AA, 0);
	    }
	    
	    dst[0] = cdstP;
	    dst[1] = linesP;//linesP;
	    HighGui.imshow("HoughTransfrom", cdstP);
	    HighGui.imshow("src", src);

	    return dst;
	}
		
	/*
	 * Modified houghTransform
	 */
	public static Mat[] houghTransform2(Mat src) {
		Mat temp = new Mat(), cdstP = new Mat();	
		Mat [] dst = new Mat[2];

		temp = ROIfilter(preProcessing(src));
		HighGui.imshow("2ROI", temp);
		
		Imgproc.cvtColor(temp, cdstP, Imgproc.COLOR_GRAY2RGB);

	    // Probabilistic Line Transform
	    Mat linesP = new Mat(); // will hold the results of the detection
	    Imgproc.HoughLinesP(temp, linesP, 1, Math.PI/180, 50, 10, 50); // runs the actual detection, threshold=50 for city, 15 for testing on grass
		


	    // Draw the lines
	    ArrayList<LaneLine> al = new ArrayList<LaneLine>();

	    Mat linesFinal = new Mat(linesP.rows(),1,CvType.CV_32SC4,new Scalar(0));
	    for (int x = 0; x < linesP.rows(); x++) {
	        double[] l = linesP.get(x, 0);
	        LaneLine ll = new LaneLine(new Point(l[0],l[1]), new Point(l[2],l[3]));
	        if(!al.contains(ll)) {
	        	al.add(ll);
	        	linesFinal.put(al.size()-1, 0, new double[] {l[0],l[1],l[2],l[3]});
	        }

	    	
	    }
	    for(int x = 0; x < linesFinal.rows(); x++) {
	    	double[] l = linesFinal.get(x, 0);
	        Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255,0,255), 2, Imgproc.LINE_AA, 0);
	    }
	    
	    dst[0] = cdstP;
	    dst[1] = linesFinal;//linesP;

	    return dst;
	}
	
	
	
	/*
	 *  Apply Gaussian blur to image
	 *  @param source image
	 *  @return blurred image
	 */
	public static Mat gaussianBlur(Mat src) {
		final Size mask = new Size(3, 3);
		Mat dst = new Mat();
		Imgproc.GaussianBlur(src, dst, mask, 0);

		return dst;
	}
	
	public static Mat cannyEdge(Mat gray) {
		Mat dst = new Mat();

		double lowThresh = 100;//150.0; //350.0; //default 100
	    int RATIO = 3;	//recommended = 3
	    Imgproc.Canny(gray, dst, lowThresh, lowThresh * RATIO);

        Mat dst2 = closing(dst);

		return dst2;
	}
	
	private static Mat sobelFilter(Mat src) {
		Mat dst = new Mat();
		Imgproc.Sobel(src, dst, -1, 0, 1);
		return dst;
	}
	
	private static Mat colorFilter(Mat src) {
		Mat dst = new Mat();
		
		Mat srchsv = new Mat();
		Mat tempYellow = new Mat();
		Mat tempWhite = new Mat();
		
	    Imgproc.cvtColor(src, srchsv, Imgproc.COLOR_BGR2HSV); // COLOR_RGB2HSV
	     
	    Scalar lowerYellow = new Scalar(18,94,140);
	    Scalar upperYellow = new Scalar(48,255,255);
	    Scalar lowerWhite = new Scalar(0,0,200);
	    Scalar upperWhite = new Scalar(255,255,255);

	    Core.inRange(srchsv, lowerYellow, upperYellow, tempYellow);
	    Core.inRange(srchsv, lowerWhite, upperWhite, tempWhite);
	    Core.bitwise_or(tempYellow, tempWhite, dst);

		return dst;
	}
	
	public static Mat colorFilterBlack(Mat src) {
		Mat dst = new Mat();
		Mat srchsv = new Mat();
		Mat tempBlack = new Mat();
		Mat tempWhite = new Mat();
		
	    Imgproc.cvtColor(src, srchsv, Imgproc.COLOR_BGR2HSV);
        Imgproc.resize(srchsv, srchsv, new Size(10*srchsv.size().width, 10*srchsv.size().height)); 

	    Scalar lowerBlack = new Scalar(0,0,0);
	    Scalar upperBlack = new Scalar(180,255,30);

	
	    Core.inRange(srchsv, lowerBlack, upperBlack, dst);
	    //Core.bitwise_or(tempBlack, src, dst);
	    
	
		return dst;
		
	}
	
	
	public static Mat closing(Mat src) {
		Mat dst = new Mat();
		//Mat matImgDst = new Mat();
		//dilation and erosion
		int kernelSize = 11;
		Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(2 * kernelSize + 1, 2 * kernelSize + 1),
                new Point(kernelSize, kernelSize));
		Mat temp = new Mat();
		Imgproc.dilate(src, temp, element);
        Imgproc.erode(temp, dst, element);
        
		return dst;
	}
	
	
	/*
	 *  apply gray scale, gaussian blur and then canny edge detection to src image
	 *  @param src image
	 *  @return preprocessed image
	 */
	 public static Mat preProcessing(Mat src) {
		Mat gray = new Mat();
		Mat cny = new Mat();

		Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
		HighGui.imshow("0-1Gray", gray);

		cny = cannyEdge(gaussianBlur(gray));
		HighGui.imshow("0-2Canny", cny);

		Mat clr = new Mat();
		clr = colorFilter(src);
		HighGui.imshow("0-3Color", clr);

		Mat dst = new Mat();
		Core.bitwise_and(cny, clr, dst);
		HighGui.imshow("1End_of_Preprocessing", dst);

		return dst;
	}

	 /*
	  * Crop out the region of interest(ROI)
	  */
	 private static Mat ROIfilter(Mat src) {
		 Mat dst = new Mat();
		 Rect roi = new Rect(new Point(0, src.rows()/2), new Point(src.cols(),src.rows()));
		 dst = src.submat(roi);
		 
		 return dst;
	 }
	 
	 private static Mat[] getIPMMatrices(Mat src, Point[] pts) {
		 List<Point> corner = new ArrayList<Point>();		 
		 List<Point> target = new ArrayList<Point>(); 
		 
		 corner.add(pts[3]);
		 corner.add(pts[0]);
		 corner.add(pts[1]);
		 corner.add(pts[2]);
		 
 	     double offset = src.cols()/3;
	  	 target.add(new Point(offset,src.rows())); 
	     target.add(new Point(offset,0));
	     target.add(new Point(src.cols()-offset, 0));	
	     target.add(new Point(src.cols()-offset,src.rows())); 		
	     
		 Mat cornerMat = Converters.vector_Point2f_to_Mat(corner);	
		 Mat targetMat = Converters.vector_Point2f_to_Mat(target);
		 
		 Mat trans = new Mat();
		 trans = Imgproc.getPerspectiveTransform(cornerMat,targetMat);
		 Mat invtrs = new Mat();
		 invtrs = Imgproc.getPerspectiveTransform(targetMat,cornerMat);
		 
		 return new Mat[] {trans,invtrs};
	 }
	 
	 public static Mat perspectiveWarp(Mat src, Mat IPMtrans) {
			Mat temp = new Mat();	
			temp = preProcessing(src);	
			Imgproc.warpPerspective(src, temp, IPMtrans, new Size(src.cols(),src.rows()));
			
			Mat dst = new Mat();
			dst = colorFilter(gaussianBlur(temp));
			return dst;
		}
	 
	 /*
	  * @param src the image after Inverse Perspective Transform 
	  * @return mat the obtained histogram
	  */
	 public static Mat calcHist(Mat src, Point[] _pts) {
	        Mat dst = new Mat( src.rows(), src.cols(), CvType.CV_8UC3, new Scalar( 0,0,0) );
	       
	    	float [] x = new float[src.cols()];
	    			
	    	//counting the number of non-black pixels in each bin
	    	for(int c = 0; c< src.cols(); c++) {
	    		for(int r = 0; r< src.rows(); r++) {
		    	    if(src.get(r, c)[0]==255.0) {
		    	    	x[c]++;
		    	    }
	    		}
	    	}
	    	
	    	histogramArray = x;
	    	
	        for(int i = 0 ; i < x.length-1; i++) {
	            Imgproc.line(dst, new Point(i, src.rows() - Math.round(x[i])),
	                    new Point(i+1, src.rows() - Math.round(x[i+1])), new Scalar(255, 0, 0), 2);
	        }
	        
	       	        
		   	return dst;
	}
	
	 /*
	  * Given two points (down-left and up-right corner of window, display that window in the source image
	  */
	 private static void displaySlidingWindow(Point p1, Point p2, Mat src) {
		 Imgproc.line(src, new Point(p1.x, p2.y), new Point(p2.x, p2.y), new Scalar(100,255,0), 2, Imgproc.LINE_AA, 0);
		 Imgproc.line(src, new Point(p2.x, p2.y), new Point(p2.x, p1.y), new Scalar(100,255,0), 2, Imgproc.LINE_AA, 0);
		 Imgproc.line(src, new Point(p2.x, p1.y), new Point(p1.x, p1.y), new Scalar(100,255,0), 2, Imgproc.LINE_AA, 0);
		 Imgproc.line(src, new Point(p1.x, p1.y), new Point(p1.x, p2.y), new Scalar(100,255,0), 2, Imgproc.LINE_AA, 0);
	 }
	 
	 /*
	  * Given the center point of a window, return number of non-zero points enclosed in that window (Lower Abstraction)
	  */
	 private static ArrayList<Point> getpointsEnclosed(int low_x, int high_x, int low_y, int high_y, List<Point> nArray){
		 ArrayList<Point> e = new ArrayList<Point>();
		 for(Point p: nArray) {
				if(p.y >= low_y && p.y < high_y && p.x >= low_x && p.x < high_x) {
					e.add(p);
				}
			}
		 return e;
	 }
	 
	 /*
	  * Given a point(center of window), get the four boundary(down, up, right, left) of that window
	  */
	 private static int[] calculateWindow(Point p, int h, int w) {
		 int [] windowBorder =  new int[4]; // win_y_high, win_y_low, win_x_high, win_x_low
		 
		 windowBorder[0] = (int)(p.y + h/2);
		 windowBorder[1] = (int)(p.y - h/2);
		 windowBorder[2] = (int)(p.x + w/2);
		 windowBorder[3] = (int)(p.x - w/2);

		 return windowBorder;
	 }
	 
	 /*
	  * Given the center point of a window, return number of non-zero points enclosed in that window (Higher Abstraction)
	  */
	 private static LaneLine getTestPoints(Point centerPoint, int wHeight, int wWidth, List<Point> nArray) {
		 int [] windowBoarder = calculateWindow(centerPoint, wHeight, wWidth);
		 
		 LaneLine pointsEnclosed = new LaneLine();
		 pointsEnclosed.addPoints(getpointsEnclosed(windowBoarder[3], windowBoarder[2], windowBoarder[1], windowBoarder[0], nArray));
		 
		 return pointsEnclosed;
	 }
	 
	 /*
	  * Given the center point of old window, return the vector that point from center of old window to that of new window
	  */
	 private static Point testWindowShift(Point oldWindowCenter, int wHeight, int wWidth, Point betaVector, List<Point> nArray) {
		 Point betaPoint = new Point(oldWindowCenter.x + betaVector.x, oldWindowCenter.y + betaVector.y);
		 LaneLine pointsInBeta =  getTestPoints(betaPoint, wHeight, wWidth, nArray);
		 int betaAvgX = pointsInBeta.averageX();

		 Point GammaPoint = new Point(betaAvgX, betaPoint.y);
		 LaneLine pointsInGamma =  getTestPoints(GammaPoint, wHeight, wWidth, nArray);
		 int gammaAvgX = pointsInGamma.averageX();

		 double weightBeta = 0;
		 double weightGamma = 0;
		 
		 if(0 != (pointsInBeta.size()+pointsInGamma.size())) {
			 weightBeta = (double)pointsInBeta.size()/(pointsInBeta.size()+pointsInGamma.size());
			 weightGamma = (double)pointsInGamma.size()/(pointsInBeta.size()+pointsInGamma.size());
		 }
		 
		 Point finalVector = new Point((betaAvgX-oldWindowCenter.x)*weightBeta + (gammaAvgX-oldWindowCenter.x)*weightGamma, betaVector.y);
		 
		 return finalVector;
	 }
	 
	
	 public static Point getMidLanePointFromIPM() {
		 Point p = new Point();

		 if(egoLanel.getPoints().size()>3 && egoLaner.getPoints().size()>3) {
			 p.x = (egoLanel.getPoints().get(3).x + egoLaner.getPoints().get(3).x)/2;
			 p.y = egoLanel.getPoints().get(3).y;
		 }
		 
		 //System.out.println("invSIZE="+_iPMMatrices.size());
		 //System.out.println("invDUMP="+_iPMMatrices.dump());

		 double [][] ipm = mat2Double(_invIPMMatrice);
		 //retrieve lane points from IPM
		 Point dstP = reverseIPMPoint(p, ipm);
		 
		 return dstP;	
	 }
	 
	 /*
	  * Convert IPM transfer matrix to a 2D array of type double
	  */
	 public static double [][] mat2Double(Mat invIPM){	
		 double [][] ipm = new double [3][3];
		 for(int i = 0 ; i< invIPM.cols(); i++) {
			 for(int j = 0 ; j< invIPM.rows(); j++) {
				 ipm[j][i] = invIPM.get(j, i)[0];		
			 }
		 }
		 
		 for(int j = 0 ; j< invIPM.cols(); j++) {
			 for(int i = 0 ; i< invIPM.rows(); i++) {
				 //System.out.print(ipm[j][i]+",");		 
			 }
			 //System.out.println();
		 }
		 return ipm;
	 }
	 
	 public static Point reverseIPMPoint(Point p, double [][] ipm) {
		 Point dstP = new Point();
		 dstP.x = (ipm[0][0]*p.x+ipm[0][1]*p.y+ipm[0][2])/(ipm[2][0]*p.x+ipm[2][1]*p.y+ipm[2][2]);
		 dstP.y = (ipm[1][0]*p.x+ipm[1][1]*p.y+ipm[1][2])/(ipm[2][0]*p.x+ipm[2][1]*p.y+ipm[2][2]);
		 return dstP;
	 }
	 
	 
	 
	 /*
	  * improved sliding window for curve markings
	  * @param src bird-eye view of lane image; pts coordinates for calculating inverse Matrix of IPM ; 
	  * hist histogram; narray array that stores all non-zero points in histogram
	  * @return 
	  */
	public static Mat multiLaneFit(Mat src, Point[] pts, Mat hist, List<Point> nArray, boolean findEgoOnly) {
		Mat dst = src;
		List <Integer> _laneMiddlePoints = getLaneStartingPoints();
		
		LaneLine [] lanes = new LaneLine [_laneMiddlePoints.size()];
		LaneLine curLane = new LaneLine();
		LaneLine curLaneMidPoints;
		
		int [] positionMarker = new int[] {0, _laneMiddlePoints.size()};

		egoLanel = new LaneLine();
		egoLanel.getPoints().add(new Point(0,src.rows()));
		egoLaner = new LaneLine();
		egoLaner.getPoints().add(new Point(src.cols(),src.rows()));
		
		for(Integer i : _laneMiddlePoints) {
			if(i<src.cols()/2) {
				if(egoLanel.getPoints().get(0).x < i) {
					egoLanel.getPoints().get(0).x = i;
					positionMarker[0] = _laneMiddlePoints.indexOf(i);
				}
			}else {
				if(egoLaner.getPoints().get(0).x > i) {
					egoLaner.getPoints().get(0).x = i;
					positionMarker[1] = _laneMiddlePoints.indexOf(i);
				}
			}
		}
		
		int wNumber = 9;
		int wHeight = src.rows()/wNumber;
		int wWidth = 100;
		int minPix = 300;//90
		
		int currentX = 0;
		int win_y_low = 0;
		int win_y_high = 0;
		int win_x_low = 0;
		int win_x_high = 0;
		
		for(int i=0 ; i< lanes.length; i++) {
			lanes[i] = new LaneLine();
		}
		
		totLanes = new ArrayList<LaneLine>();
		
		for(int i=0; i< _laneMiddlePoints.size() ; i++) {
			currentX = _laneMiddlePoints.get(i);
			curLaneMidPoints = new LaneLine();

			int VectorX = 0;
			Point oldPoint = new Point(currentX, src.rows()+ 0.5 * wHeight);
			Point newPoint = new Point(currentX, src.rows()+ 0.5 * wHeight);
			
			for(int j = 0 ; j < wNumber ; j++) {
				curLane.getPoints().clear();
				
				Point betaTestVector = new Point(VectorX, -wHeight); 
				Point actualVector = testWindowShift(oldPoint, wHeight, wWidth, betaTestVector, nArray);
				newPoint = new Point(oldPoint.x + actualVector.x, oldPoint.y + actualVector.y);
				
				if(findEgoOnly) {
					if(i == positionMarker[0] || i == positionMarker[1]) {
						displaySlidingWindow(new Point(newPoint.x - wWidth/2, newPoint.y + wHeight/2), new Point(newPoint.x + wWidth/2, newPoint.y - wHeight/2), src);
					}
				}else {
					displaySlidingWindow(new Point(newPoint.x - wWidth/2, newPoint.y + wHeight/2), new Point(newPoint.x + wWidth/2, newPoint.y - wHeight/2), src);
				}

				VectorX = (int)(newPoint.x-oldPoint.x);
				oldPoint = newPoint;

				curLane.addPoints(getpointsEnclosed(win_x_low,win_x_high,win_y_low,win_y_high,nArray));
				
				lanes[i].addPoints(curLane.getPoints());
				
				curLaneMidPoints.getPoints().add(newPoint);
				
				if(curLane.size() > minPix) {
					newPoint.x = curLane.averageX();
				}
							
			}
				
			if(i == positionMarker[0]){
				egoLanel.getPoints().clear();
				egoLanel.getPoints().addAll(curLaneMidPoints.getPoints());
				totLanes.add(egoLanel);		
				drawDetectedLine(egoLanel, dst);
			}else if(i == positionMarker[1]) {
				egoLaner.getPoints().clear();
				egoLaner.getPoints().addAll(curLaneMidPoints.getPoints());
				totLanes.add(egoLaner);		
				drawDetectedLine(egoLaner, dst);
			}else {
				if(!findEgoOnly) {
					totLanes.add(curLaneMidPoints);		
				}
			}
		}
		return dst; 
	}
	
	
	private static void drawDetectedLine(LaneLine curLaneMidPoints, Mat dst) {
		//polynomial line fit
		MatOfPoint mp = new MatOfPoint();
		mp.fromList(curLaneMidPoints.getPoints());
		List<MatOfPoint> lmop = new ArrayList<MatOfPoint>();
		lmop.add(mp);
		Imgproc.polylines (
				 dst,                    // Matrix obj of the image
		         lmop,                      // java.util.List<MatOfPoint> pts
		         false,                     // isClosed
		         new Scalar(255,0,255),     // Scalar object for color, only black or white(gray scale)
		         3                          // Thickness of the line
		);
		 
		HighGui.imshow("drawDetectedLine", dst);
		/*
		//Spline fit
		MatOfPoint mp = new MatOfPoint();
		mp.fromList(curLaneMidPoints.extSplineFit().getPoints());
		List<MatOfPoint> lmop = new ArrayList<MatOfPoint>();
		lmop.add(mp);
		Imgproc.polylines (
				 dst,                    // Matrix obj of the image
		         lmop,                      // java.util.List<MatOfPoint> pts
		         false,                     // isClosed
		         new Scalar(255,0,255),     // Scalar object for color, only black or white(gray scale)
		         3                          // Thickness of the line
		);
		*/	
	}
	
	/*
	 * return a list of lane marking starting points
	 */
	public static List<Integer> getLaneStartingPoints() {
		List<Integer> lsp = new ArrayList<Integer>();
		boolean sequenceStarted = false;
		float peakValue = 0.0f;
		int peakLocation = 0;
		int sequenceLength = 0;
				
		for(int i = 0 ; i <= histogramArray.length-1; i++) {
			if(0 == histogramArray[i] || i == histogramArray.length-1) {
				//End of sequence
				if(sequenceStarted) {
					if(sequenceLength > 10 && peakValue > 10) {
						lsp.add(peakLocation);
					}
					sequenceLength = 0;
					peakLocation = 0;
					peakValue = 0;
					sequenceStarted = false;
				}
			}else {
				//sequence keep adding
				if(sequenceStarted) {
					peakLocation = (histogramArray[i] > peakValue) ? i : peakLocation;
					peakValue = (histogramArray[i] > peakValue) ? histogramArray[i] : peakValue;
				}
				//Start of sequence
				else {	
					sequenceStarted = true;
					peakLocation = i;
					peakValue = histogramArray[i];
				}
				sequenceLength++;
			}
		}
		//histogramArray
		return lsp;
	}
	
	/*
	 * find current lane(left/right side) from the list of all lanes detected
	 */
	private static int[] getcurrentLaneStart(LaneLine peaks, int imgWidth) {
		int[] start_x = new int[] {0,imgWidth}; 	//left, right
		Integer peak = 0;
		for(Point p: peaks.getPoints()) {
			peak = (int)p.x;
			if(peak <= imgWidth/2) {	//left lane
				if(peak > start_x[0])
					start_x[0] = peak;
			}else {						//right lane
				if(peak < start_x[1])
					start_x[1] = peak;
			}
		}
		
		return start_x;
	}
	


	/*
	 * @param src: quasi-binarized image having only pixel values of 255.0(white) or 0.0(black)
	 */
	private static List<Point> getLanePoints(Mat src) {
		List<Point> nArray = new ArrayList<Point>();
		
    	//counting the number of non-black pixels in each bin
    	for(int c = 0; c< src.cols(); c++) {
    		for(int r = 0; r< src.rows(); r++) {
	    	    if(src.get(r, c)[0]==255.0) {
	    	    	nArray.add(new Point(c, r));
	    	    }
    		}
    	}
    	return nArray;
	}
	
	/*
	 * 
	 */
	public static Mat getInvpers(Mat image_mat_Cropped, Point[] _pts) {
		Mat[] IPMMatrices = getIPMMatrices(image_mat_Cropped, _pts);
		Mat _invPers = IPMMatrices[1];
		return _invPers;
	}
	
	
	/*
	 * 
	 */
	public static Mat multiLaneFinder(Mat image_mat_Cropped, Point[] _pts, boolean findEgoOnly) {
		Mat[] IPMMatrices = getIPMMatrices(image_mat_Cropped, _pts);
		Mat _invPers = ImageUtils.perspectiveWarp(image_mat_Cropped, IPMMatrices[0]);
		_invIPMMatrice = IPMMatrices[1];
		_iPMMatrice = IPMMatrices[0];
		Mat _hist = ImageUtils.calcHist(_invPers, _pts);
		List<Point> nArray = getLanePoints(_invPers);
		Mat laneFit = ImageUtils.multiLaneFit(_invPers, _pts, _hist, nArray, findEgoOnly);
		Mat dst = new Mat();
		
		HighGui.imshow("_invPers", _invPers);
		
		Imgproc.warpPerspective(laneFit, dst, IPMMatrices[1], new Size(laneFit.cols(),laneFit.rows()));
        
		return dst; //_invPers
	}
	
	
			
	
}
