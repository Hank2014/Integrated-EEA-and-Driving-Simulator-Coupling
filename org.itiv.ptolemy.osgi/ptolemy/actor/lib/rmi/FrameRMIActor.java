package ptolemy.actor.lib.rmi;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.imageio.ImageIO;

import org.apache.log4j.PropertyConfigurator;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.AWTImageToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import rmiinterface.RMIObjectInterface;
import rmiinterface.RMICallBackListener;

public class FrameRMIActor extends RMIUpdatableImpl{
	private RMICallBackListener tListener;
	int i = 1; 
	public FrameRMIActor(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
		super(container,name);
		screenshot = new TypedIOPort(this, "image", false, true);
		screenshot.setTypeEquals(BaseType.OBJECT);
	

	}
	
	///////////////////////////////////////////////////////////////////
	////                         public methods                    ////
	@Override
	public void initialize() throws IllegalActionException{
		super.initialize();
		PropertyConfigurator.configure("resources\\log4j.properties");
		try {
			stub = (RMIObjectInterface)Naming.lookup("rmi://127.0.0.1/opendsSim");
			

			//stub.startSimulator(((BooleanToken)_isHeadless.getToken()).booleanValue());				
			
			while(!stub.getInitializationFinished()) {
				try{
					Thread.sleep(2000);
				}catch(Exception e) {}
			};
			tListener = new RMICallBackListener();
			tListener.addListener(this);
			stub.addRMIListener(tListener);
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
    public boolean prefire() throws IllegalActionException {
		if(!screenshotReady) {
			return false;
		}
		return super.prefire();
	}
	
	
	@Override
	public void fire() throws IllegalActionException{
		screenshot.send(0, new AWTImageToken(_image));
		screenshotReady = false;
		super.fire();
	}
	
	public void setImgByteArray(byte[] b) throws IOException {
		//this._imageArray = b;
		sendImage(b);
	}
	
	public void sendImage(byte[] b) throws IOException {
		_image = ImageIO.read(new ByteArrayInputStream(b));
		if(_image!=null) {
			screenshotReady = true;
		}
		
	}
	
	///////////////////////////////////////////////////////////////////
	////                     ports and parameters		           ////
	
	//inputs

	//outputs
	public TypedIOPort screenshot;
	
	
	
	///////////////////////////////////////////////////////////////////
	////                         private members                   ////
	private RMIObjectInterface stub;
	public static boolean screenshotReady;
	private Image _image;
	
	//private byte[] _imageArray = null;


}
