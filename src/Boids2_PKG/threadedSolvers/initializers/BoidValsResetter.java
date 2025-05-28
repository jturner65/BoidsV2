package Boids2_PKG.threadedSolvers.initializers;

import java.util.List;
import java.util.concurrent.Callable;

import Boids2_PKG.flocks.BoidFlock;
import Boids2_PKG.flocks.boids.Boid;
import Boids2_PKG.ui.Boids_3DWin;
import base_Math_Objects.vectorObjs.floats.myPointf;

/**
 * reset all values at start of timestep
 * @author John Turner
 *
 */
public class BoidValsResetter implements Callable<Boolean> {
	public List<Boid> bAra;								//boid ara being worked on
	public BoidFlock f;
	private int flagInt;						//bitmask of current flags
	public boolean[] stFlags; // doHunt, doSpawn;							//is torroidal
	public final int 		
			doHunt 		= 0,		//idxs in local flags array
			doSpawn 	= 1;
	public final int[] stFlagIDXs = new int[]{Boids_3DWin.flkHunt, Boids_3DWin.flkSpawn};

	public BoidValsResetter(BoidFlock _f, int _flagInt,  List<Boid> _bAra) {
		f = _f; bAra=_bAra;
		flagInt = _flagInt;
		setStFlags();		
	}
	//TODO set these externally when/if eventually recycling threads
	public void setStFlags(){
		stFlags = new boolean[stFlagIDXs.length];
		for(int i =0;i<stFlagIDXs.length;++i){stFlags[i] = (((flagInt>>stFlagIDXs[i]) & 1) == 1);} 
	} 
	
	public void run(){	
		for(Boid b:bAra){	b.forces.set(myPointf.ZEROPT); b.clearNeighborMaps();	}
		if (stFlags[doHunt]){	for(Boid b:bAra){b.clearHuntMaps();			}}
		if (stFlags[doSpawn]) {	for(Boid b : bAra){	b.posMate.clear();}}	
	}//run()	
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
}