package rmiinterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import ptolemy.actor.lib.rmi.FrameRMIActor;
import ptolemy.actor.lib.rmi.RMIClientCenter;
import ptolemy.actor.lib.rmi.RMIUpdatable;
import ptolemy.actor.lib.rmi.RMIUpdatableImpl;


public class RMICallBackListener extends UnicastRemoteObject implements RMIListener {

	private static final long serialVersionUID = -7222558137524034774L;
	ArrayList<RMIUpdatable> _list = new ArrayList<RMIUpdatable>();
	
	public RMICallBackListener() throws RemoteException {
		
	}

	public void addListener(RMIUpdatableImpl listener) {
		this._list.add(listener);
	}
	
	//important for image communication
	@Override
	public void sendImageInByteArray(byte[] b) throws IOException {
		for(RMIUpdatable listenerClass: _list) {
			if(listenerClass instanceof FrameRMIActor) {
				((FrameRMIActor) listenerClass).setImgByteArray(b);
			}
		}
	}

	//unused
	@Override
	public void sendSteeringAngle(float f) throws RemoteException {
		for(RMIUpdatable listenerClass: _list) {
			if(listenerClass instanceof RMIClientCenter) {
				((RMIClientCenter) listenerClass).setAnglefromDS(f);
			}
		}	
	}


	

}
