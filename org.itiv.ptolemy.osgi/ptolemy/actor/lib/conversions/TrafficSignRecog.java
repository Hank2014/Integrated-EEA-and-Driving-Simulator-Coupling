/**
 * Module to detect traffic sing from input image and recognize speed limit sign.
 * */
package ptolemy.actor.lib.conversions;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.github.sarxos.webcam.Webcam;

import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.image.ImageUtils;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.AWTImageToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class TrafficSignRecog extends TypedAtomicActor {
  private static final Logger logger = Logger.getLogger(TrafficSignRecog.class);
  public TrafficSignRecog(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);
    input = new TypedIOPort(this, "input", true, false);
    output = new TypedIOPort(this, "output", false, true);
    detection = new TypedIOPort(this, "detection", false, true);
    input.setTypeEquals(BaseType.GENERAL);
    output.setTypeEquals(BaseType.INT);  
    detection.setTypeEquals(BaseType.GENERAL);
  }
  
  
    Tesseract instance = new Tesseract(); //
    
    
    /** Initialize the OCR and OpenCV.
     *  OCR engine is defined with "TessOcrEngineMode".
     */  
  public void initialize() throws IllegalActionException {
      super.initialize();
      File tess=LoadLibs.extractTessResources("tessdata");      
      instance.setDatapath(tess.getAbsolutePath()); 
      instance.setLanguage("eng");
      System.setProperty("jna.encoding", "UTF8");
   // instance.setOcrEngineMode(TessAPI.TessOcrEngineMode.OEM_DEFAULT);
      instance.setOcrEngineMode(TessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
	  nu.pattern.OpenCV.loadShared();
	  PropertyConfigurator.configure("resources\\log4j.properties");
  }
  
  
  
  
  
  public void fire() throws IllegalActionException {
	 if (input.hasToken(0)) {
	  long startTime=System.currentTimeMillis(); 
	  super.fire();
	  
         //get input token from input port
	     Object inputValue = new Object();
	     try {
	     inputValue = input.get(0);
	     }
	     finally {
	       
	     }

	   // Import .xml file for traffic sign detection. Note: for simulation in PREEvision is a complete path is required.
	   // For simulation in Ptolemy II: complete path or $classpath$\\org.itiv.ptolemy.osgi\\lbpCascade.xml 
	   CascadeClassifier signalDetector =new CascadeClassifier(new File(System.getProperty("user.dir")+"\\lbpCascade.xml").getAbsolutePath());  
	     MatOfRect signDetections = new MatOfRect();
	       BufferedImage image2_bufferedimage = getRenderedImage(
	                 ((AWTImageToken) inputValue).getValue());
	         Mat image_mat = img2Mat(image2_bufferedimage);
	         
	       //Detect traffic sign from input image
	         try{
	           signalDetector.detectMultiScale(image_mat, signDetections,1.15,3,0,new Size(20,20),new Size());
	         }catch (Exception ex) {
					System.out.println("Error");
				}

	        //Detects objects of different sizes in the input image. The detected objects are returned as a list of rectangles.
	         System.out.println(String.format("Detected %s signs", signDetections.toArray().length));
	         
	         for (Rect rect : signDetections.toArray()) {    //Drawing Rectangle in the image
	           Imgproc.rectangle(image_mat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
	         }

	         AWTImageToken _detection = new AWTImageToken(matToBufferedImage(image_mat));
	         detection.send(0, _detection);
	      
	         String result = "";
	         int speed = 0;

	         
	           

	         Rect[] rect1 = signDetections.toArray();
	         if (rect1 != null && rect1.length > 0){
	           Rect rect = rect1[0];
	                   Mat lett_mat2 = image_mat.submat(rect); //The detected traffic sign
	                   /*
	                   Mat lett_matBlack = ImageUtils.colorFilterBlack(lett_mat2);	//after color HSV space 
	                   Mat lett_matGaussian =  ImageUtils.gaussianBlur(lett_mat2);
	                   Mat lett_matCanny = ImageUtils.cannyEdge(lett_mat2);
	                   Mat lett_matCG = ImageUtils.cannyEdge(ImageUtils.gaussianBlur(lett_mat2));
	                   
	                   
	                   Imgproc.resize(lett_mat2, lett_mat2, new Size(10*lett_mat2.size().width, 10*lett_mat2.size().height)); 
	                   Imgproc.resize(lett_matBlack, lett_matBlack, new Size(10*lett_matBlack.size().width, 10*lett_matBlack.size().height));
	                   Imgproc.resize(lett_matGaussian, lett_matGaussian, new Size(10*lett_matGaussian.size().width, 10*lett_matGaussian.size().height));
	                   Imgproc.resize(lett_matCanny, lett_matCanny, new Size(10*lett_matCanny.size().width, 10*lett_matCanny.size().height));
	                   Imgproc.resize(lett_matCG, lett_matCG, new Size(10*lett_matCG.size().width, 10*lett_matCG.size().height));
*/
	                   BufferedImage letter_bufferedimage = matToBufferedImage(lett_mat2);
	                   
	                   double x = letter_bufferedimage.getWidth();
	                   double y = letter_bufferedimage.getHeight();
	                   BufferedImage lette_bufferedimage = letter_bufferedimage.getSubimage ((int)Math.round(x * 0.22), 
	                                 (int)Math.round(y * 0.25), (int)Math.round(x * 0.60), (int)Math.round(y * 0.60));
	                   // Cut the detected traffic sign so that the Tess4j can read the content of the traffic sign better
	                   // Start with x*0.25, y*0.25, end with x*0.53, y*0.53
	                   
	                   //HighGui.imshow("original", lett_mat2);
	                   //HighGui.imshow("lett_matBlack", lett_matBlack);
	                   //HighGui.imshow("lett_matGaussian", lett_matGaussian);
	                   //HighGui.imshow("lett_matCanny", lett_matCanny);
	                   //HighGui.imshow("lett_matCG", lett_matCG);
	                   
	                   //HighGui.waitKey();
	                   try{
	                     result = instance.doOCR(lette_bufferedimage);
	                     speed = getNum(result);  //Extract the integer from string and set the output interval
	                     if (speed > 0) {
	                     Token out = new IntToken(speed);  // Send the detected speed limit with in a token
	                     output.send(0, out);
	 					 
	                     System.err.println(" result"+ ": " + speed );
	                    }
		                     /*File outputfile = new File("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\laneassistantTest\\TSR"+in+"correct.jpg");
				           		try {
				           			ImageIO.write(lette_bufferedimage, "png", outputfile);
				           		}catch (IOException e) {
				           			e.printStackTrace();
				           	   }
		                 }else {
		                	 File outputfile = new File("C:\\Users\\hanky\\Downloads\\Simulators\\OpenDS Free\\screenshots\\laneassistantTest\\TSR"+in+".jpg");
				           		try {
				           			ImageIO.write(lette_bufferedimage, "png", outputfile);
				           		}catch (IOException e) {
				           			e.printStackTrace();
				           	   } 
		                 }*/
	                   }
	                   catch (TesseractException e) {                     
	                     e.printStackTrace();
	                   }
	       
	 		   

	       }
	           long endTime=System.currentTimeMillis(); 
	           //System.err.println("Time of running TSR:  "+(endTime-startTime)+"ms");   
	   }
  }

  
  
  
  /**
   * Cut a ellipse from a image
   * 
   * @param BufferedImage
   * @return BufferedImage
   */  
  public BufferedImage getEllipse(BufferedImage bi1) {
    int x = bi1.getWidth();
    int y = bi1.getHeight();
    BufferedImage bi2 = new BufferedImage((int)Math.round(x), (int)Math.round(y),
            BufferedImage.TYPE_INT_RGB);
    Ellipse2D.Double shape = new Ellipse2D.Double(x*0.19, y*0.19, x*0.65, y*0.65);
    Graphics2D g2 = bi2.createGraphics();
    g2.setBackground(Color.WHITE);

    g2.fill(new Rectangle(bi2.getWidth(), bi2.getHeight()));
    g2.setClip(shape);
    g2.drawImage(bi1, 0, 0, null);
    g2.dispose();
    return bi2;
  }
  
  

  
  
  /**
   * Extract integer from string
   * 
   * @param String
   * @return integer
   */  
  public static Integer getNum(String str) {  
    String dest1 = "";  
    int dest = 0;  
    if (str != null && str != "") { 
    //  System.err.println(" String "+ ": " + str );
        dest1 = str.replaceAll("[^0-9]","");   
        if (dest1 != null && !dest1.isEmpty()) {
     //     System.err.println(" Digits "+ ": " + dest1 );
          dest = Integer.parseInt(dest1);         
        }else {
          return 0;
        }       
    }
    if ((dest == 10) ||(dest == 20) || (dest ==30 ) || (dest ==40 ) || (dest ==50 )|| (dest ==60 ) || (dest ==70 )
        || (dest ==80 ) || (dest ==100 ) || (dest ==120 ) || (dest ==130 )) {  
    	// Check if the result match the speed limits
      return dest;                
    }else {
      return 0;
    }
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
  
  
  
  
  
  /** Convert an AWT Image object to a BufferedImage.
   *  @param in An AWT Image object.
   *  @return a BufferedImage.
   */
  public BufferedImage getRenderedImage(Image in) {
    BufferedImage out = new BufferedImage(in.getWidth(null),
            in.getHeight(null), BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = out.createGraphics();
    g2.drawImage(in, 0, 0, null);
    g2.dispose();
    return out;       
}
  
  
  
  /**
   * Converts/writes a Mat into a BufferedImage.
   * 
   * @param matrix Mat of type CV_8UC3 or CV_8UC1
   * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY
   */
  public BufferedImage matToBufferedImage(Mat matrix) {
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
  
  
  
  /** Check that input port has at least one token, and if
   *  so, return the result of the superclass prefire() method.
   *  Otherwise, return false.
   *  @return True if there inputs available on input port.
   *  @exception IllegalActionException If the base class throws it.
   */
   @Override
  public boolean prefire() throws IllegalActionException {
      if (!input.hasToken(0)) {
    	  return false;
      }

      return super.prefire();
  }
  
   ///////////////////////////////////////////////////////////////////
   ////                     ports and parameters                  ////

   /** The input trigger port */
   public TypedIOPort input;

   /** The output port. */
   public TypedIOPort output;
   
   /** The detection port. */
   public TypedIOPort detection;
   
  
}