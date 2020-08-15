package ptolemy.actor.lib.image;

import java.awt.Dimension;
import java.util.Random;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.video.KalmanFilter;

import com.sun.pdfview.decode.Predictor;

/*
 * Implemented but not integrated yet, could be used in addition to lane detection algorithm as post-processing techniques 
 * to enhance accuracy by considering relationship between consecutive frames
 */
public class KalmanLaneTrack extends KalmanFilter{
	Mat transitionMatrix = new Mat();
	Mat preState = new Mat();
	Mat postState = new Mat();
	Mat noiseCov = new Mat();
	Mat measurementMatrix = new Mat();
	Mat measure = new Mat();
	Mat state = new Mat();
	HoughTransform htf = new HoughTransform(3); //measureparams

	/*
	public static void main (String args []) {
		nu.pattern.OpenCV.loadShared();
		float[] tM = {1,1,0,1
				,0,1,0,1
				,0,0,1,0
				,0,0,0,1};
		float[] initState = {300, 200, 0, 0};
		KalmanLaneTrack klt = new KalmanLaneTrack(2, 2, 0, CvType.CV_32F, tM, initState);
		//for(;;) {}
		for(int i=0; i<30; i++) {
			klt.printParameters(i);
			klt.prediction(i);
			klt.innovation(i);
			System.out.println("Iteration "+i+": A posteriori estimate= \n"+klt.postState.dump());
		}
			
	}
	*/
	
	
	public void printParameters(int i) {
		System.out.println("/*************************\n Iteration "+i+": Parameters");

		System.out.println("-transitionMatrix:A (pxp)"+this.get_transitionMatrix().size());
		System.out.println(this.get_transitionMatrix().dump()+"\n");
		
		System.out.println("-measurementMatrix:C (rxp)"+this.get_measurementMatrix().size());
		System.out.println(this.get_measurementMatrix().dump()+"\n");
		
		System.out.println("-pre-state:x[n] (px1)"+this.get_statePre().size());
		System.out.println(this.get_statePre().dump()+"\n");
		
		System.out.println("-post-state: (^x)(n)"+this.get_statePost().size());
		System.out.println(this.get_statePost().dump()+"\n");
		
		System.out.println("-noiseCov:Q (pxp)"+this.get_processNoiseCov().size());
		System.out.println(this.get_processNoiseCov().dump()+"\n");
		
		System.out.println("-measureNoiseCov: R ()"+this.get_measurementNoiseCov().size());
		System.out.println(this.get_measurementNoiseCov().dump()+"\n");

		System.out.println("-A posteriori Error Estimate Cov: P(n) (pxp)"+this.get_errorCovPost().size());
		System.out.println(this.get_errorCovPost().dump()+"\n");
		
		System.out.println("-Controlmatrix: (pxq)"+this.get_controlMatrix().size());
		System.out.println(this.get_controlMatrix().dump());
		
		System.out.println("Kalman Gain (pxr) "+this.get_gain().size());
		System.out.println(this.get_gain().dump());
	}
	
	// Standard Kalman Filter 
	//x(n+1) = Ax(n)+Bu(n)+Gv(n)
	// z(n) = Cx(n)+m(n) 
	//dynamParams = p, measureParams = r, controlParams = q, measurementMatrix = C, G = 1
	public KalmanLaneTrack(int dynamParams, int measureParams, int controlParams, int type, float [] tM, float[] initState) {
		super(dynamParams, measureParams, controlParams, type);
			

		//Systemmatrix A
		this.transitionMatrix = new Mat(dynamParams, dynamParams, CvType.CV_32F, new Scalar(0));
		this.transitionMatrix.put(0,0,tM);
		this.set_transitionMatrix(transitionMatrix);
		
		
		//initialize measurement
		this.measurementMatrix = Mat.eye(measureParams, dynamParams, CvType.CV_32F);
		//this.measurementMatrix.setTo(new Scalar(0));										//set all the pixels to 0.
		this.set_measurementMatrix(measurementMatrix);
		
		
		//Initial states matrix
		this.preState = new Mat(dynamParams ,1, CvType.CV_32F);
		for(int i=0; i<dynamParams; i++) {
			this.preState.put(i, 0, 0); //initState[i]
		}
		this.set_statePre(preState);
		
		
		this.postState = new Mat(dynamParams ,1, CvType.CV_32F);
		for(int i=0; i<dynamParams; i++) {
			this.postState.put(i, 0, initState[i]); //0
		}
		this.set_statePost(postState);

		
		//Initial System noise covariance matrix: Q
		this.noiseCov = Mat.eye(dynamParams, dynamParams, CvType.CV_32F);
		this.noiseCov = noiseCov.mul(noiseCov,1e-3);
		this.set_processNoiseCov(noiseCov);

		
		//Measurement noise covariance matrix: R
		Mat measureNoiseCov = Mat.eye(measureParams, measureParams,CvType.CV_32F); 	
		measureNoiseCov = measureNoiseCov.mul(measureNoiseCov,1e-4);//1e-1
		this.set_measurementNoiseCov(measureNoiseCov);

		
		
		//a-priori error estimate covariance matrix: (P-)(n)
		/*
		this.set_errorCovPre(Mat.eye(dynamParams, dynamParams ,CvType.CV_32F)
				.mul(Mat.eye(dynamParams, dynamParams ,CvType.CV_32F),0.1)); //Mat.zeros(new Size(dynamParams, dynamParams), CvType.CV_32F)
		System.out.println("/A priori Error Estimate Cov: (P-)(n) (pxp)");
		System.out.println(this.get_errorCovPost().dump());
		System.out.println("/\n");
		*/
		
		measure = Mat.zeros(new Size(1, measureParams), CvType.CV_32F);	//z[0]
		state = Mat.zeros(new Size(dynamParams, 1), CvType.CV_32F);		//x[0]

		
		//A posteriori error estimate covariance matrix: P(n) (pxp)
		Mat pErrorCov = Mat.eye(dynamParams, dynamParams ,CvType.CV_32F);
		pErrorCov=pErrorCov.mul(pErrorCov,0.1);
	    this.set_errorCovPost(pErrorCov);
		
		this.set_gain(Mat.zeros(new Size(measureParams, dynamParams), CvType.CV_32F));//Mat.eye(dynamParams, measureParams ,CvType.CV_32F));
		
		if(controlParams>0) {
			this.set_controlMatrix(Mat.eye(dynamParams, controlParams ,CvType.CV_32F));//Mat.zeros(new Size(dynamParams, controlParams), CvType.CV_32F));
		}else;
		
	}
	
	public void update(int i, double[] measurement) {
		this.printParameters(i);
		this.prediction(i);
		this.innovation(i, measurement);
		System.out.println("Iteration "+i+": A posteriori estimate= \n"+this.postState.dump());
	}
	
	private void prediction(int i) {
		//predicted state estimate: (^(x-)(n+1)) = A * (^(x)(n)) + B * u(n) 
		//(a-priori) predicted error covariance: (P-)(n) = A * P(n-1) * (A^T) + Q (done internally)
		this.predict();
		System.out.println("\n"+i+"th Prediction:");

		System.out.println("- (^x-)(n)");
		System.out.println(this.get_statePre().dump()+"\n");
		
		System.out.println("-A priori Error Estimate Cov: (P-)(n) (pxp)");
		System.out.println(this.get_errorCovPost().dump());
		System.out.println("\n");
	}
	
	private void innovation(int k, double[] measurement) {
		//measurement residual: y(n) = z(n) - C * (^(x-)(n))
		System.out.println(k+"th Innovation:");

		/*
		float [] hp = htf.nextStep();		
		for(int i = 0; i< hp.length; i++)
			System.out.println(hp[i]);
		
		for(int i = 0; i< this.measure.rows(); i++) {
			this.measure.put(i,0,hp[i]);	
		}
		*/
		
		System.out.println("-measurement"+measure.size());
		System.out.println(this.measure.dump());
		System.out.println("");

		Mat estimated = this.correct(this.measure);				//Updates the predicted state from the measurement.
		
		//updated state estimate: ^(x)(n) = ^(x-)(n) * K(n) * y(n)		
		System.out.println("-Updated state estimate ^x(n)");
		System.out.println(this.get_statePost().dump());
		System.out.println("");
		
		//updated error covariance: P(n) = (I - K(n)*C) * (P-)(n)
		System.out.println("-Updated error covariance P(n)");
		System.out.println(this.get_errorCovPost().dump());
		System.out.println("");
		
		//kalman gain: K(n) = (P-)(n) * (C^T) * (C * (P-)(n) * (C^T) ) ^ (-1)
		System.out.println("-Updated Kalman Gain K: (pxr) ");
		System.out.println(this.get_gain().dump());
		System.out.println("");
		
		Point newPoint = new Point();
		newPoint.x = estimated.get(0, 0)[0];
		newPoint.y = estimated.get(1, 0)[0];
		
		System.out.println("************************/\n");
		
	}

	//return two points
	public static class HoughTransform{ 
		private float [] ans;
		public HoughTransform(int dimension) {
			ans = new float [dimension];			
		}
		float [] nextStep() {
			Random r = new Random();
			int low = 99;
			int high = 101;
			
			for(int i = 0 ; i < ans.length; i++) {
				if (i==0)
					ans[i] = 10000;
				else
					ans[i] = r.nextInt(high-low) + low;
			}

			return ans; 
		}
		
	}
}
