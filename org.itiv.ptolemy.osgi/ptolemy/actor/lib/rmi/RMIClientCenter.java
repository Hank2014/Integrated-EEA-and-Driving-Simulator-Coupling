package ptolemy.actor.lib.rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.FloatToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.domains.sr.kernel.SRDirector;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import rmiinterface.RMIObjectInterface;
import rmiinterface.RMICallBackListener;


public class RMIClientCenter extends RMIUpdatableImpl{

	
	public RMIClientCenter(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
		super(container,name);
		desiredAcceleration = new TypedIOPort(this,"desiredAcceleration",true,false);
		desiredAcceleration.setTypeEquals(BaseType.DOUBLE);

		desiredSteeringValue = new TypedIOPort(this, "desiredSteer", true, false);
		desiredSteeringValue.setTypeEquals(BaseType.DOUBLE);
		
		currentSpeed = new TypedIOPort(this,"speed",false,true);
		currentSpeed.setTypeEquals(BaseType.FLOAT);

		latOffFrontC  = new TypedIOPort(this,"latOffFrontC",false,true);
		latOffFrontC.setTypeEquals(BaseType.DOUBLE);
		yawRateFild  = new TypedIOPort(this,"yawRateFild",false,true);
		yawRateFild.setTypeEquals(BaseType.FLOAT);
		speedLateral  = new TypedIOPort(this,"speedLateral",false,true);
		speedLateral.setTypeEquals(BaseType.FLOAT);
		
		//steeringAngle = new TypedIOPort(this,"steered",false,true);
		//steeringAngle.setTypeEquals(BaseType.FLOAT);
		
		logicalTime = new TypedIOPort(this,"time",false,true);
		logicalTime.setTypeEquals(BaseType.FLOAT);
		logicalTime.setMultiport(true);
		
		_testScenario = new FileParameter(this, "testFile");
		
		simSpeed = new Parameter(this, "simulation clock speed");
		simSpeed.setTypeEquals(BaseType.FLOAT);
		simSpeed.setExpression("1.0f");

	}
	
	///////////////////////////////////////////////////////////////////
	////                         public methods                    ////
	@Override
	public void initialize() throws IllegalActionException{
		super.initialize();
		
		PropertyConfigurator.configure("resources\\log4j.properties");
		if(getDirector() instanceof SRDirector ) {
			srDir = (SRDirector)getDirector();
		}else {
			logger.error("wrong director!");
			System.exit(0);
		} 
	
		try {
			stub = (RMIObjectInterface)Naming.lookup("rmi://127.0.0.1/opendsSim");
			
			if(_testScenario.getExpression() != "") {
				stub.setTestFileName(_testScenario.getExpression());
			}
			
			stub.startSimulator();				

			connectionEstablished = true;
			while(!stub.getInitializationFinished()) {}
			stub.setMinTimeDiffForUpdate((float)srDir.periodValue());
			stub.setEngineOn();
			
			//for testing lane detecion algorithm only
			tListener = new RMICallBackListener();
			tListener.addListener(this);
			stub.addRMIListener(tListener);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
    public boolean prefire() throws IllegalActionException {
		logger.info("prefire=:"+new DecimalFormat("#.##").format(getDirector().getModelTime().getDoubleValue()));	

		try {
			while(!stub.isSimPaused()) {}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
		return super.prefire();
	}
	
	
	@Override
	public void fire() throws IllegalActionException{	
		logger.info("fire=:"+new DecimalFormat("#.##").format(getDirector().getModelTime().getDoubleValue()));	
		updateCarInfo();
		
		if(desiredAcceleration.hasToken(0)) {
			acceleration = ((DoubleToken) (desiredAcceleration).get(0)).doubleValue();
			sendCarAcceleration((float)acceleration);
			logger.info("acceleration="+acceleration);
		}
		if(desiredSteeringValue.hasToken(0)) {
			if(angle*previousAngle >=0) {
				angle = ((DoubleToken)desiredSteeringValue.get(0)).doubleValue();
				sendCarSteer((float)angle);
				logger.debug("actualAngle=" +angle);
			}	
				previousAngle = angle;
		}
		
		try {
			resumeSimulator();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		super.fire();
	}
	
	public void updateCarInfo() {
		try {
			if(connectionEstablished) {
				logicalTime.send(0, new FloatToken(stub.getLogicalTime()));
		
				FloatToken angleIstToken = new FloatToken(stub.getCarHeading());
				
				//steerIstToken = new FloatToken(steeringAngleFromDS);
				//steeringAngle.send(0, steerIstToken);
				
				FloatToken speedIstToken = new FloatToken(stub.getCarSpeed());
				currentSpeed.send(0, speedIstToken);
				
				float [] ODRelatedInfo = stub.getODInfo();	//float LatOffFrontC, float YawRateFild, float speedLateral
				latOffFrontC.send(0, new FloatToken(ODRelatedInfo[0]));
				yawRateFild.send(0, new FloatToken(ODRelatedInfo[1]));
				speedLateral.send(0, new FloatToken(ODRelatedInfo[2]));
				
			}
		}catch (Exception e) {
			logger.fatal("start simulator failed!");
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
		logger.info("resumedSimulator!");
		stub.resumeSimulator();
	}
	
	public void setAnglefromDS(float f) {
		//this.steeringAngleFromDS = f;
	}
	
	///////////////////////////////////////////////////////////////////
	////                     ports and parameters		           ////
	
	//inputs
	public TypedIOPort desiredAcceleration;
	public TypedIOPort desiredSteeringValue;
	
	//outputs
	public TypedIOPort currentSpeed;
	public TypedIOPort latOffFrontC;
	public TypedIOPort yawRateFild;
	public TypedIOPort speedLateral;
	
	public TypedIOPort logicalTime;
	//public TypedIOPort steeringAngle;
	private FileParameter _testScenario;
	private Parameter simSpeed;

	///////////////////////////////////////////////////////////////////
	////                         private members                   ////
	private static final Logger logger = Logger.getLogger(RMIClientCenter.class);
	private RMICallBackListener tListener;
	private RMIObjectInterface stub;
	private boolean connectionEstablished = false;
	
	private double acceleration = 0.0;
	private double angle = 0.0;
	private double previousAngle = 0.0;
	SRDirector srDir;
	//Object obj = new Object();
	//boolean simPaused = true;
}
