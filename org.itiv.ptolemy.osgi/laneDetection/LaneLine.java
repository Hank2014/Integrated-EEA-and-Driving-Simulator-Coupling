package laneDetection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

public class LaneLine{
	private List <Point> points;
	private double slope = 0.0;
	private double intercept = 0.0;	//value of y when x=0
	private boolean assigned = false;		//assigned => found a ture positive detection

	
	public LaneLine() {
		points = new ArrayList<Point>();
	}
	
	public LaneLine(Point p1, Point p2) {
		points = new ArrayList<Point>();
		points.add(p1);
		points.add(p2);
		if(0 != (p2.x-p1.x)) {
			slope = ((double)(p2.y-p1.y)/(p2.x-p1.x));
			intercept = (p2.y*p1.x-p1.y*p2.x)/(p1.x-p2.x);
		}
		assigned = false;
	}
	
	public LaneLine(ArrayList <Point> pts) throws WrongParameterException{
		 if(5 == pts.size()) {
			 points = pts;	 
		 }else {
			 throw new WrongParameterException(
					 "Wrong number of points for LaneLines constructor, accepted number of points is 2 or 5");
		 }
	}
	
	public int averageX() {
		int avg = 0;
		int sum = 0;
		
		for(Point p : points) {
			sum += p.x;
		}
		if(0!=points.size())
			avg = ((int)sum)/points.size();
		
		return avg;
	}
	
	public ArrayList<Point> getPoints(){
	 return (ArrayList<Point>)this.points;
	}
	
	public void addPoint(Point e) {
		points.add(e);
	}
	
	public void addPoints(List<Point> e) {
		points.addAll(e);
	}
	
	public int size() {
		return points.size();
	}
	
	@Override
	public boolean equals(Object o) {
		boolean ret = false;
		if(o instanceof LaneLine) {
			float f = (float)Math.abs(this.slope-((LaneLine)o).slope);
			if(f<0.1)
				ret = true;
		}
		return ret;
	}
	
	
	public LaneLine generateRepresentativePoints(LaneLine adjustment) {
		LaneLine ll = new LaneLine();
		
		double minY = getExtremeYCord()[1];
		double maxY = getExtremeYCord()[0];
		
		if(2 == this.points.size()) {
			ll.addPoint(new Point((194.06-intercept)/slope, 194.06));
			ll.addPoint(new Point((127.98-intercept)/slope, 127.98));
			ll.addPoint(new Point((93.27-intercept)/slope, 93.27));
			ll.addPoint(new Point((71.87-intercept)/slope, 71.87));
			ll.addPoint(new Point((57.36-intercept)/slope, 57.36));
			ll.addPoint(new Point((46.87-intercept)/slope, 46.87));
			ll.addPoint(new Point((38.93-intercept)/slope, 38.93));
			ll.addPoint(new Point((32.72-intercept)/slope, 32.72));
			ll.addPoint(new Point((27.72-intercept)/slope, 27.72));
		}else if(9 == this.points.size()) {
			PolynomialSplineFunction func = this.splineFit();
			for(Point p:adjustment.getPoints()) {
				if(p.y >= minY && p.y <= maxY)
					ll.addPoint(new Point(func.value(p.y), p.y));
				else {
					if(p.y < minY) {
						ll.addPoint(new Point(func.value(minY+1), minY+1));						
					}else { //p.y > 230.01 
						ll.addPoint(new Point(func.value(maxY-1), maxY-1));
					}
				}
			}
			for(Point p : ll.getPoints()) {
			//	System.out.println("newPoints= "+p);
			}
		}

		return ll;
	}
	
	public LaneLine generateRepresentativePoints() {
		LaneLine ll = new LaneLine();
		if(2 == this.points.size()) {
			ll.addPoint(new Point((194.06-intercept)/slope, 194.06));
			ll.addPoint(new Point((127.98-intercept)/slope, 127.98));
			ll.addPoint(new Point((93.27-intercept)/slope, 93.27));
			ll.addPoint(new Point((71.87-intercept)/slope, 71.87));
			ll.addPoint(new Point((57.36-intercept)/slope, 57.36));
			ll.addPoint(new Point((46.87-intercept)/slope, 46.87));
			ll.addPoint(new Point((38.93-intercept)/slope, 38.93));
			ll.addPoint(new Point((32.72-intercept)/slope, 32.72));
			ll.addPoint(new Point((27.72-intercept)/slope, 27.72));
		}else if(5 == this.points.size()) {
			PolynomialSplineFunction func = this.splineFit();
			double [] extY = getExtremeYCord(); 	//maxY, minY
			int delta = (int)((extY[0] - extY[1])/8);
				ll.addPoint(new Point(func.value(extY[0]), extY[0]));
				ll.addPoint(new Point(func.value(extY[0]-delta*1), extY[0]-delta*1));
				ll.addPoint(new Point(func.value(extY[0]-delta*2), extY[0]-delta*2));
				ll.addPoint(new Point(func.value(extY[0]-delta*3), extY[0]-delta*3));
				ll.addPoint(new Point(func.value(extY[0]-delta*4), extY[0]-delta*4));
				ll.addPoint(new Point(func.value(extY[0]-delta*5), extY[0]-delta*5));
				ll.addPoint(new Point(func.value(extY[0]-delta*6), extY[0]-delta*6));
				ll.addPoint(new Point(func.value(extY[0]-delta*7), extY[0]-delta*7));
				ll.addPoint(new Point(func.value(extY[1]), extY[1]));

		}

		return ll;
	}
	public boolean isAssigned() {
		return assigned;
	}
	
	public void assign() {
		assigned = true;
	}
	
	private PolynomialSplineFunction splineFit() {
		//sort by point.y
		this.getPoints().sort(new Comparator<Point>()
		{
		    @Override
		    public int compare(Point o1, Point o2)
		    {
		        return (new Double(o1.y).compareTo(o2.y));
		    }
		});
		
		double [] x = new double [this.size()];
		double [] y = new double [this.size()];
		for(int j = 0; j < this.size(); j++) {
			//System.out.println("sorted="+this.getPoints().get(j));
			x[j] = this.getPoints().get(j).x;
			y[j] = this.getPoints().get(j).y;
		}
		
		SplineInterpolator spline = new SplineInterpolator();
		PolynomialSplineFunction func = spline.interpolate(y,x);	//x,y
		
		/*
		for(int yy = image_mat.rows()/2 ; yy < image_mat.rows() ; yy+=image_mat.rows()/9) {
			System.out.println("funk"+func.value(yy)+",+yy");	// xmax>x>xmin
			Imgproc.circle (
					image_mat,                 //Matrix obj of the image
					new Point(func.value(yy), yy),    //Center of the circle
					5,                    //Radius
					new Scalar(5, 25, 255),  //Scalar object for color
					8                      //Thickness of the circle
			);	 
		}
		*/
		return func;
	}
	
	public LaneLine extSplineFit() {
		PolynomialSplineFunction func = this.splineFit();
		LaneLine ll = new LaneLine();
		for(Point p : this.getPoints()) {
			ll.addPoint(new Point(func.value(p.y), p.y));
		}
		/*
		ll.addPoint(new Point(func.value(194.06), 194.06));
		ll.addPoint(new Point(func.value(127.98), 127.98));
		ll.addPoint(new Point(func.value(93.27), 93.27));
		ll.addPoint(new Point(func.value(71.87), 71.87));
		ll.addPoint(new Point(func.value(57.36), 57.36));
		ll.addPoint(new Point(func.value(46.87), 46.87));
		ll.addPoint(new Point(func.value(38.93), 38.93));
		ll.addPoint(new Point(func.value(32.72), 32.72));
		ll.addPoint(new Point(func.value(27.72), 27.72));
		*/
		return ll;
	}
	
	public class WrongParameterException extends Exception{
		public WrongParameterException(String message) {
			super(message);
		}
	}
	
	public double[] getExtremeYCord() {
		double[] yCoordinates= new double[] {0,this.getPoints().get(0).y}; //max, min
		
		for(Point p : this.getPoints()) {
			if(p.y>yCoordinates[0]) {
				yCoordinates[0] = p.y;
			}else if(p.y<yCoordinates[1]) {
				yCoordinates[1] = p.y;
			}
		}
		
		return yCoordinates;
	}
}
