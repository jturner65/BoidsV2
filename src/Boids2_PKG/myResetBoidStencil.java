package Boids2_PKG;

import java.util.List;
import java.util.concurrent.Callable;

//reset all values at start of timestep
public class myResetBoidStencil implements Callable<Boolean> {
	public Boids_2 p;
	public List<myBoid> bAra;								//boid ara being worked on
	public myBoidFlock f, pry;
	private int flagInt;						//bitmask of current flags
	public boolean[] stFlags; // doHunt, doSpawn;							//is torroidal
	public final int 		
			doHunt 		= 0,		//idxs in local flags array
			doSpawn 	= 1;
	public final int[] stFlagIDXs = new int[]{myBoids3DWin.flkHunt, myBoids3DWin.flkSpawn};

	public myResetBoidStencil(Boids_2 _p, myBoidFlock _f,myBoidFlock _pry,int _flagInt,  List<myBoid> _bAra) {
		p = _p;	f = _f; pry = _pry; bAra=_bAra;
		flagInt = _flagInt;
		setStFlags();		
	}
	//TODO set these externally when/if eventually recycling threads
	public void setStFlags(){
		stFlags = new boolean[stFlagIDXs.length];
		for(int i =0;i<stFlagIDXs.length;++i){stFlags[i] = (((flagInt>>stFlagIDXs[i]) & 1) == 1);} 
	} 
	
	public void run(){	
		for(myBoid b:bAra){	b.forces[0].set(myPointf.ZEROPT); b.clearNeighborMaps();	}
		if (stFlags[doHunt] && (f!=pry)){	for(myBoid b:bAra){b.clearHuntMaps();			}}
		if (stFlags[doSpawn]) {	for(myBoid b : bAra){	b.ptnWife.clear();}}	
	}//run()	
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}
}