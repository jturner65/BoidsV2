package Boids2_PKG.threadedSolvers.forceSolvers;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;


import Boids2_PKG.myBoidFlock;
import Boids2_PKG.boids.myBoid;
import Boids2_PKG.threadedSolvers.forceSolvers.base.myFwdForceSolver;
import base_UI_Objects.my_procApplet;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;

public class myOrigForceSolver extends myFwdForceSolver{
	public myOrigForceSolver(my_procApplet _p, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
		super(_p, _f,_flagInt, _isClk, _bAra);
	}
	
	//collect to center of local group
	@Override
	protected myVectorf frcToCenter(myBoid b){
		float wtSqSum = 0, wtDist;	
		myVectorf frcVec = new myVectorf();
		for(Float bd_k : b.neighLoc.keySet()){	
			wtDist = 1.0f/bd_k;//(bd_k*bd_k);
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(bd_k), b.coords), wtDist));
			//frcVec._add(myVectorf._mult(myVectorf._sub(b.neighLoc.get(b.neighbors.get(bd_k).ID), b.coords), wtDist));
			wtSqSum += wtDist;	
		}
		frcVec._div(wtSqSum);				
		return frcVec;
	}//frcToCenter

	//avoid collision, avoid predators within radius frcThresh
	@Override
	protected myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh){
		myVectorf frcVec = new myVectorf(), tmpVec;
		float subRes, wtSqSum = 0;
		for(Float bd_k : otherLoc.keySet()){	//already limited to those closer than colRadSq
			tmpVec = myVectorf._sub(b.coords,otherLoc.get(bd_k));
			subRes = 1.0f/(bd_k * tmpVec.magn);
			wtSqSum += subRes;
			frcVec._add(myVectorf._mult(tmpVec, subRes ));
		}
		frcVec._div(wtSqSum);	
		return frcVec;
	}//frcAvoidCol
	
	@Override
	protected myVectorf frcVelMatch(myBoid b){
		float dsq, wtSqSum = 0;
		myVectorf frcVec = new myVectorf();		
		for(Float bd_k : b.neighbors.keySet()){	
			if(bd_k>velRadSq){continue;}
			dsq = 1.0f/bd_k;
			wtSqSum += dsq;
			//frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity[0], b.velocity[0]), dsq));
			frcVec._add(myVectorf._mult(myVectorf._sub(b.neighbors.get(bd_k).velocity, b.velocity), dsq));
		}
		frcVec._div(wtSqSum == 0 ? 1 : wtSqSum);
		return frcVec;
	}//frcVelMatch
	
}//myOrigForceStencil