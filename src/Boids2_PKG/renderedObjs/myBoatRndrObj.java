package Boids2_PKG.renderedObjs;

import Boids2_PKG.myBoids3DWin;
import Boids2_PKG.boids.myBoid;
import base_UI_Objects.my_procApplet;
import base_Utils_Objects.vectorObjs.myPointf;
import base_Utils_Objects.vectorObjs.myVectorf;
import processing.core.*;

//build a registered pre-rendered instantiatable object for each objRep - speeds up display by orders of magnitude
public class myBoatRndrObj extends myRenderObj {
	//all boid obj types need this
	//if overall geometry has been made or not
	private static boolean made;
	//precalc consts
	private static final int numOars = 5;
	//objRep geometry/construction variables
	private final static myPointf[][] boatVerts = new myPointf[5][12];						//seed points to build object 	
	private static myPointf[][] boatRndr;													//points of hull
	private static myPointf[] pts3, pts5, pts7;	
		
	//extra pshapes for this object
	private PShape[] oars;										//1 array for each type of objRep, 1 element for each animation frame of oar motion
	private static myPointf[] uvAra;
	
	private PImage sailTexture;
	
	//colors for boat reps of boids
	//primary object color (same across all types of boids); individual type colors defined in instance class
	private static myRndrObjClr mainColor;	
	//base IDX - this is main color for all boats
	private static final int baseBoatIDX = 0;
	//divisors for stroke color from fill color
	private static float[] clrStrkDiv = new float[]{.8f,5.0f,.75f,4.0f,.3f};
	//boat colors - get from load? TODO
	private static int[][] 
			boatFillClrs = new int[][]{{110, 65, 30,255},	{30, 30, 30,255},	{130, 22, 10,255},	{22, 230, 10,255},	{22, 10, 130,255}},
			boatStrokeClrs = new int[5][4],//overridden to be fraction of fill color
			boatEmitClrs = new int[][]{boatFillClrs[0],		boatFillClrs[1],	boatFillClrs[2],	boatFillClrs[3],	boatFillClrs[4]};
	private static final int[] boatSpecClr = new int[]{255,255,255,255};
	private static final float strkWt = 1.0f;
	private static final float shn = 5.0f;
	
	
	public myBoatRndrObj(my_procApplet _p, myBoids3DWin _win, int _type) {	
		super(_p, _win, _type);	 
		made = initGeometry(made);
	}//ctor

	//inherited from myRenderObj
	//colors shared by all instances/flocks of this type of render obj
	@Override
	protected void initMainColor(){
		mainColor = makeColor(boatFillClrs[baseBoatIDX], boatStrokeClrs[baseBoatIDX], boatEmitClrs[baseBoatIDX], new int[]{0,0,0,0}, boatSpecClr, clrStrkDiv[baseBoatIDX], strkWt, shn);
		mainColor.disableAmbient();
	}			
	//set up colors for individual flocks/teams 
	@Override
	protected void initFlkColor(){	
		flockColor = makeColor(boatFillClrs[type], boatStrokeClrs[type], boatEmitClrs[type], new int[]{0,0,0,0}, boatSpecClr,clrStrkDiv[type], strkWt, shn);
		flockColor.disableAmbient();
	}
	
	//builds geometry for object to be instanced - only perform once per object type 
	@Override
	protected void initObjGeometry() {		
		float xVert, yVert, zVert;	
		for(int j = 0; j < boatVerts[0].length; ++j){
			zVert = j - 4;		
			float sf = (1 - ((zVert+3)*(zVert+3)*(zVert+3))/(boatVerts[0].length * boatVerts[0].length * boatVerts[0].length * 1.0f));
			for(int i = 0; i < boatVerts.length; ++i){
				float ires1 = (1.5f*i - 3);
				xVert = ires1 * sf;
				yVert = ((-1 * PApplet.sqrt(9 - (ires1*ires1)) ) * sf) + (3*(zVert-2)*(zVert-2))/(boatVerts[0].length *boatVerts[0].length);
				boatVerts[i][j] = new myVectorf(xVert, yVert, zVert);
			}//for i	
		}//for j	
		pts3 = buildSailPtAra(3);
		pts5 = buildSailPtAra(5);
		pts7 = buildSailPtAra(7);		
		initBoatBody();	
		//UV ara shaped like sail
		uvAra = new myPointf[]{new myPointf(0,0,0),new myPointf(0,1,0),
				new myPointf(.375f,.9f,0),new myPointf(.75f,.9f,0),
				new myPointf(1,1,0),new myPointf(1,0,0),
				new myPointf(.75f,.1f,1.5f),new myPointf(.375f,.1f,1.5f)};
			
	}//initObjGeometry()	
	
	//set values for flock-type specific instance of boid render rep, (rep is built in base class call)
	@Override
	protected void initInstObjGeometryIndiv(){ 
		//create all shapes for individual oar pshape groups, for each frame of animation
		oars = new PShape[myBoid.numAnimFrames];
		for(int a=0; a<myBoid.numAnimFrames; ++a){
			oars[a] = p.createShape(PConstants.GROUP); 		
		}	

		sailTexture = ((myBoids3DWin)win).flkSails[type];
	}//initInstObjGeometry

	@Override //representation-specific drawing code (i.e. oars settings for boats)
	protected void drawMeIndiv(int animIDX){//which oars array instance of oars to show - oars move relative to speed of boid
		p.shape(oars[animIDX]);
	}//drawMe
	
	@Override
	protected void buildObj(){
		//send color to use for masts and oars
		initBoatMasts(mainColor);
		int numZ = boatVerts[0].length-1, numX = boatVerts.length;
		int btPt = 0;
		float[] tmpAra = new float[]{0,1,0}, tmpAra2 = new float[]{0,1.5f,0};
		for(int j = 0; j < numZ; ++j){
			btPt = buildQuadShape(tmpAra, numX, btPt, boatRndr);
		}//for j
		for(int i = 0; i < numX; ++i){	
			buildBodyBottom(boatVerts,i, numZ, numX);	
		}//for i	
		for(int j = 0; j < numZ; ++j){
			btPt = buildQuadShape( tmpAra, 1, btPt, boatRndr);
			btPt = buildQuadShape( tmpAra, 1, btPt, boatRndr);		
		}//for j		
		//draw rear and front castle
		for(int j = 0; j < 27; ++j){
			btPt = buildQuadShape( tmpAra2, 1, btPt, boatRndr);
		}		
	}//buildShape
	//end inherited from myRenderObj
	
	private myPointf[] buildSailPtAra(float len){
		myPointf[] res = new myPointf[]{new myPointf(0,0,.1f),new myPointf(0,len,.1f),
				new myPointf(-1.5f,len*.9f,1.5f),new myPointf(-3f,len*.9f,1.5f),
				new myPointf(-4f,len,0),new myPointf(-4f,0,0),
				new myPointf(-3f,len*.1f,1.5f),new myPointf(-1.5f,len*.1f,1.5f)};
		return res;
	}
	//build masts and oars(multiple orientations in a list to just show specific frames)
	private void initBoatMasts(myRndrObjClr clr){	
		myVectorf[] trans1Ara = new myVectorf[]{new myVectorf(0, 3.5f, -3),new myVectorf(0, 1.25f, 1),new myVectorf(0, 2.2f, 5),new myVectorf(0, 2.3f, 7)},
				scale1Ara = new myVectorf[]{new myVectorf(.95f,.85f,1),new myVectorf(1.3f,1.2f,1),new myVectorf(1f,.9f,1),new myVectorf(1,1,1)};
		
		float[][] rot1Ara = new float[][]{new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{pi3rds, 1, 0,0}};
		int idx = 0;
		for(int rep = 0; rep < 3; rep++){buildSail( false, pts7,pts5, (type%2==1), trans1Ara[idx],  scale1Ara[idx]);idx++; }
		buildSail(true, pts3,pts3, true, trans1Ara[idx],  scale1Ara[idx]);   //
				
		for(int j = 0; j<trans1Ara.length; ++j){//mainColor,
			if(j==3){//front sail
				objRep.addChild(buildPole(0, clr, .1f, 7, false, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0},new myVectorf(0,0,0), new float[]{0,0,0,0}));
				objRep.addChild(buildPole(4, clr, .05f, 3,  true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(1,-1.5f,0), new float[]{0,0,0,0}));
			}
			else{
				objRep.addChild(buildPole(1,clr, .1f, 10, false,trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0}, new myVectorf(0,0,0), new float[]{0,0,0,0}));
				objRep.addChild(buildPole(2,clr, .05f, 7, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(0,-3.5f,0), new float[]{0,0,0,0}));
				objRep.addChild(buildPole(3,clr, .05f, 5, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(4.5f,-2.5f,0), new float[]{0,0,0,0}));
			}					
		}
		for(int j = 0; j < myBoid.numAnimFrames; ++j){
			float animCntr = (j* myBoid.maxAnimCntr/(1.0f*myBoid.numAnimFrames)) ;
			buildOars(j, clr, animCntr, 1, new myVectorf(0, 0.3f, 3));
			buildOars(j, clr, animCntr, -1, new myVectorf(0, 0.3f, 3));
		}
	}//initBoatMasts	

//	//build oars to orient in appropriate position for animIdx frame of animation - want all numAnimFrm of animation to cycle
//	private void buildOars(int animIdx, myRndrObjClr clr, float animCntr, float dirMult, myVectorf transVec){
//		float[] rotAra1 = new float[]{PConstants.HALF_PI, 1, 0, 0},
//				rotAra2, rotAra3;
//		myVectorf transVec1 = new myVectorf(0,0,0);
//		float disp = 0, d=-6, distMod = 10.0f/numOars;
//		for(int i =0; i<numOars;++i){
//			float ca = pi4thrds + .65f*PApplet.cos(animCntr*pi100th), sa = pi6ths + .65f*PApplet.sin(((animCntr + i/(1.0f*numOars)))*pi100th);
//			transVec1.set((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);//sh.translate((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);
//			rotAra2 = new float[]{ca, 0,0,dirMult};
//			rotAra3 = new float[]{sa*.5f, 1,0, 0};			
//			oars[animIdx].addChild(buildPole(1,clr,.1f, 6, false, transVec1, new myVectorf(1,1,1), rotAra1, new myVectorf(0,0,0), rotAra2, new myVectorf(0,0,0), rotAra3));			
//			disp+=distMod;
//		}			
//	}//buildOars

	//build oars to orient in appropriate position for animIdx frame of animation - want all numAnimFrm of animation to cycle
	private void buildOars(int animIdx, myRndrObjClr clr, float animCntr, float dirMult, myVectorf transVec){
		float[] rotAra1 = new float[]{PConstants.HALF_PI, 1, 0, 0},
				rotAra2, rotAra3;
		myVectorf transVec1 = new myVectorf(0,0,0);
		float disp = 0, d=-6, distMod = 10.0f/numOars;
		for(int i =0; i<numOars;++i){
			float ca = pi4thrds + .65f*PApplet.cos(animCntr*pi100th), sa = pi6ths + .65f*PApplet.sin(((animCntr + i/(1.0f*numOars)))*pi100th);
			transVec1.set((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);//sh.translate((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);
			rotAra2 = new float[]{ca, 0,0,dirMult};
			rotAra3 = new float[]{sa*.5f, 1,0, 0};			
			oars[animIdx].addChild(buildPole(1,clr,.1f, 6, false, transVec1, new myVectorf(1,1,1), rotAra1, new myVectorf(0,0,0), rotAra2, new myVectorf(0,0,0), rotAra3));			
			disp+=distMod;
		}			
	}//buildOars

	private void build1Sail( boolean renderSigil, myPointf[] pts, myVectorf transVec,myVectorf trans2Vec, myVectorf scaleVec){
		PShape sh = makeShape(transVec.x, transVec.y, transVec.z);
		sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
		sh.translate(0,4.5f,0);
		sh.rotate(PConstants.HALF_PI, 0,0,1 );
		sh.translate(0,-3.5f,0);
		sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
		sh.beginShape(); 
		sh.fill(0xFFFFFFFF);	
		sh.noStroke();	
		if(renderSigil){	
			//processing bug with textures which corrupts fill color of boat
			//sh.texture(sailTexture);
			for(int i=0;i<pts.length;++i){	sh.vertex(pts[i].x,pts[i].y,pts[i].z,uvAra[i].y,uvAra[i].x);}		
		}
		else {						
			//sh.noTexture();	
			//for(int i=0;i<pts.length;++i){	sh.vertex(pts[i].x,pts[i].y,pts[i].z);}		
			for(int i=0;i<pts.length;++i){	sh.vertex(pts[i].x,pts[i].y,pts[i].z,uvAra[i].y,uvAra[i].x);}		
		}			
		sh.endShape(PConstants.CLOSE);
		objRep.addChild(sh);			
	}
	
	private void buildSail(boolean frontMast, myPointf[] pts1, myPointf[] pts2, boolean renderSigil, myVectorf transVec, myVectorf scaleVec){
		if(frontMast){
			PShape sh = makeShape(0, 2.3f, 7);
			sh.rotate(pi3rds, 1, 0,0);
			sh.translate(0,5,0);
			sh.rotate(PConstants.HALF_PI, 0,0,1 );
			sh.translate(1,-1.5f,0);			
			sh.beginShape(); 
			sh.fill(0xFFFFFFFF);	
			sh.noStroke();	
			//processing bug with textures which corrupts fill color of boat - fixed in proc 3.3.3 but orientations are all messed up in 3.3.3
			//sh.texture(sailTexture);
			for(int i=0;i<pts1.length;++i){	sh.vertex(pts1[i].x,pts1[i].y,pts1[i].z,uvAra[i].y,uvAra[i].x);}			
			sh.endShape(PConstants.CLOSE);
			objRep.addChild(sh);			
		}
		else {			
			build1Sail( renderSigil, pts1, transVec, myVectorf.ZEROVEC, scaleVec);
			build1Sail( !renderSigil, pts2, transVec,new myVectorf(4.5f,1,0), scaleVec);
		}
	}//drawSail
	
	
	private void buildBodyBottom(myPointf[][] boatVerts, int i, int lastIDX, int numX){
		PShape sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.TRIANGLE);			
			flockColor.shPaintColors(sh);
			sh.vertex(boatVerts[i][lastIDX].x, boatVerts[i][lastIDX].y, 	boatVerts[i][lastIDX].z);	sh.vertex(0, 1, lastIDX-1);	sh.vertex(boatVerts[(i+1)%numX][lastIDX].x, boatVerts[(i+1)%numX][lastIDX].y, 	boatVerts[(i+1)%numX][lastIDX].z);	
		sh.endShape(PConstants.CLOSE);
		objRep.addChild(sh);			

		sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.QUAD);		
			flockColor.shPaintColors(sh);
			sh.vertex(boatVerts[i][0].x, boatVerts[i][0].y, boatVerts[i][0].z);sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z -.5f);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z -.5f);sh.vertex(boatVerts[(i+1)%numX][0].x, boatVerts[(i+1)%numX][0].y, 	boatVerts[(i+1)%numX][0].z );
		sh.endShape(PConstants.CLOSE);
		objRep.addChild(sh);			
		
		sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.TRIANGLE);		
			flockColor.shPaintColors(sh);
			sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z  -.5f);	sh.vertex(0, 0, boatVerts[i][0].z - 1);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z  -.5f);	
		sh.endShape(PConstants.CLOSE);		
		objRep.addChild(sh);
	}
	
	//build objRep's body points
	private void initBoatBody(){
		int numZ = boatVerts[0].length, numX = boatVerts.length, idx, pIdx = 0, araIdx = 0;
		myPointf[] tmpPtAra;
		myPointf[][] resPtAra = new myPointf[104][];
		
		for(int j = 0; j < numZ-1; ++j){
			for(int i = 0; i < numX; ++i){
				tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[i][j].x, 	boatVerts[i][j].y, 	boatVerts[i][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[(i+1)%numX][j].x, 		boatVerts[(i+1)%numX][j].y,			boatVerts[(i+1)%numX][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[(i+1)%numX][(j+1)%numZ].x,boatVerts[(i+1)%numX][(j+1)%numZ].y, boatVerts[(i+1)%numX][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[i][(j+1)%numZ].x,			boatVerts[i][(j+1)%numZ].y, 			boatVerts[i][(j+1)%numZ].z);
				resPtAra[araIdx++] = tmpPtAra;
			}//for i	
		}//for j		
		for(int j = 0; j < numZ-1; ++j){
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x, boatVerts[0][j].y, 	 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x, 					boatVerts[0][j].y +.5f,			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x,			boatVerts[0][(j+1)%numZ].y + .5f, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x,			boatVerts[0][(j+1)%numZ].y, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x, boatVerts[numX-1][j].y, 	 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x, 		 boatVerts[numX-1][j].y + .5f,			 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x, boatVerts[numX-1][(j+1)%numZ].y +.5f, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x, boatVerts[numX-1][(j+1)%numZ].y, 	 boatVerts[numX-1][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		//draw rear castle
		for(int j = 0; j < 3; ++j){
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 			boatVerts[0][j].y-.5f, 			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 					boatVerts[0][j].y+2,			 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y+2, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y-.5f, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y-.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, 		 boatVerts[numX-1][j].y+2,			 boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y+2, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y-.5f, 	 boatVerts[numX-1][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+1.5f,		boatVerts[0][j].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, 		boatVerts[numX-1][j].y+1.5f,			boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f,boatVerts[numX-1][(j+1)%numZ].y+1.5f, 	boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,		boatVerts[0][(j+1)%numZ].y+1.5f, 		boatVerts[0][(j+1)%numZ].z);					
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][3].x*.9f, 		boatVerts[0][3].y+2,		boatVerts[0][3].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][3].x*.9f, boatVerts[0][3].y+2,boatVerts[numX-1][3].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][3].x*.9f, boatVerts[0][3].y-.5f,boatVerts[numX-1][3].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][3].x*.9f,		boatVerts[0][3].y-.5f, 	boatVerts[0][3].z);			
		resPtAra[araIdx++] = tmpPtAra;

		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 	boatVerts[0][0].y-.5f, 	boatVerts[0][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 	boatVerts[0][0].y+2.5f,	boatVerts[0][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,	boatVerts[0][0].y+2, 	boatVerts[0][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,	boatVerts[0][0].y-1, 	boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;

		tmpPtAra = new myPointf[4];pIdx = 0;
		tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f, 	boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f, boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2, 	boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-1, 	boatVerts[numX-1][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y+2.5f,		boatVerts[0][0].z - 1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f,	boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2f,	boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y+2f, 		boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y-.5f,		boatVerts[0][0].z - 1);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-1,boatVerts[numX-1][1].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-1, 	boatVerts[0][1].z-1);			
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, 		boatVerts[0][0].y+2.5f,		boatVerts[0][0].z - 1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y+2.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[numX-1][0].y-.5f,boatVerts[numX-1][0].z-1);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-.5f, 	boatVerts[0][0].z-1);				
		resPtAra[araIdx++] = tmpPtAra;
		tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f, boatVerts[0][0].y+2,		boatVerts[0][0].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[0][0].y+2,boatVerts[numX-1][0].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][0].x*.9f, boatVerts[0][0].y-.5f,boatVerts[numX-1][0].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][0].x*.9f,		boatVerts[0][0].y-.5f, 	boatVerts[0][0].z);	
		resPtAra[araIdx++] = tmpPtAra;
		//draw front castle
		for(int j = numZ-4; j < numZ-1; ++j){
			tmpPtAra = new myPointf[4];	pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y-.5f, 		boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+.5f,		 boatVerts[0][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y+.5f, boatVerts[0][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,			boatVerts[0][(j+1)%numZ].y-.5f, 	 boatVerts[0][(j+1)%numZ].z);				
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y-.5f, 	boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y+.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y+.5f, boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f, boatVerts[numX-1][(j+1)%numZ].y-.5f, 	 boatVerts[numX-1][(j+1)%numZ].z);					
			resPtAra[araIdx++] = tmpPtAra;
			tmpPtAra = new myPointf[4];pIdx = 0;	tmpPtAra[pIdx++] = new myPointf(boatVerts[0][j].x*.9f, 		boatVerts[0][j].y+.5f,			boatVerts[0][j].z);	tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][j].x*.9f, boatVerts[numX-1][j].y+.5f, boatVerts[numX-1][j].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][(j+1)%numZ].x*.9f,boatVerts[numX-1][(j+1)%numZ].y+.5f, 	boatVerts[numX-1][(j+1)%numZ].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][(j+1)%numZ].x*.9f,		boatVerts[0][(j+1)%numZ].y+.5f, 		boatVerts[0][(j+1)%numZ].z);
			resPtAra[araIdx++] = tmpPtAra;
		}//for j
		idx = numZ-1;
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f, 		boatVerts[0][ idx].y-.5f,	boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][ idx].x*.9f, boatVerts[0][ idx].y-.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][ idx].x*.9f, boatVerts[0][ idx].y+.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f,		boatVerts[0][ idx].y+.5f, 	boatVerts[0][ idx].z);			
		resPtAra[araIdx++] = tmpPtAra;
		idx = numZ-4;
		tmpPtAra = new myPointf[4];pIdx = 0;tmpPtAra[pIdx++] = new myPointf(boatVerts[0][ idx].x*.9f, 		boatVerts[0][idx].y-.5f,	boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][idx].x*.9f, boatVerts[0][idx].y-.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[numX-1][idx].x*.9f, boatVerts[0][idx].y+.5f,		boatVerts[0][ idx].z);tmpPtAra[pIdx++] = new myPointf(boatVerts[0][idx].x*.9f,		boatVerts[0][idx].y+.5f, 	boatVerts[0][idx].z);			
		resPtAra[araIdx++] = tmpPtAra;
		boatRndr = resPtAra;
	}//initBoatBody	

}//class myBoatRndrObj
