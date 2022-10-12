package Boids2_PKG.threadedSolvers.forceSolvers;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import Boids2_PKG.flocks.myBoidFlock;
import Boids2_PKG.flocks.boids.myBoid;
import Boids2_PKG.threadedSolvers.forceSolvers.base.myFwdForceSolver;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;

public class myLinForceSolver extends myFwdForceSolver{
	public myLinForceSolver(GUI_AppManager _AppMgr, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
		super(_AppMgr,_f,_flagInt, _isClk, _bAra);
	}
	
	//collect to center of local group
	@Override
	protected myVectorf frcToCenter(myBoid b){
		float wtSqSum = 0, wtDist;	
		myVectorf frcVec = new myVectorf();
		for(Float bd_k : b.neighLoc.keySet()){	
			wtDist = bd_k;
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(bd_k), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh - scale avoidance force by distThresh
	@Override
	protected myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh){
		myVectorf frcVec = new myVectorf(), tmpVec;
		float subRes;
		for(Float bd_k : otherLoc.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVectorf._sub(b.coords,otherLoc.get(bd_k));
			subRes = (frcThresh - bd_k); 	//old
			frcVec._add(myVectorf._mult(tmpVec, subRes ));
		}
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVectorf frcVelMatch(myBoid b){
		float dsq;
		myVectorf frcVec = new myVectorf();		
		for(Float bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq=(velRadSq-bd_k);
			//frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity, b.velocity), dsq));
		}
		return frcVec;
	}
	
	
}//myLinForceStencil