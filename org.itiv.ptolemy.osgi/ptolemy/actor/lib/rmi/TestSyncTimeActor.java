package ptolemy.actor.lib.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import org.python.modules.synchronize;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.FloatToken;
import ptolemy.data.type.BaseType;
import ptolemy.domains.sr.kernel.SRDirector;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import rmiinterface.RMIObjectInterface;
import rmiinterface.RMICallBackListener;

public class TestSyncTimeActor extends RMIUpdatableImpl{
	private RMICallBackListener tListener;
	
	public TestSyncTimeActor(CompositeEntity container, String name)
	            throws NameDuplicationException, IllegalActionException {
		super(container,name);
		
		
		speed = new TypedIOPort(this,"speed",false,true);
		speed.setTypeEquals(BaseType.FLOAT);
		heading = new TypedIOPort(this,"angle",false,true);
		heading.setTypeEquals(BaseType.FLOAT);

		
		logicalTime = new TypedIOPort(this,"time",false,true);
		logicalTime.setTypeEquals(BaseType.FLOAT);
		

	}
	
	
	///////////////////////////////////////////////////////////////////
	////                         public methods                    ////
	@Override
	public void initialize() throws IllegalActionException{
		super.initialize();
		try {
			stub = (RMIObjectInterface)Naming.lookup("rmi://127.0.0.1/opendsSim");
			connectionEstablished = true;
			stub.startSimulator(); //not headless
			
			while(!stub.getInitializationFinished()) {};
			
			SRDirector srd = (SRDirector)getDirector();
			stub.setMinTimeDiffForUpdate((float)(srd.periodValue()));
			stub.setEngineOn();
			
			//for testing lane detecion algorithm only
			tListener = new RMICallBackListener();
			tListener.addListener(this);
			stub.addRMIListener(tListener);
		}catch (RemoteException r) {
			r.printStackTrace();
		}catch (NotBoundException r) {
			r.printStackTrace();
		}catch (MalformedURLException r) {
			r.printStackTrace();
		}
	}
	
	@Override
    public boolean prefire() throws IllegalActionException {		
		try {
			while(!stub.isSimPaused());		
		}catch (RemoteException e) {
			e.printStackTrace();
		}
        return super.prefire();
    }
	
	@Override
	public void fire() throws IllegalActionException{
		super.fire();
		try{	
			speedIstToken = new FloatToken(stub.getCarSpeed());
			speed.send(0, speedIstToken);

			updateCarInfo();
			speedShould = -0.6f;
			sendCarAcceleration((float)speedShould);
	
			resumeSimulator();

		}catch(RemoteException e) {
			e.printStackTrace();
		}
	
	}

	public void updateCarInfo() {
		try {
			if(connectionEstablished) {
				//stub.setCameraUpdatedFrameNotRead(false);
				
				logicalTime.send(0, new FloatToken(stub.getLogicalTime()));
				heading.send(0, new FloatToken(stub.getCarHeading()));

			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendCarAcceleration(Float acceleration) {
		try {
			stub.setCarAcceleratePedalValue(acceleration);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendCarSteer(Float angle) {
		try {
			stub.setCarSteering(angle);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void resumeSimulator() throws RemoteException {
		stub.resumeSimulator();
	}
	
	public void initialized() {
		synchronized (paused) {
			paused.notifyAll();			
		}
	}
	///////////////////////////////////////////////////////////////////
	////                     ports and parameters		           ////
	//inputs

	//outputs
	public TypedIOPort speed;
	public TypedIOPort heading;
	public TypedIOPort logicalTime;

	///////////////////////////////////////////////////////////////////
	////                         private members                   ////
	private RMIObjectInterface stub;
	private boolean connectionEstablished = false;
	private FloatToken speedIstToken;
	private double speedShould = 0.0;
	private double angleShould = 0.0f;
	
	// The lock that monitors the 
	private Object paused = new Object();
	private long lastprefireTime = 0;

}

