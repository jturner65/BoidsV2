package Boids2_PKG.flocks.boids;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.myBoidFlock;
import base_Render_Interface.IRenderInterface;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.base.Base_DispWindow;

/**
 * class defining a creature object for flocking
 * @author John
 */
public class myBoid {
	public static GUI_AppManager AppMgr;
	public myBoidFlock flk;
	
	public int ID;
	public static int IDcount = 0;
	
	public int starveCntr, spawnCntr;
	public float[] O_axisAngle,															//axis angle orientation of this boid
				baby_O_axisAngle;													//axis angle orientation of this boid's spawn
	public float mass,oldRotAngle;	
	public final float sizeMult = .15f;
	public final myVectorf scMult = new myVectorf(.5f,.5f,.5f);				//multiplier for scale based on mass
	private myVectorf scaleBt,rotVec, birthVel, birthForce;						//scale of boat - reflects mass, rotational vector, vel and force applied at birth - hit the ground running
	
	public myPointf coords;													//com coords
	public myVectorf velocity,
					  forces;												//force accumulator
	public myVectorf[] orientation;												//Rot matrix - 3x3 orthonormal basis matrix - cols are bases for body frame orientation in world frame
	
	public boolean[] bd_flags;	
	public final static int canSpawn 		= 0,							//whether enough time has passed that this boid can spawn
						 	isDead			= 1,							//whether this boid is dead
						 	isHungry		= 2,							//whether this boid is hungry
						 	hadChild		= 3;							//had a child this cycle, needs to "deliver"
	private final static int NumBoidFlags 	= 4;
	
	/**
	 * location to put new child
	 */
	private myPointf birthLoc;	
	
	/**
	 * animation controlling variables
	 */	
	private double animCntr;
	
	/**
	 * Fraction of animation cycle currently at
	 */
	public double animPhase;
	/**
	 * Current render object type's max anim counter
	 */
	private double maxAnimCntr;	
	/**
	 * 
	 */
	private final double baseAnimSpd = 1.0;
	
	public final int type;
	/**
	 * for spawning gender = 0 == female, 1 == male;
	 */
	public final int gender;												
	public static final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
		
	public ConcurrentSkipListMap<Float, myBoid> neighbors,	//sorted by distance map of neighbors to this boid
									preyFlk,				//sorted by distance map of prey near this boid
									ptnWife;				//sorted by distance map of potential mates near this boid
	
	public ConcurrentSkipListMap<Float, myPointf> neighLoc,	//boid mapped to location used for distance calc
										colliderLoc,		//boid mapped to location used for distance calc
										predFlkLoc,			//boid mapped to location used for distance calc
										preyFlkLoc;			//boid mapped to location used for distance calc
	
	public myBoid(myBoidFlock _f,  myPointf _coords, int _type){
		ID = IDcount++;	 flk = _f; type=_type; 
		AppMgr = Base_DispWindow.AppMgr;
		initbd_flags();
		rotVec = myVectorf.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVectorf[3];
		orientation[O_FWD] = myVectorf.FORWARD.cloneMe();
		orientation[O_RHT] = myVectorf.RIGHT.cloneMe();
		orientation[O_UP] = myVectorf.UP.cloneMe();
		//keep initial phase between .25 and .75 so that cyclic-force boids start moving right away
		animPhase = (float) ThreadLocalRandom.current().nextDouble(.25f, .75f ) ;
		maxAnimCntr = flk.getMaxAnimCounter();
		animCntr = animPhase * maxAnimCntr;
		
		coords = new myPointf(_coords);	//new myPointf[2]; 
		velocity = new myVectorf();
		forces  = new myVectorf();//= new myVectorf[2];
		setInitState();
		O_axisAngle=new float[]{0,1,0,0};
		oldRotAngle = 0;
		gender = ThreadLocalRandom.current().nextInt(1000)%2;												//0 or 1
		neighbors 	= new ConcurrentSkipListMap<Float, myBoid>();
		preyFlk 	= new ConcurrentSkipListMap<Float, myBoid>();
		ptnWife 	= new ConcurrentSkipListMap<Float, myBoid>();
		
		neighLoc 	= new ConcurrentSkipListMap<Float, myPointf>();
		colliderLoc = new ConcurrentSkipListMap<Float, myPointf>();
		predFlkLoc	= new ConcurrentSkipListMap<Float, myPointf>();
		preyFlkLoc	= new ConcurrentSkipListMap<Float, myPointf>();
	}//constructor
	
	public void setInitState(){
		mass=flk.flv.getInitMass();
		scaleBt = new myVectorf(scMult);					//for rendering different sized boids
		scaleBt._mult(mass);		
		//init starve counter with own mass
		eat(mass);//starveCntr = resetCntrs(fv.eatFreq[type],ThreadLocalRandom.current().nextFloat(mass ));
		spawnCntr = 0;		
		bd_flags[canSpawn] = true;
	}
	
	public void clearNeighborMaps(){	
		neighbors.clear(); 
		neighLoc.clear(); 
		colliderLoc.clear();	 
	}
	public void clearHuntMaps(){
		predFlkLoc.clear();
		preyFlk.clear(); 
		preyFlkLoc.clear();		
	}
	
	public void copySubSetBoidsCol(Float colRadSq){		
		colliderLoc.putAll(neighLoc.subMap(0.0f, colRadSq));	
	}
	public void copySubSetBoidsMate(Float spawnRadSq){
		if((!bd_flags[canSpawn]) || (gender==0)){return;}//need "males" who can mate
		for(Float dist : neighbors.keySet()){
			if (dist > spawnRadSq){return;}//returns in increasing order - can return once we're past spawn Rad Threshold
			myBoid b = neighbors.get(dist);
			if((b.gender==0)&&(b.canSpawn())){	ptnWife.put(dist, b);}
		}
	}//copySubSetBoidsMate
	public void haveChild(myPointf _bl, myVectorf _bVel, myVectorf _bFrc){bd_flags[hadChild]=true; birthLoc=_bl;birthVel=_bVel;birthForce=_bFrc;}
	public boolean hadAChild(myPointf[] _bl, myVectorf[] _bVelFrc){//if baby is born then set values of arrays and return
		if(bd_flags[hadChild]){
			bd_flags[hadChild]=false;
			_bl[0].set(birthLoc);
			_bVelFrc[0].set(birthVel);
			_bVelFrc[1].set(birthForce);
			return true;} 
		return false;
	}	
	private int resetCntrs(int cntrBseVal, float mod){return (int)(cntrBseVal*(1+mod));}
	//only reset spawn counters once boid has spawned
	public void hasSpawned(){spawnCntr = resetCntrs(flk.flv.spawnFreq,ThreadLocalRandom.current().nextFloat()); bd_flags[canSpawn] = false;}
	public boolean canSpawn(){return bd_flags[canSpawn];}
	//update spawn counters
	public void updateSpawnCntr(){
		--spawnCntr;
		bd_flags[canSpawn]=(spawnCntr<=0);
	}//updateBoidCounters	
	
	//update hunger counters
	public void updateHungerCntr(){
		--starveCntr;
		if (starveCntr<=0){killMe("Starvation"); return;}//if can get hungry then can starve to death
		//bd_flags[isHungry] = (bd_flags[isHungry] || (p.random(f.flv.eatFreq)>=starveCntr)); //once he's hungry he stays hungry unless he eats (hungry set to false elsewhere)
		bd_flags[isHungry] = (bd_flags[isHungry] || (ThreadLocalRandom.current().nextInt(flk.flv.eatFreq)>=starveCntr)); //once he's hungry he stays hungry unless he eats (hungry set to false elsewhere)
	}	
	public void eat(float tarMass){	starveCntr = resetCntrs(flk.flv.eatFreq,tarMass);bd_flags[isHungry]=false;}
	public boolean canSprint(){return (starveCntr > flk.flv.canSprintCycles);}
	
	/**
	 * Whether or not we're hungry
	 * @return
	 */
	public boolean isHungry(){return bd_flags[isHungry];}
	/**
	 * Whether or not we're dead
	 * @return
	 */
	public boolean isDead() {return bd_flags[isDead];}
	//init bd_flags state machine
	public void initbd_flags(){bd_flags = new boolean[NumBoidFlags];for(int i=0;i<NumBoidFlags;++i){bd_flags[i]=false;}}
	
	//initialize newborn velocity, forces, and orientation
	public void initNewborn(myVectorf[] bVelFrc){
		velocity.set(bVelFrc[0]);
		forces.set(bVelFrc[1]);
	}	
	//align the boid along the current orientation matrix
	private void alignBoid(IRenderInterface ri){
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		float rotAngle = (float) (oldRotAngle + ((O_axisAngle[0]-oldRotAngle) * flk.getDeltaT()));
		ri.rotate(rotAngle,rotVec.x, rotVec.y, rotVec.z);
		oldRotAngle = rotAngle;
	}//alignBoid	
	//kill this boid
	public void killMe(String cause){
		if(AppMgr.isDebugMode()){AppMgr.msgObj.dispConsoleDebugMessage("myBoid", "killMe", "Boid : " +ID+" killed by : " + cause);}
		bd_flags[isDead]=true;
	}	
	
	/**
	 * set this boat to be camera location
	 * @param ri
	 * @param dThet
	 * @param dPhi
	 * @param dz
	 */
	public void setBoatCam(IRenderInterface ri,float dThet, float dPhi, float dz){
		//set eye to initially be at coords of boid, modified for world being displaced by half grid dims
		myPointf eyeTmp = myPointf._sub(coords,AppMgr.gridHalfDim);
		myVectorf tmpEyeMod = new myVectorf( orientation[O_FWD]);
		tmpEyeMod._mult(2.0f);
		tmpEyeMod._add(orientation[O_UP]);
		tmpEyeMod._mult(4.0f);
		//move eye to be on deck
		eyeTmp._add(tmpEyeMod);
		myVectorf rotdir = myVectorf._rotAroundAxis( orientation[O_FWD], orientation[O_UP], dPhi);
		myVectorf eyeLookVec = myVectorf._rotAroundAxis( rotdir, rotdir._cross(orientation[O_UP]), dThet);
		myPointf eye = myPointf._add(eyeTmp, .1f*dz, eyeLookVec);
		myPointf dir =  myPointf._add(eyeTmp, eyeLookVec);
		ri.setCameraWinVals(new float[] {eye.x, eye.y, eye.z, dir.x, dir.y, dir.z, -orientation[O_UP].x, -orientation[O_UP].y, -orientation[O_UP].z});
	}//setBoatCam
	
	private void drawTmpl(IRenderInterface ri) {
		ri.pushMatState();
		flk.getCurrTemplate().drawMe(animPhase, ID);
		ri.popMatState();
	}
	
	/**
	 * draw this body on mesh
	 * @param ri
	 */
	public void drawMe(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			alignBoid(ri);
			drawTmpl(ri);		
		ri.popMatState();
		animIncr(velocity.magn*.1f);
	}//drawme	
	
	public void drawMeScaled(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			alignBoid(ri);
			ri.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			drawTmpl(ri);
		ri.popMatState();
		animIncr(velocity.magn*.1f);
	}//drawme	
	
	/**
	 * Only called when animated object is drawn
	 * @param vel
	 */
	private void animIncr(float vel){
		animCntr += (baseAnimSpd + vel);//*preCalcAnimSpd;						//set animMod based on velocity -> 1 + mag of velocity	
		maxAnimCntr = flk.getMaxAnimCounter();
		animCntr %= maxAnimCntr;
		animPhase = (animCntr/maxAnimCntr);									//phase of animation cycle
	}//animIncr	
	
	/**
	 * draw this boid as a ball - replace with sphere render obj 
	 * @param debugAnim
	 * @param showVel
	 */
	public void drawMeAsBall(IRenderInterface ri){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location			
			flk.getSphereTemplate().drawMe(animPhase, ID);
		ri.popMatState();
	}//drawme 
	
	public void drawClosestPrey(IRenderInterface ri){
		if(this.preyFlkLoc.size() == 0){return;}
		myPointf tmp = this.preyFlkLoc.firstEntry().getValue();
		int clr1 = IRenderInterface.gui_Red, clr2 = IRenderInterface.gui_White;
		drawClosestOther(ri, tmp, clr1, clr2);
	}
	
	public void drawClosestPredator(IRenderInterface ri){
		if(this.predFlkLoc.size() == 0){return;}
		myPointf tmp = this.predFlkLoc.firstEntry().getValue();
		int clr1 = IRenderInterface.gui_Cyan, clr2 = IRenderInterface.gui_White;
		drawClosestOther(ri, tmp, clr1, clr2);
	}	
	
	private void drawClosestOther(IRenderInterface ri, myPointf tmp, int stClr, int endClr){
		ri.pushMatState();
			ri.setStrokeWt(3.0f);
			//p.setColorValStroke(stClr);
			ri.drawLine(coords, tmp,stClr,endClr );
			//p.line(coords, tmp);
			ri.translate(tmp.x,tmp.y,tmp.z);		//move to location
			ri.setColorValFill(endClr, 255);
			ri.noStroke();
			ri.drawSphere(10);
		ri.popMatState();		
	}		
	//public double calcBobbing(){		return 2*(p.cos(.01f*animCntr));	}		//bobbing motion
	
	/**
	 * Draw velocity vector
	 * @param ri
	 */
	public void drawMyVel(IRenderInterface ri) {
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			ri.setColorValStroke(IRenderInterface.gui_Magenta, 255);
			ri.setStrokeWt(.5f);
			ri.drawLine(myPointf.ZEROPT,velocity);
		ri.popMatState();
	}
	/**
	 * Draw rotation vector and orientation frame
	 * @param ri
	 */
	public void drawMyFrame(IRenderInterface ri) {
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			ri.setColorValStroke(IRenderInterface.gui_Cyan, 255);
			ri.setStrokeWt(2.0f);
			ri.drawLine(myPointf.ZEROPT,myPointf._mult(rotVec, 100f));
			AppMgr.drawAxes(100, 2.0f, myPoint.ZEROPT, orientation, 255);
		ri.popMatState();	
	}
	
	protected void drawMyVec(IRenderInterface ri, myVectorf v, int clr, float sw){
		ri.pushMatState();
			ri.translate(coords.x,coords.y,coords.z);		//move to location
			ri.setColorValStroke(clr, 255);
			ri.setStrokeWt(sw);
			ri.drawLine(myPointf.ZEROPT,v);
		ri.popMatState();	
	}

	public String toString(){
		String result = "ID : " + ID + " Type : "+flk.getName()+" | Mass : " + mass + " | Spawn CD "+spawnCntr + " | Starve CD " + starveCntr+"\n";
		result+=" | location : " + coords + " | velocity : " + velocity + " | forces : " + forces +"\n" ;
		//if(p.flags[p.debugMode]){result +="\nOrientation : UP : "+orientation[O_UP] + " | FWD : "+orientation[O_FWD] + " | RIGHT : "+orientation[O_RHT] + "\n";}
		int num =neighbors.size();
		result += "# neighbors : "+ num + (num==0 ? "\n" : " | Neighbor IDs : \n");
		if(flk.getShowFlkMbrs()){	for(Float bd_K : neighbors.keySet()){result+="\tNeigh ID : "+neighbors.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = colliderLoc.size();
		result += "# too-close neighbors : "+ num + (num==0 ? "\n" : " | Colliders IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : colliderLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		result += "# predators : "+ num + (num==0 ? "\n" : " | Predator IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : predFlkLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		num = preyFlk.size();
		result += "# prey : "+ num + (num==0 ? "\n" : " | Prey IDs : \n");
		if(flk.getShowFlkMbrs()){for(Float bd_K : preyFlk.keySet()){result+="\tPrey ID : "+preyFlk.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		return result;
	}	
}//myBoid class
