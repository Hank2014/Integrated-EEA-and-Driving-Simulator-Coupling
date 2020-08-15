package ptolemy.actor.lib.conversions;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

public class LaneDetection {
	final static int Ncols = 1280;
	final static int Nrows = 720;
	public static void main(String[] args) {
		nu.pattern.OpenCV.loadShared();	
		VideoCapture cam = new VideoCapture("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\train5.mp4");
		if(!cam.isOpened()) {
			System.err.println("Error!");
			System.exit(0);
		}
		
		Mat frame = new Mat();
		int count = 0;  
		while(cam.read(frame)) { 
			if(count<1750)
				count++;
			else {
				System.out.println("got a new frame!");
				Point p1= new Point(frame.cols()*0.25,frame.rows()*0.25),
					  p4= new Point(frame.cols()*0.75,frame.rows()*0.75);
				Rect rectCrop = new Rect(p1,p4);
				Mat frameCrop = frame.submat(rectCrop);
				System.out.println(frame.size()+" "+frameCrop.size());
				Mat a = gaussianAndSobel(perspectiveWarp(thresholding(frameCrop)));
				//HighGui.imshow("frameCrop",frameCrop);
				//Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\testVid.png",frameCrop);
				
				Mat b = perspectiveWarp(thresholding(frameCrop));
				
				Mat c = houghTransfrom(frameCrop);
				Mat dst = new Mat();
				
				
				Core.addWeighted(frameCrop, 0.8, c, 1, 0, dst);
				
				HighGui.imshow("overlap", dst);
				HighGui.imshow("perspectiveWarp",a);
				HighGui.imshow("hough",b);
				HighGui.waitKey();
			}
		}System.out.println("count="+count);
		cam.release();                              
		String file = "C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\RGB_49.png";
		
		//Optimize: Should the src image be cropped for Region of Interests?
		//final Mat srcUncropped = Imgcodecs.imread(file);
		//Rect rectCrop = new Rect(0, (int)(srcUncropped.rows()*0.33), srcUncropped.cols(), srcUncropped.rows());
		
		
		Mat src = Imgcodecs.imread(file);
		
		//System.out.println(System.getenv("PTII"));
		System.out.println(System.getProperty("user.dir")); // \rmiinterface Ordner
	
		
		/////////////// Image Processing ///////////////
		////										////

		//Mat sob = gaussianAndSobel(src);
		//Imgproc.threshold(src, dst, thresh, maxval, type);
		//Mat aft = affineTransform(sob); 
		//Mat warpMat = Imgproc.getPerspectiveTransform(src,dst);
        
		/*
		thresholding(src);
		houghTransfrom(src);
		perspectiveWarp(src);
		perspectiveWarp(thresholding(src));
		*/
	
		
        
     
		Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_RED.png",src);
        
	}
		
	public static Mat grayScale(Mat src) {
		Mat dst = new Mat();
		Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);
		//Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_GRAY.png",dst);
		//HighGui.imshow("gray", dst);
		return dst;
	}
	
	
	public static Mat affineTransform(Mat sob) {
		Mat warpDst = Mat.zeros(sob.cols(), sob.rows(), sob.type() );
		
		Point[] srcTri = new Point[3];
        srcTri[0] = new Point( 0, 0 );
        srcTri[1] = new Point( sob.cols() - 1, 0 );
        srcTri[2] = new Point( 0, sob.rows() - 1 );
        
        Point[] dstTri = new Point[3];
        dstTri[0] = new Point( 0, sob.rows()*0.33 );
        dstTri[1] = new Point( sob.cols()*0.85, sob.rows()*0.25 );
        dstTri[2] = new Point( sob.cols()*0.15, sob.rows()*0.7 );
		
        Mat warpMat = Imgproc.getAffineTransform( new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri) );
        
        Imgproc.warpAffine( sob, warpDst, warpMat, warpDst.size() );
        Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_AffineTrans.png",warpDst);
        
        return warpDst;
	}
	
	
	public static Mat cannyEdge(Mat gray) {
		Mat dst = new Mat();
        double lowThresh = 150.0; //350.0;
        int RATIO = 3;	//recommended = 3
        
       
        //Core.bitwise_and(roi, tempWhite, dst);
        
        Imgproc.Canny(gray, dst, lowThresh, lowThresh * RATIO);
        //HighGui.imshow("canny", dst);
        
        //Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_CannyEdge.png",dst);
		return dst;
	}
	
	
	public static Mat gaussianAndSobel(Mat src) {
		Mat dst = new Mat();
	
		//gaussian blur
		final Size mask = new Size(3, 3);
		Mat gus = new Mat();
		Imgproc.GaussianBlur(src, gus, mask, 0);
		Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_GaussianBlur.png",gus);

				
		//sobel filter
		Imgproc.Sobel(gus, dst, -1, 0, 1);
		Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\1_SobelFiltered.png",dst);
		
		return dst;
				
	}
	
	
	public static Mat colorFilter(Mat src) {
		Mat dst = new Mat();
		
		Mat srchsv = new Mat();
		Mat tempYellow = new Mat();
		Mat tempWhite = new Mat();
		
        Imgproc.cvtColor(src, srchsv, Imgproc.COLOR_BGR2HSV);
         
        Scalar lowerYellow = new Scalar(18,94,140);
        Scalar upperYellow = new Scalar(48,255,255);
        Scalar lowerWhite = new Scalar(0,0,200);
        Scalar upperWhite = new Scalar(255,255,255);

        Core.inRange(srchsv, lowerYellow, upperYellow, tempYellow);
        Core.inRange(srchsv, lowerWhite, upperWhite, tempWhite);
        Core.bitwise_or(tempYellow, tempWhite, dst);
        
		return dst;
	}
	
	public static Mat gaussianBlur(Mat src) {
		final Size mask = new Size(3, 3);
		Mat dst = new Mat();
		Imgproc.GaussianBlur(src, dst, mask, 0);
		return dst;
	}
	
	public static Mat thresholding(Mat src) {
		Mat dst = new Mat();
		Mat gray = new Mat();
		Mat cny = new Mat();
		
		Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
		
		cny = cannyEdge(gaussianBlur(gray));
		
		Mat clr = new Mat();
		clr = colorFilter(src);
		
		Core.bitwise_or(cny, clr, dst);

		return dst;
			
	}
	
	public static Mat closing(Mat src) {
		Mat dst = new Mat();
		//Mat matImgDst = new Mat();
		/* dilation and erosion
		int kernelSize = 0;
		Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(2 * kernelSize + 1, 2 * kernelSize + 1),
                new Point(kernelSize, kernelSize));
		Mat temp = new Mat();
		Imgproc.dilate(cny, temp, element);
        Imgproc.erode(temp, matImgDst, element);
       
		*/
		return dst;
	}
	
	public static Mat houghTransfrom(Mat src) {
		Mat dst = new Mat(), cdst = new Mat(), cdstP = new Mat();	
		dst = thresholding(src);
		
		Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);
		//HighGui.imshow("cdst", cdst);
		cdstP = cdst.clone();
		
		Mat lines = new Mat();
		Imgproc.HoughLines(dst, lines, 1, Math.PI/180, 150); 
		// Draw the lines
        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0],
                    theta = lines.get(x, 0)[1];
            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a*rho, y0 = b*rho;
            Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
            Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));
            //Imgproc.line(cdst, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }

        // Probabilistic Line Transform
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(dst, linesP, 1, Math.PI/180, 50, 50, 10); // runs the actual detection
        // Draw the lines
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 0), 1, Imgproc.LINE_AA, 0);
        }

        findLaneSlopes(cdstP, linesP);
		return cdstP;
	}
	
	public static Mat perspectiveWarp(Mat src) {
		Mat dst = new Mat();
		
		List<Point> corner = new ArrayList<Point>();		 
		List<Point> target = new ArrayList<Point>();

		corner.add(new Point(3, 0.713*src.rows()));//3,514)); ld
		corner.add(new Point(0.566*src.cols(),0.518*src.rows()));//725,373));lu 
		corner.add(new Point(0.689*src.cols(),0.518*src.rows()));//882,373)); ru 
		corner.add(new Point(0.996*src.cols(),0.679*src.rows()));//1276,489)); rd
	

		double offset = src.cols()/3;
		target.add(new Point(offset,src.rows())); 
	    target.add(new Point(offset,0));
	    target.add(new Point(src.cols()-offset, 0));	
	    target.add(new Point(src.cols()-offset,src.rows())); 
		
		
	    Mat cornerMat = Converters.vector_Point2f_to_Mat(corner);
			
		Mat targetMat = Converters.vector_Point2f_to_Mat(target);
	
		Mat trans = new Mat();
		trans = Imgproc.getPerspectiveTransform(cornerMat,targetMat);
			
		Mat invdst = new Mat();
		invdst = Imgproc.getPerspectiveTransform(targetMat, cornerMat);
			
		Imgproc.warpPerspective(src, dst, trans, new Size(src.cols(),src.rows()));
		//Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\ok\\dst.png",dst);
		//HighGui.imshow("prspTrans", dst);
		//HighGui.waitKey();
		/*rotate image
		Mat rotMat = new Mat();
		rotMat = Imgpc.getRotationMatrix2D(new Point(src.cols()/2), src.rows()/2, 90, 1.0);
		Mat dstrot = new Mat();		
		Imgproc.warpAffine(dst, dstrot, rotMat, new Size(src.cols(),src.rows()));
		Imgcodecs.imwrite("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\dstrot.png",dstrot);
		*/
		
		return dst;
	}
	

	public static Mat fitLine(Mat src) {
		Mat dst = new Mat();
		Mat points = new Mat(1, 4, CvType.CV_32FC2);
		points.put(0, 0, 0, 0, 2, 3, 3, 4, 5, 8);
		Mat linePoints = new Mat(4, 1, CvType.CV_32FC1);
		linePoints.put(0, 0, 0.53198653, 0.84675282, 2.5, 3.75);
		Imgproc.fitLine(src, dst, Imgproc.CV_DIST_L12, 0, 0.01, 0.01);
		//HighGui.imshow("fitline", dst);
		//assertMatEqual(linePoints, dst, EPS);
		    
		return dst;
	}
	
	
	private static void findLaneSlopes(Mat src, Mat linesP) {
		double [] leftSide = {0.0,0.0};
		double [] rightSide = {0.0,0.0};
		List <Double> leftSlope = new ArrayList<Double>();
		List <Double> rightSlope = new ArrayList<Double>();
		List <Double> leftIntercept = new ArrayList<Double>();
		List <Double> rightIntercept = new ArrayList<Double>();
		
		double slope = 0.0;
		double intercept = 0.0;	//y-intercept (x=0)
		int [] xy = {0,0,0,0};
		for (int x = 0; x < linesP.rows(); x++) {
				xy[0] = (int)linesP.get(x, 0)[0];	//x1
				xy[1] = (int)linesP.get(x, 0)[1];	//y1
				xy[2] = (int)linesP.get(x, 0)[2];	//x2
				xy[3] = (int)linesP.get(x, 0)[3];	//y2
			if(0 != xy[2]-xy[0]) {
				slope = (double)(xy[3]-xy[1])/(xy[2]-xy[0]);
				intercept =(xy[3]*xy[0]-xy[1]*xy[2])/(xy[0]-xy[2]);
			}
			
			if(slope < 0 ) {
				leftSlope.add(slope);
				leftIntercept.add(intercept);
			}else{
				rightSlope.add(slope);
				rightIntercept.add(intercept);
			}
			//System.out.println(new DecimalFormat("#.##").format(slope)+","+intercept);
	    }
		
		leftSide[0] = leftSlope.stream().mapToDouble(val -> val).average().orElse(0.0);	//avgLeftSlope
		leftSide[1] = leftIntercept.stream().mapToDouble(val -> val).average().orElse(0.0);	//avgLIntercept
		rightSide[0] = rightSlope.stream().mapToDouble(val -> val).average().orElse(0.0);	//avgRightSlope
		rightSide[1] = rightIntercept.stream().mapToDouble(val -> val).average().orElse(0.0);	//avgRIntercept
		
		newCoordinates(src, leftSide);	
		newCoordinates(src, rightSide);	
		
		float left_begin_position = 0.0f;
		float right_begin_position = 0.0f;
		
		left_begin_position = (float) ((src.rows()*0.6 - leftSide[1])/leftSide[0]);
		right_begin_position = (float) ((src.rows()*0.6 - rightSide[1])/rightSide[0]);
		
		System.out.println("leftLine= "+leftSide[0]+"x+"+leftSide[1]);
		System.out.println("rightLine= "+rightSide[0]+"x+"+rightSide[1]);

		
		float mean_begin_position = (left_begin_position + right_begin_position) / 2;
		getAngleBetweenPoints(src, new Point(src.cols()/2,src.rows()),new Point(mean_begin_position,src.rows()*0.6));
	}
	
	/*
	 * returns true if the slope!=0 and is drawn on the photo
	 */
	public static boolean newCoordinates(Mat src, double[] lp) {
		boolean found = false;
		int y1 = src.rows();
		int y2 = (int)(y1*0.6);
		Point pt1 = new Point((int)((y1-lp[1])/lp[0]), y1);
        Point pt2 = new Point((int)((y2-lp[1])/lp[0]), y2);
        //System.out.println("["+pt1.x+","+pt1.y+"] "+"["+pt2.x+","+pt2.y+"]");
        //System.out.println("int="+lp[1]);
        if(0 != lp[0]) {
        	found = true;
        	Imgproc.line(src, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }
        return found;
   }
	
	public static double getAngleBetweenPoints(Mat src, Point carFront, Point laneMiddle) 
	{
		float angle = 0.0f;
		//Point vector1 = new Point(0,-1);
		Point vector2 = new Point(laneMiddle.x-carFront.x, laneMiddle.y-carFront.y);
		
		double lengthV2 = Math.sqrt(vector2.x * vector2.x + vector2.y * vector2.y);
		
		angle = (float)Math.toDegrees(Math.acos(-1 * vector2.y / lengthV2));
		
		System.out.println("carFront=["+carFront.x+","+carFront.y+"]");
		System.out.println("laneMiddle=["+laneMiddle.x+","+laneMiddle.y+"]");
		System.out.println("ang= "+angle);
    	
		Imgproc.line(src, carFront, new Point(carFront.x,carFront.y*0.6), new Scalar(0, 255, 255), 3, Imgproc.LINE_AA, 0);
    	Imgproc.line(src, carFront, laneMiddle, new Scalar(0, 255, 0), 3, Imgproc.LINE_AA, 0);

		//Math.acos((vector1.x * vector2.x +vector1.y * vector1.y) / lengthV2);
		return angle;
	}
}


