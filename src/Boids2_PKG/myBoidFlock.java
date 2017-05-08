package Boids2_PKG;

//import java.util.SortedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import processing.core.PImage;

public class myBoidFlock {
	public Boids_2 p;	
	public myBoids3DWin win;

	public String name;
	public int numBoids;
	public ArrayList<myBoid> boidFlock;		
	
	public float delT;
	//specific flock vars for this flock TODO
	//public flkVrs fv;
	
	public myFlkVars flv;						//flock vars per flock	
	
	public final float  distGrwthMod = 1.1f,	//how the radius should grow to look for more creatures for neighbors, if haven't satisfied minimum number
						nearPct = .4f;			//% size of total population to use as neighborhood target, if enough creatures
	
	public float totMaxRad;						//max search distance for neighbors
	public int nearCount;						//# of creatures required to have a neighborhood - some % of total # of creatures, or nearMinCnt, whichever is larger
	public final int nearMinCnt = 5;			//smallest neighborhood size allowed -> 5 or total # of creatures, whichever is smaller
	
	public int curFlagState;					//holds current state of first 32 flags from win/UI
	
	public final int type, mtFrameSize = 50;		//mtFrameSize is # of boids per thread
	public myRenderObj tmpl, sphTmpl;				//template to render boid; simplified sphere template
	public myBoidFlock preyFlock, predFlock;		//direct reference to flock that is my prey and my predator -- set in main program after init is called
	
	public List<Future<Boolean>> callFwdSimFutures, callUpdFutures, callInitFutures, callResetBoidFutures;
	public List<myFwdStencil> callFwdBoidCalcs;
	public List<myUpdateStencil> callUbdBoidCalcs;	
	public List<myInitPredPreyMaps> callInitBoidCalcs;
	public List<myResetBoidStencil> callResetBoidCalcs;
	///////////
	//graphical constructs for boids of this flock
	///////////
	public PImage flkSail;						//image sigil for sails
	private static final float bdgSizeX = 20, bdgSizeY = 15;			//badge size
	private static final myPointf[] mnBdgBox = new myPointf[]{new myPointf(0,0,0),new myPointf(0,bdgSizeY,0),new myPointf(bdgSizeX,bdgSizeY,0),new myPointf(bdgSizeX,0,0)};
	private static final myPointf[] mnUVBox = new myPointf[]{new myPointf(0,0,0),new myPointf(1,0,0),new myPointf(1,1,0),new myPointf(0,1,0)};

	//flock-specific data
	private int flkMenuClr;//color of menu	
	
	public myBoidFlock(Boids_2 _p, myBoids3DWin _win, String _name, int _numBoids, int _type){
		p = _p; win=_win;	name = _name; 
		//fv = new flkVrs(p, win, win.MaxNumFlocks);	
		type = _type;
		flv = new myFlkVars(p, win, this, win.flkRadMults[type]);
		//Boids_2 _p, myBoids3DWin _win, myBoidFlock _flock, int _bodyClr, int numSpc, float _nRadMult
		delT = win.getTimeStep();
		setNumBoids(_numBoids);
		totMaxRad = p.gridDimX + p.gridDimY + p.gridDimZ;
		
		flkSail = win.flkSails[type];
		flkMenuClr = win.clrList[type];		
		
		flv = new myFlkVars(p,win,this,(float)ThreadLocalRandom.current().nextDouble(0.65, 1.0));
		
		callFwdBoidCalcs= new ArrayList<myFwdStencil>();
		callFwdSimFutures = new ArrayList<Future<Boolean>>(); 

		callUbdBoidCalcs = new ArrayList<myUpdateStencil>();
		callUpdFutures = new ArrayList<Future<Boolean>>(); 
		
		callInitBoidCalcs = new ArrayList<myInitPredPreyMaps>();
		callInitFutures = new ArrayList<Future<Boolean>>(); 	

		callResetBoidCalcs = new ArrayList<myResetBoidStencil>();
		callResetBoidFutures = new ArrayList<Future<Boolean>>(); 	
		
		curFlagState = win.getFlkFlagsInt();

	}//myBoidFlock constructor
	//init bflk_flags state machine
	
	//public void initbflk_flags(boolean initVal){bflk_flags = new boolean[numbflk_flags];for(int i=0;i<numbflk_flags;++i){bflk_flags[i]=initVal;}}
	public void initFlock(){
		boidFlock = new ArrayList<myBoid>(numBoids);
		//System.out.println("make flock of size : "+ numBoids);
		for(int c = 0; c < numBoids; ++c){
			boidFlock.add(c, new myBoid(p, win,this,randBoidStLoc(1), type));
		}
	}//initFlock - run after each flock has been constructed
	
	public void setPredPreyTmpl(int predIDX, int preyIDX, myRenderObj _tmpl, myRenderObj _sphrTmpl){
		predFlock = win.flocks[predIDX];//flock 0 preys on flock 2, is preyed on by flock 1
		preyFlock = win.flocks[preyIDX];	
		tmpl = _tmpl;
		sphTmpl = _sphrTmpl;
	}//set after init - all flocks should be made
	
	//finds valid coordinates if torroidal walls 
	public myPointf findValidWrapCoordsForDraw(myPointf _coords){return new myPointf(((_coords.x+p.gridDimX) % p.gridDimX),((_coords.y+p.gridDimY) % p.gridDimY),((_coords.z+p.gridDimZ) % p.gridDimZ));	}//findValidWrapCoords	
	public void setValidWrapCoordsForDraw(myPointf _coords){_coords.set(((_coords.x+p.gridDimX) % p.gridDimX),((_coords.y+p.gridDimY) % p.gridDimY),((_coords.z+p.gridDimZ) % p.gridDimZ));	}//findValidWrapCoords	
	public float calcRandLocation(float randNum1, float randNum2, float sqDim, float mathCalc, float mult){return ((sqDim/2.0f) + (randNum2 * (sqDim/3.0f) * mathCalc * mult));}
	public myPointf randBoidStLoc(float mult){		return new myPointf(ThreadLocalRandom.current().nextFloat()*p.gridDimX,ThreadLocalRandom.current().nextFloat()*p.gridDimY,ThreadLocalRandom.current().nextFloat()*p.gridDimZ);	}
	
	public void setNumBoids(int _numBoids){
		numBoids = _numBoids;
		nearCount = (int) Math.max(Math.min(nearMinCnt,numBoids), numBoids*nearPct); 		
	}
	
	//adjust boid population by m
	public void modBoidPop(int m){
		if(m>0){if(boidFlock.size() >= win.MaxNumBoids) {return;}for(int i=0;i<m;++i){ addBoid();}} 
		else { int n=-1*m; n = (n>numBoids-1 ? numBoids-1 : n);for(int i=0;i<n;++i){removeBoid();}}
	}//modBoidPop
	
	public myBoid addBoid(){	return addBoid(randBoidStLoc(1));	}	
	public myBoid addBoid(myPointf stLoc){
		myBoid tmp = new myBoid(p, win, this, stLoc, type); 
		boidFlock.add(tmp);
		setNumBoids(boidFlock.size());
		return tmp;
	}//addBoid	
	
	public void removeBoid(){removeBoid(boidFlock.size()-1);}
	public void removeBoid(int idx){
		if(idx<0){return;}	
		boidFlock.remove(idx);
		setNumBoids(boidFlock.size());
	}//removeBoid	
	
	//move creatures to random start positions
	public void scatterBoids() {for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).coords.set(randBoidStLoc(1));}}//	randInit
	public void drawBoids(){
		if(win.getPrivFlags(win.drawBoids)){
	  		for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMe();}			
		} else {
			for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawMeBall();  }
			if(win.getPrivFlags(win.showFlkMbrs)){
				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPrey();  }
				for(int c = 0; c < boidFlock.size(); ++c){boidFlock.get(c).drawClosestPredator();  }
			}
		}
	}//drawBoids
	//handle click in menu region - return idx of mod obj or -1
	public int handleFlkMenuClick(int mouseX, int mouseY){
		int vIdx = -1;
		float mod = 0;
		int clkRow = (mouseY/12);//UI values modifiable in rows 1,3,5 and 6
		switch(clkRow){
		case 1 : {//radii 40 -100 | 110-165 | 180 ->
			if(40 > mouseX) {		vIdx =  -1;	} 
			else {		vIdx = (mouseX - 40)/60; vIdx = (vIdx > 2 ? 2 : vIdx);}
			break;	}
		case 3 : {//weight vals : ctr : 10-45; av 50-90; velM 95-125; wander 130-165; avPred 170-200   ; chase 205->    ;
			if(10 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 3 + (mouseX - 10)/40;vIdx = (vIdx > 8 ? 8 : vIdx);}
			break;	}
		case 5 : {//spawn vals ( 60-85; 90-125; 130-165 )
			if(60 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 9 +  (mouseX - 60)/30;	}
			break;		}
		case 6 : {//hunt vals (  60-85; 90-135; 140-175 )
			if(60 > mouseX) {		vIdx = -1;	} 
			else {		vIdx = 12 + (mouseX - 60)/30;	}
			break;	}		
		default : {break;}
		}//switch			
		p.outStr2Scr("handleFlkMenuClick : Flock : " + name + " [" + mouseX + "," + mouseY + "] row : " +clkRow + " obj idx : " + vIdx);	
		return vIdx;
	}
	//handle click in menu region - abs x, rel to start y
	public boolean handleFlkMenuDrag(int flkVarIDX, int mouseX, int mouseY, int pmx, int pmy, int mseBtn){
		boolean res = true;
		float mod = (mouseX-pmx) + (mouseY-pmy)*-5.0f;		
		flv.modFlkVal(flkVarIDX, mod);		
		p.outStr2Scr("handleFlkMenuDrag : Flock : " + name + " flkVar IDX : " + flkVarIDX + " mod amt : " + mod);		
		return res;
	}//handleFlkMenuClick
	
	public void drawMenuBadge(myPointf[] ara, myPointf[] uvAra, int type) {
		p.beginShape(); 
			p.texture(flkSail);
			for(int i=0;i<ara.length;++i){	p.vTextured(ara[i], uvAra[i].y, uvAra[i].x);} 
		p.endShape(p.CLOSE);
	}//
	
	public void drawFlockMenu(int i){
		String fvData[] = flv.getData(numBoids);			
		p.translate(0,-bdgSizeY-6);
		drawMenuBadge(mnBdgBox,mnUVBox,i);
		p.translate(bdgSizeX+3,bdgSizeY+6);
		p.setColorValFill(flkMenuClr);
		p.text(fvData[0],0,-win.yOff*.5f);p.translate(0,win.yOff*.75f);
		p.translate(-bdgSizeX-3,0);
		for(int j=1;j<fvData.length; ++j){p.text(fvData[j],0,-win.yOff*.5f);p.translate(0,win.yOff*.75f);}	
	}//drawFlockMenu
	
	//clear out all data for each boid
	public void clearOutBoids(){
		curFlagState = win.getFlkFlagsInt();
		//sets current time step from UI
		delT = win.getTimeStep();
		callResetBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callResetBoidCalcs.add(new myResetBoidStencil(p, this, preyFlock, curFlagState, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callResetBoidFutures = p.th_exec.invokeAll(callResetBoidCalcs);for(Future<Boolean> f: callResetBoidFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	//build all data structures holding neighbors, pred, prey
	public void initAllMaps(){
		callInitBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callInitBoidCalcs.add(new myInitPredPreyMaps(p, this, preyFlock, predFlock, flv, curFlagState, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callInitFutures = p.th_exec.invokeAll(callInitBoidCalcs);for(Future<Boolean> f: callInitFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }			
	}
	//TODO get this from myDispWindow instead of p
	private boolean ckAddFrc(){return (p.flags[p.mouseClicked] ) && (!p.flags[p.shiftKeyPressed]);}
	//build forces using linear distance functions
	public void moveBoidsLinMultTH(){
		callFwdBoidCalcs.clear();
		boolean addFrc = ckAddFrc();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callFwdBoidCalcs.add(new myLinForceStencil(p, this, curFlagState, addFrc, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callFwdSimFutures = p.th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	
	//build forces using original boids-style distance functions
	public void moveBoidsOrigMultTH(){
		callFwdBoidCalcs.clear();
		boolean addFrc = ckAddFrc();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callFwdBoidCalcs.add(new myOrigForceStencil(p, this, curFlagState, addFrc, boidFlock.subList(c, c+finalLen)));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {callFwdSimFutures = p.th_exec.invokeAll(callFwdBoidCalcs);for(Future<Boolean> f: callFwdSimFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		
	}
	public void updateBoidMovement(){
		callUbdBoidCalcs.clear();
		for(int c = 0; c < boidFlock.size(); c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < boidFlock.size() ? mtFrameSize : boidFlock.size() - c);
			callUbdBoidCalcs.add(new myUpdateStencil(p, this, curFlagState, boidFlock.subList(c, c+finalLen)));
		}							//apply update
		try {callUpdFutures = p.th_exec.invokeAll(callUbdBoidCalcs);for(Future<Boolean> f: callUpdFutures) { f.get(); }} catch (Exception e) { e.printStackTrace(); }		    	
    	//update - remove dead, add babies
        myPointf[] bl = new myPointf[]{new myPointf()};
        myVectorf[] bVelFrc  = new myVectorf[]{new myVectorf(),new myVectorf()};
        myBoid b;
        for(int c = 0; c < boidFlock.size(); ++c){
        	b = boidFlock.get(c);
        	if((b != null) && (b.bd_flags[myBoid.isDead])){    removeBoid(c);  }
        	else {  
        		if(b.hadAChild(bl,bVelFrc)){myBoid tmpBby = this.addBoid(bl[0]); tmpBby.initNewborn(bVelFrc);}}
        } 
	}//updateBoids	

	public String[] getInfoString(){return this.toString().split("\n",-1);}
	
	public String toString(){
		String res = "Flock Size " + boidFlock.size() + "\n";
		for(myBoid bd : boidFlock){			res+="\t     "+bd.toString(); res+="\n";	}
		return res;
	}
		
}//myBoidFlock class
