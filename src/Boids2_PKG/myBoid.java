package Boids2_PKG;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * class defining a creature object for flocking
 * @author John
 */
public class myBoid {
	public Boids_2 p;
	public myBoids3DWin win;
	public myBoidFlock f;
	
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
						 	hadChild		= 3,							//had a child this cycle, needs to "deliver"
						 	numbd_flags 	= 4;
	
	//location to put new child
	public myPointf birthLoc;
	//animation controlling variables
	public float animCntr;
	public static final float maxAnimCntr = 1000.0f, baseAnimSpd = 1.0f;
	public final float preCalcAnimSpd;
	//boat construction variables
	public final int type,gender;//,bodyColor;													//for spawning gender = 0 == female, 1 == male;
	public static final int O_FWD = 0, O_RHT = 1,  O_UP = 2;
		
	public ConcurrentSkipListMap<Float, myBoid> neighbors,			//sorted map of neighbors to this boid
									preyFlk,				//sorted map of prey near this boid
									ptnWife;				//sorted map of potential mates near this boid
	
	public ConcurrentSkipListMap<Float, myPointf> neighLoc,			//boid mapped to location used for distance calc
										colliderLoc,					//boid mapped to location used for distance calc
										predFlkLoc,						//boid mapped to location used for distance calc
										preyFlkLoc;						//boid mapped to location used for distance calc
			
	public myBoid(Boids_2 _p, myBoids3DWin _win, myBoidFlock _f,  myPointf _coords, int _type){
		ID = IDcount++;		p = _p;		f = _f; win = _win;
		initbd_flags();
		rotVec = myVectorf.RIGHT.cloneMe(); 			//initial setup
		orientation = new myVectorf[3];
		orientation[O_FWD] = myVectorf.FORWARD.cloneMe();
		orientation[O_RHT] = myVectorf.RIGHT.cloneMe();
		orientation[O_UP] = myVectorf.UP.cloneMe();
		preCalcAnimSpd = (float) ThreadLocalRandom.current().nextDouble(.5f,2.0);		
		animCntr = (float) ThreadLocalRandom.current().nextDouble(.000001f ,maxAnimCntr );
		coords = new myPointf(_coords);	//new myPointf[2]; 
		velocity = new myVectorf();
		forces  = new myVectorf();//= new myVectorf[2];
		type=_type;
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
		mass=f.flv.getInitMass();
		scaleBt = new myVectorf(scMult);					//for rendering different sized boids
		scaleBt._mult(mass);		
		//init starve counter with own mass
		eat(mass);//starveCntr = resetCntrs(fv.eatFreq[type],ThreadLocalRandom.current().nextFloat(mass ));
		spawnCntr = 0;		
		bd_flags[canSpawn] = true;
	}
	
	public void clearNeighborMaps(){	
		neighbors.clear(); 
		neighLoc.clear(); colliderLoc.clear();	 
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
			if((b.gender==0)&&(b.canSpawn())){				
				ptnWife.put(dist, b);
			}
		}
	}//copySubSetBoidsMate
	public void haveChild(myPointf _bl, myVectorf _bVel, myVectorf _bFrc){bd_flags[hadChild]=true; birthLoc=_bl;birthVel=_bVel;birthForce=_bFrc;}
	public boolean hadAChild(myPointf[] _bl, myVectorf[] _bVelFrc){if(bd_flags[hadChild]){bd_flags[hadChild]=false;_bl[0].set(birthLoc);_bVelFrc[0].set(birthVel);_bVelFrc[1].set(birthForce);return true;} else {return false;}}	
	public int resetCntrs(int cntrBseVal, float mod){return (int)(cntrBseVal*(1+mod));}
	//only reset spawn counters once boid has spawned
	public void hasSpawned(){spawnCntr = resetCntrs(f.flv.spawnFreq,ThreadLocalRandom.current().nextFloat()); bd_flags[canSpawn] = false;}
	public boolean canSpawn(){return bd_flags[canSpawn];}
	public void eat(float tarMass){	starveCntr = resetCntrs(f.flv.eatFreq,tarMass);bd_flags[isHungry]=false;}
	public boolean canSprint(){return (starveCntr > .25f*f.flv.eatFreq);}
	public boolean isHungry(){return bd_flags[isHungry];}
	//init bd_flags state machine
	public void initbd_flags(){bd_flags = new boolean[numbd_flags];for(int i=0;i<numbd_flags;++i){bd_flags[i]=false;}}
	//update hunger counters
	public void updateHungerCntr(){
		starveCntr--;
		if (starveCntr<=0){killMe("Starvation");}//if can get hungry then can starve to death
		bd_flags[isHungry] = (bd_flags[isHungry] || (p.random(f.flv.eatFreq)>=starveCntr)); //once he's hungry he stays hungry unless he eats (hungry set to false elsewhere)
	}
	//update spawn counters
	public void updateSpawnCntr(){
		spawnCntr--;
		bd_flags[canSpawn]=(spawnCntr<=0);
	}//updateBoidCounters	
	
	//initialize newborn velocity, forces, and orientation
	public void initNewborn(myVectorf[] bVelFrc){
		velocity.set(bVelFrc[0]);
		forces.set(bVelFrc[1]);
	}	
	//align the boid along the current orientation matrix
	private void alignBoid(){
		rotVec.set(O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		//TODO change f.delT to get value from win UI input
		float rotAngle = (float) (oldRotAngle + ((O_axisAngle[0]-oldRotAngle) * f.delT));
		p.rotate(rotAngle,O_axisAngle[1],O_axisAngle[2],O_axisAngle[3]);
		oldRotAngle = rotAngle;
	}//alignBoid	
	//kill this boid
	public void killMe(String cause){
		if(p.flags[p.debugMode]){System.out.println("Boid : " +ID+" killed : " + cause);}
		bd_flags[isDead]=true;
	}	
	
	//set this boat to be camera location
	public void setBoatCam(float dThet, float dPhi, float dz){
		//set eye to initially be at coords of boid, modified for world being displaced by half grid dims
		myPointf eyeTmp = myPointf._sub(coords,p.gridHalfDim);
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
		p.camera(eye.x, eye.y, eye.z, dir.x, dir.y, dir.z, -orientation[O_UP].x, -orientation[O_UP].y, -orientation[O_UP].z);
	}//setBoatCam
	
	//draw this body on mesh
	public void drawMe(){
		p.pushMatrix();p.pushStyle();
			p.translate(coords.x,coords.y,coords.z);		//move to location
			if(win.getPrivFlags(win.debugAnimIDX)){drawMyVec(rotVec, Boids_2.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			if(win.getPrivFlags(win.showVel)){drawMyVec(velocity, Boids_2.gui_DarkMagenta,.5f);}
			alignBoid();
			p.rotate(p.PI/2.0f,1,0,0);
			p.rotate(p.PI/2.0f,0,1,0);
			p.scale(scaleBt.x,scaleBt.y,scaleBt.z);																	//make appropriate size				
			p.pushStyle();
			f.tmpl.drawMe(animCntr);

			p.popStyle();
			
		p.popStyle();p.popMatrix();
		animIncr();
	}//drawme	
	
	//draw this boid as a ball - replace with sphere render obj 
	public void drawMeBall(){
		p.pushMatrix();p.pushStyle();
			//p.strokeWeight(1.0f);
			p.translate(coords.x,coords.y,coords.z);		//move to location
			if(win.getPrivFlags(win.debugAnimIDX)){drawMyVec(rotVec, Boids_2.gui_Black,4.0f);p.drawAxes(100, 2.0f, new myPoint(0,0,0), orientation, 255);}
			if(win.getPrivFlags(win.showVel)){drawMyVec(velocity, Boids_2.gui_DarkMagenta,.5f);}
			p.setColorValFill(p.gui_boatBody1 + type);
			p.noStroke();
			p.sphere(5);
		p.popStyle();p.popMatrix();
		animIncr();
	}//drawme 
	
	public void drawClosestPrey(){
		if(this.preyFlkLoc.size() == 0){return;}
		myPointf tmp = this.preyFlkLoc.firstEntry().getValue();
		int clr1 = p.gui_Red, clr2 = p.gui_boatBody1 + ((type +3 - 1)%3);
		drawClosestOther(tmp, clr1, clr2);
	}
	
	public void drawClosestPredator(){
		if(this.predFlkLoc.size() == 0){return;}
		myPointf tmp = this.predFlkLoc.firstEntry().getValue();
		int clr1 = p.gui_Cyan, clr2 = p.gui_boatBody1 + ((type +3 + 1)%3);
		drawClosestOther(tmp, clr1, clr2);
	}	
	
	private void drawClosestOther(myPointf tmp, int stClr, int endClr){
		p.pushMatrix();p.pushStyle();
			p.strokeWeight(1.0f);
			//p.setColorValStroke(stClr);
			p.line(coords, tmp,stClr,endClr );
			//p.line(coords, tmp);
			p.translate(tmp.x,tmp.y,tmp.z);		//move to location
			p.setColorValFill(endClr);
			p.noStroke();
			p.sphere(10);
		p.popStyle();p.popMatrix();
		
	}		
	//public double calcBobbing(){		return 2*(p.cos(.01f*animCntr));	}		//bobbing motion
	
	public void drawMyVec(myVectorf v, int clr, float sw){
		p.pushMatrix();
			p.pushStyle();
			p.setColorValStroke(clr);
			p.strokeWeight(sw);
			p.line(new myPointf(0,0,0),v);
			p.popStyle();
		p.popMatrix();		
	}
	
	private void animIncr(){
		animCntr = (animCntr + (baseAnimSpd + (velocity.magn*.1f))*preCalcAnimSpd) % maxAnimCntr;						//set animMod based on velocity -> 1 + mag of velocity		
//		if((animCntr>maxAnimCntr)||(animCntr<0)){
//			animCntr =0;
//		}
	}//animIncr		
	
	public String toString(){
		String result = "ID : " + ID + " Type : "+win.flkNames[f.type]+" | Mass : " + mass + " | Spawn CD "+spawnCntr + " | Starve CD " + starveCntr+"\n";
		result+=" | location : " + coords + " | velocity : " + velocity + " | forces : " + forces +"\n" ;
		//if(p.flags[p.debugMode]){result +="\nOrientation : UP : "+orientation[O_UP] + " | FWD : "+orientation[O_FWD] + " | RIGHT : "+orientation[O_RHT] + "\n";}
		int num =neighbors.size();
		result += "# neighbors : "+ num + (num==0 ? "\n" : " | Neighbor IDs : \n");
		if(win.getPrivFlags(win.showFlkMbrs)){	for(Float bd_K : neighbors.keySet()){result+="\tNeigh ID : "+neighbors.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		num = colliderLoc.size();
		result += "# too-close neighbors : "+ num + (num==0 ? "\n" : " | Colliders IDs : \n");
		if(win.getPrivFlags(win.showFlkMbrs)){for(Float bd_K : colliderLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		result += "# predators : "+ num + (num==0 ? "\n" : " | Predator IDs : \n");
		if(win.getPrivFlags(win.showFlkMbrs)){for(Float bd_K : predFlkLoc.keySet()){result+="\tDist from me : " + bd_K+"\n";}}
		num = preyFlk.size();
		result += "# prey : "+ num + (num==0 ? "\n" : " | Prey IDs : \n");
		if(win.getPrivFlags(win.showFlkMbrs)){for(Float bd_K : preyFlk.keySet()){result+="\tPrey ID : "+preyFlk.get(bd_K).ID + " dist from me : " + bd_K+"\n";}}
		return result;
	}	
}//myBoid class
