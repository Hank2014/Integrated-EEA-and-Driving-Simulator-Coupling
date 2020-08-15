package rmiinterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIListener extends Remote{
	//void simInitialized() throws RemoteException;
	//void update(Object observable, Object updateMsg) throws RemoteException;
	void sendImageInByteArray(byte[] b) throws RemoteException;
	void sendSteeringAngle(float f) throws RemoteException;
}
