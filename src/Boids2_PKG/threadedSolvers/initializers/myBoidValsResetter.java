package Boids2_PKG.threadedSolvers.initializers;

import java.util.List;
import java.util.concurrent.Callable;

import Boids2_PKG.myBoidFlock;
import Boids2_PKG.myBoids3DWin;
import Boids2_PKG.boids.myBoid;
import base_Math_Objects.vectorObjs.floats.myPointf;

//reset all values at start of timestep
public class myBoidValsResetter implements Callable<Boolean> {
	public List<myBoid> bAra;								//boid ara being worked on
	public myBoidFlock f, pry;
	private int flagInt;						//bitmask of current flags
	public boolean[] stFlags; // doHunt, doSpawn;							//is torroidal
	public final int 		
			doHunt 		= 0,		//idxs in local flags array
			doSpawn 	= 1;
	public final int[] stFlagIDXs = new int[]{myBoids3DWin.flkHunt, myBoids3DWin.flkSpawn};

	public myBoidValsResetter(myBoidFlock _f,myBoidFlock _pry,int _flagInt,  List<myBoid> _bAra) {
		f = _f; pry = _pry; bAra=_bAra;
		flagInt = _flagInt;
		setStFlags();		
	}
	//TODO set these externally when/if eventually recycling threads
	public void setStFlags(){
		stFlags = new boolean[stFlagIDXs.length];
		for(int i =0;i<stFlagIDXs.length;++i){stFlags[i] = (((flagInt>>stFlagIDXs[i]) & 1) == 1);} 
	} 
	
	public void run(){	
		for(myBoid b:bAra){	b.forces.set(myPointf.ZEROPT); b.clearNeighborMaps();	}
		if (stFlags[doHunt] && (f!=pry)){	for(myBoid b:bAra){b.clearHuntMaps();			}}
		if (stFlags[doSpawn]) {	for(myBoid b : bAra){	b.ptnWife.clear();}}	
	}//run()	
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
}