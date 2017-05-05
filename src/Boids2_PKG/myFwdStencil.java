package Boids2_PKG;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

public abstract class myFwdStencil implements Callable<Boolean> {

	//an overlay for calculations to be used to determine forces acting on a creature
	public Boids_2 p;
	//protected flkVrs fv;
	private myFlkVars fv;
	protected List<myBoid> bAra;								//boid being worked on
	protected myBoidFlock f;
	myVectorf dampFrc;
	protected float velRadSq, predRadSq, neighRadSq, colRadSq;				
	private int flagInt;						//bitmask of current flags
	public boolean[] stFlags; 
	public final int 		
		flkCenter		= 0,
        flkVelMatch		= 1,
        flkAvoidCol		= 2,
        flkWander		= 3,
        flkAvoidPred 	= 4,
        flkHunt			= 5,
        attractMode 	= 6;
	
	public static final int[] stFlagIDXs = new int[]{
		myBoids3DWin.flkCenter,
		myBoids3DWin.flkVelMatch,
		myBoids3DWin.flkAvoidCol,
		myBoids3DWin.flkWander,
		myBoids3DWin.flkAvoidPred,
		myBoids3DWin.flkHunt,
		myBoids3DWin.attractMode};
	
	private boolean addFrc;

	public myFwdStencil(Boids_2 _p, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
		p = _p;	f = _f;fv = f.flv; bAra=_bAra;
		flagInt = _flagInt;
		setStFlags();		
		addFrc = _isClk;
		velRadSq = fv.velRad * fv.velRad; 		
		predRadSq = fv.predRad * fv.predRad;
		neighRadSq = fv.nghbrRad* fv.nghbrRad;
		colRadSq = fv.colRad * fv.colRad;
		dampFrc = new myVectorf();
	}	
	
	//TODO set these externally when/if eventually recycling threads
	public void setStFlags(){
		stFlags = new boolean[stFlagIDXs.length];
		for(int i =0;i<stFlagIDXs.length;++i){stFlags[i] = (((flagInt>>stFlagIDXs[i]) & 1) == 1);} 
	} 
	
	protected abstract myVectorf frcToCenter(myBoid b); 
	protected abstract myVectorf frcAvoidCol(myBoid b, ConcurrentSkipListMap<Float,myPointf> otherLoc, float frcThresh);
	protected abstract myVectorf frcVelMatch(myBoid b);

	protected myVectorf frcWander(myBoid b){ return new myVectorf((float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),(float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass),(float)ThreadLocalRandom.current().nextDouble(-b.mass,b.mass));}			//boid independent

	//pass arrays since 2d arrays are arrays of references in java
	private myVectorf setFrcVal(myVectorf frc, float[] multV, float[] maxV, int idx){
		frc._mult(multV[idx]);
		if(frc.magn > maxV[idx]){	frc._mult(maxV[idx]/frc.magn);}
		return frc;		
	}
	//returns a vector denoting the environmental velocity like wind or fluid
	protected myVectorf getVelAtLocation(myVectorf _loc){
		//TODO stam fluid lookup could happen here
		return new myVectorf();
	}

	
	
	//all inheriting classes use the same run
	public void run(){
		//if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){//add click force : overwhelms all forces - is not scaled
		if (addFrc){
			for(myBoid b : bAra){b.forces._add(p.mouseForceAtLoc(b.coords, stFlags[attractMode]));}
		}
		//if(!stFlags[singleFlock]){
			if (stFlags[flkHunt]) {//go to closest prey
				if (stFlags[flkAvoidPred]){//avoid predators
					for(myBoid b : bAra){
						if (b.predFlkLoc.size() !=0){//avoid predators if they are nearby
							//b.forces._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc, predRadSq), fv.wts, fv.maxFrcs,fv.wFrcAvdPred));	//flee from predators
							b.forces._add(setFrcVal(frcAvoidCol(b, b.predFlkLoc, predRadSq), fv.wts, fv.maxFrcs,fv.wFrcAvdPred));	//flee from predators
							if(b.canSprint()){ 
								//add greater force if within collision radius
								//b.forces._add(setFrcVal(frcAvoidCol(b, b.predFlk, b.predFlkLoc,  colRadSq),fv.wts, fv.maxFrcs,fv.wFrcAvdPred));
								b.forces._add(setFrcVal(frcAvoidCol(b, b.predFlkLoc,  colRadSq),fv.wts, fv.maxFrcs,fv.wFrcAvdPred));
								//expensive to sprint, hunger increases
								--b.starveCntr;
							}//last gasp, only a brief period for sprint allowed, and can starve prey
						}					
					}
				}			
				for(myBoid b : bAra){
//					if ((b.preyFlkLoc.size() !=0) || (b.predFlkLoc.size() !=0)){//if prey exists
//						System.out.println("Flock : " + f.name+" ID : " + b.ID + " preyFlock size : " + b.preyFlkLoc.size()+ " pred flk size : " + b.predFlkLoc.size());
//					}										
					if (b.preyFlkLoc.size() !=0){//if prey exists
						myPointf tar = b.preyFlkLoc.firstEntry().getValue(); 
						//add force at single boid target
						float mult = (fv.eatFreq/(b.starveCntr + 1.0f));
						myVectorf chase = setFrcVal(myVectorf._mult(myVectorf._sub(tar, b.coords),  mult),fv.wts, fv.maxFrcs,fv.wFrcChsPrey); 
//						if(b.ID % 100 == 0){
//							System.out.println("Flock : " + f.name+" ID : " + b.ID + " Chase force : " + chase.toString() + " mult : " + mult + " starve : " + b.starveCntr);
//							
//						}						
						b.forces._add(chase);						
					}
				}
			}		
//		}//if ! single flock
		
		if(stFlags[flkAvoidCol]){//find avoidance forces, if appropriate within f.colRad
			for(myBoid b : bAra){
				if(b.colliderLoc.size()==0){continue;}
				//b.forces._add(setFrcVal(frcAvoidCol(b, b.colliders, b.colliderLoc, colRadSq),fv.wts, fv.maxFrcs,fv.wFrcAvd));
				b.forces._add(setFrcVal(frcAvoidCol(b, b.colliderLoc, colRadSq),fv.wts, fv.maxFrcs,fv.wFrcAvd));
			}
		}				
		
		if(stFlags[flkVelMatch]){		//find velocity matching forces, if appropriate within f.colRad	
			for(myBoid b : bAra){
				if(b.neighbors.size()==0){continue;}
				b.forces._add(setFrcVal(frcVelMatch(b),fv.wts, fv.maxFrcs,fv.wFrcVel));
			}
		}	
		if(stFlags[flkCenter]){ //find attracting forces, if appropriate within f.nghbrRad	
			for(myBoid b : bAra){	
				if(b.neighbors.size()==0){continue;}
				b.forces._add(setFrcVal(frcToCenter(b),fv.wts, fv.maxFrcs,fv.wFrcCtr));
			}
		}		
		if(stFlags[flkWander]){//brownian motion
			for(myBoid b : bAra){	b.forces._add(setFrcVal(frcWander(b),fv.wts, fv.maxFrcs,fv.wFrcWnd));}
		}	
		//damp velocity
		for(myBoid b : bAra){	
			//dampFrc.set(b.velocity[0]);
			dampFrc.set(b.velocity);
			dampFrc._mult(-fv.dampConst);
			b.forces._add(dampFrc);		
		}
		//for(myBoid b : bAra){b.forces.set(getForceAtLocation(b));}
	}//run
	
	@Override
	public Boolean call() throws Exception {
		run(); return true;
	}	

}//class myFwdStencil


class myOrigForceStencil extends myFwdStencil{
	public myOrigForceStencil(Boids_2 _p, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
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

class myLinForceStencil extends myFwdStencil{
	public myLinForceStencil(Boids_2 _p, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
		super(_p, _f,_flagInt, _isClk, _bAra);
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