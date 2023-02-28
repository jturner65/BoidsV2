package Boids2_PKG.threadedSolvers.forceSolvers.base;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.myBoidFlock;
import Boids2_PKG.flocks.myFlkVars;
import Boids2_PKG.flocks.boids.myBoid;
import Boids2_PKG.ui.Boids_3DWin;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;

public abstract class Base_ForceSolver implements Callable<Boolean> {
	private GUI_AppManager AppMgr;
	//protected flkVrs fv;
	private myFlkVars fv;
	protected List<myBoid> bAra;								//boid being worked on
	protected myBoidFlock f;
	myVectorf dampFrc;
	protected float velRadSq, predRadSq, neighRadSq, colRadSq;				
	private int flagInt;						//bitmask of current flags
	public boolean[] stFlags; 
	
	public final float epsValCalc;
		
	protected final float msClickForce = 100000000;
	public final int 		
		flkCenter		= 0,
        flkVelMatch		= 1,
        flkAvoidCol		= 2,
        flkWander		= 3,
        flkAvoidPred 	= 4,
        flkHunt			= 5,
        attractMode 	= 6,
        flkCyclesFrc = 7;
	
	public static final int[] stFlagIDXs = new int[]{
		Boids_3DWin.flkCenter,
		Boids_3DWin.flkVelMatch,
		Boids_3DWin.flkAvoidCol,
		Boids_3DWin.flkWander,
		Boids_3DWin.flkAvoidPred,
		Boids_3DWin.flkHunt,
		Boids_3DWin.attractMode,
		Boids_3DWin.flkCyclesFrc};
	
	private boolean addFrc;

	public Base_ForceSolver(GUI_AppManager _AppMgr, myBoidFlock _f, int _flagInt, boolean _isClk, List<myBoid> _bAra) {
		AppMgr=_AppMgr; f = _f;fv = f.flv; bAra=_bAra;
		flagInt = _flagInt;
		setStFlags();		
		addFrc = _isClk;
		velRadSq = fv.velRad * fv.velRad; 		
		predRadSq = fv.predRad * fv.predRad;
		neighRadSq = fv.nghbrRad* fv.nghbrRad;
		colRadSq = fv.colRad * fv.colRad;
		dampFrc = new myVectorf();
		epsValCalc = MyMathUtils.EPS_F;
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
	
	public void runMe(){
		//if((p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed])){//add click force : overwhelms all forces - is not scaled
		if (addFrc){
			for(myBoid b : bAra){b.forces._add(AppMgr.mouseForceAtLoc(msClickForce, b.coords, stFlags[attractMode]));}
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
		//for(myBoid b : bAra){b.forces.set(getForceAtLocation(b));}
		if(stFlags[flkCyclesFrc]){//if cyclic forces - turn off when jellyfish boid is "contracting"
			for(myBoid b : bAra){
				
				float sclAmt =0.5f+ (float) (Math.sin(MyMathUtils.TWO_PI * b.animPhase) *.5f);
				b.forces._scale(b.forces.magn * sclAmt);								
//				dampFrc.set(b.velocity);
//				dampFrc._mult(-.1f);
//				b.forces._add(dampFrc);		
//
//				if((b.animPhase <= .25f) || (b.animPhase >= .75f)){	
//					//b.forces.set(0,0,0);//extra damping
//					dampFrc.set(b.velocity);
//					dampFrc._mult(-.95f);
//					b.forces.set(dampFrc);		
//				} 
//				else {
//					float sclAmt = p.cos(b.animPhase);
//					b.forces._scale(b.forces.magn * 1.5f*sclAmt);								
//				}
			}			
		} 
		//damp velocity only if no cycling of force
		for(myBoid b : bAra){	
			dampFrc.set(b.velocity);
			dampFrc._mult(-fv.dampConst);
			b.forces._add(dampFrc);		
		}

		
	}//run
	
	@Override
	public Boolean call() throws Exception {
		runMe(); return true;
	}	

}//class myFwdStencil




