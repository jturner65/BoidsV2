package Boids2_PKG;

import processing.core.*;

//build a registered pre-rendered instantiatable object for each boat - speeds up display by orders of magnitude
public class myBoatRndrObj {
	private Boids_2 p;	
	private myBoids3DWin win;
	//precalc consts
	private static final float pi4thrds = 4*PConstants.PI/3.0f, pi100th = .01f*PConstants.PI, pi6ths = PConstants.PI/6.0f, pi3rds = PConstants.PI/3.0f;
	private static final int numOars = 5, numAnimFrm = 90;
	//boat geometry/construction variables
	private final static myPointf[][] boatVerts = new myPointf[5][12];						//body of boat, masts 	
	private static myPointf[][] boatRndr;	
	private static myPointf[] pts3, pts5, pts7;	
	private static boolean made = false;
	private static boolean[] madeType;	
	
	//individual boat-type pshapes	
	private PShape boat;										//1 shape for each type of boat
	private PShape[] oars;										//1 array for each type of boat, 1 element for each animation frame of oar motion
	private myPointf[] uvAra;
	
	private int type;											//type of flock this boat represents
	private PImage sailTexture;
	private int boatColor, mastColor;
	private int[] strokeColor, mastStClr;//, fillColor;
	
	public myBoatRndrObj(Boids_2 _p, myBoids3DWin _win, int _type) {
		p=_p; win=_win;
		if(!made){
			madeType = new boolean[win.MaxNumBoids];
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
			made = true;
		}//all boat geometry is being set once in static vars
		
		if(!madeType[_type]){
			uvAra = new myPointf[]{new myPointf(0,0,0),new myPointf(0,1,0),
					new myPointf(.375f,.9f,0),new myPointf(.75f,.9f,0),
					new myPointf(1,1,0),new myPointf(1,0,0),
					new myPointf(.75f,.1f,1.5f),new myPointf(.375f,.1f,1.5f)};
			type = _type;
			boat = p.createShape(PConstants.GROUP); 
			oars = new PShape[numAnimFrm];
			for(int a=0; a<numAnimFrm; ++a){
				oars[a] = p.createShape(PConstants.GROUP); 		
			}	
			sailTexture = win.flkSails[type];
			boatColor = win.bodyColor[type];
			mastColor = win.bodyColor[0];
			strokeColor = p.getClr(boatColor);
			mastStClr = p.getClr(mastColor);
			
			madeType[type] = true;
			initBoatMasts();
			buildBoat();
		}
	}//ctor
	
	private myPointf[] buildSailPtAra(float len){
		myPointf[] res = new myPointf[]{new myPointf(0,0,.1f),new myPointf(0,len,.1f),
				new myPointf(-1.5f,len*.9f,1.5f),new myPointf(-3f,len*.9f,1.5f),
				new myPointf(-4f,len,0),new myPointf(-4f,0,0),
				new myPointf(-3f,len*.1f,1.5f),new myPointf(-1.5f,len*.1f,1.5f)};
		return res;
	}
	//build masts and oars(multiple orientations in a list to just show specific frames)
	private void initBoatMasts(){	
		myVectorf[] trans1Ara = new myVectorf[]{new myVectorf(0, 3.5f, -3),new myVectorf(0, 1.25f, 1),new myVectorf(0, 2.2f, 5),new myVectorf(0, 2.3f, 7)},
				scale1Ara = new myVectorf[]{new myVectorf(.95f,.85f,1),new myVectorf(1.3f,1.2f,1),new myVectorf(1f,.9f,1),new myVectorf(1,1,1)};
		
		float[][] rot1Ara = new float[][]{new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{0,0,0,0},new float[]{pi3rds, 1, 0,0}};
		int idx = 0;
		for(int rep = 0; rep < 3; rep++){buildSail( false, pts7,pts5, (type%2==1), trans1Ara[idx],  scale1Ara[idx]);idx++; }
		buildSail(true, pts3,pts3, true, trans1Ara[idx],  scale1Ara[idx]);   //
		
		for(int j = 0; j<trans1Ara.length; ++j){
			if(j==3){//front sail
				boat.addChild(buildPole(0,.1f, 7, false, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0},new myVectorf(0,0,0), new float[]{0,0,0,0}));
				boat.addChild(buildPole(4,.05f, 3,  true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(1,-1.5f,0), new float[]{0,0,0,0}));
			}
			else{
				boat.addChild(buildPole(1,.1f, 10, false,trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0,0,0), new float[]{0,0,0,0}, new myVectorf(0,0,0), new float[]{0,0,0,0}));
				boat.addChild(buildPole(2,.05f, 7, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(0,-3.5f,0), new float[]{0,0,0,0}));
				boat.addChild(buildPole(3,.05f, 5, true, trans1Ara[j],  scale1Ara[j], rot1Ara[j], new myVectorf(0, 4.5f, 0), new float[]{PConstants.HALF_PI, 0,0,1},new myVectorf(4.5f,-2.5f,0), new float[]{0,0,0,0}));
			}					
		}
		for(int j = 0; j < numAnimFrm; ++j){
			float animCntr = (j* myBoid.maxAnimCntr/(1.0f*numAnimFrm)) ;
			buildOars(j, animCntr, 1, new myVectorf(0, 0.3f, 3));
			buildOars(j, animCntr, -1, new myVectorf(0, 0.3f, 3));
		}
	}//initBoatMasts	

	
	public void drawMe(float animCntr, int type){
		p.shape(boat);
		int idx = (int)((animCntr/(1.0f*myBoid.maxAnimCntr)) * numAnimFrm);			//determine which in the array of oars, corresponding to particular orientations, we should draw
		p.shape(oars[idx]);
	}

	//build oars to orient in appropriate position for animIdx frame of animation - want all numAnimFrm of animation to cycle
	public void buildOars(int animIdx, float animCntr, float dirMult, myVectorf transVec){
		float[] rotAra1 = new float[]{PConstants.HALF_PI, 1, 0, 0},
				rotAra2, rotAra3;
		myVectorf transVec1 = new myVectorf(0,0,0);
		float disp = 0, d=-6, distMod = 10.0f/numOars;
		for(int i =0; i<numOars;++i){
			float ca = pi4thrds + .65f*PApplet.cos(animCntr*pi100th), sa = pi6ths + .65f*PApplet.sin(((animCntr + i/(1.0f*numOars)))*pi100th);
			transVec1.set((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);//sh.translate((transVec.x)+dirMult*1.5f, transVec.y, (transVec.z)+ d+disp);
			rotAra2 = new float[]{ca, 0,0,dirMult};
			rotAra3 = new float[]{sa*.5f, 1,0, 0};			
			oars[animIdx].addChild(buildPole(1,.1f, 6, false, transVec1, new myVectorf(1,1,1), rotAra1, new myVectorf(0,0,0), rotAra2, new myVectorf(0,0,0), rotAra3));			
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
			//textures make things slow!
			//sh.texture(sailTexture);
			for(int i=0;i<pts.length;++i){	sh.vertex(pts[i].x,pts[i].y,pts[i].z,uvAra[i].y,uvAra[i].x);}		
		}
		else {						
			//sh.noTexture();	
			for(int i=0;i<pts.length;++i){	sh.vertex(pts[i].x,pts[i].y,pts[i].z);}		
		}			
		sh.endShape(PConstants.CLOSE);
		boat.addChild(sh);			
	}
	
	public void buildSail(boolean frontMast, myPointf[] pts1, myPointf[] pts2, boolean renderSigil, myVectorf transVec, myVectorf scaleVec){
		if(frontMast){
			PShape sh = makeShape(0, 2.3f, 7);
			sh.rotate(pi3rds, 1, 0,0);
			sh.translate(0,5,0);
			sh.rotate(PConstants.HALF_PI, 0,0,1 );
			sh.translate(1,-1.5f,0);			
			sh.beginShape(); 
			sh.fill(0xFFFFFFFF);	
			sh.noStroke();	
			//textures make things slow!
			//sh.texture(sailTexture);
			for(int i=0;i<pts1.length;++i){	sh.vertex(pts1[i].x,pts1[i].y,pts1[i].z,uvAra[i].y,uvAra[i].x);}			
			sh.endShape(PConstants.CLOSE);
			boat.addChild(sh);			
		}
		else {			
			build1Sail( renderSigil, pts1, transVec, myVectorf.ZEROVEC, scaleVec);
			build1Sail( !renderSigil, pts2, transVec,new myVectorf(4.5f,1,0), scaleVec);
		}
	}//drawSail
	
	private PShape setRotVals(myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec, float[] rotAra3){	
		//sets up initial translation/scale/rotations for poles used as masts or oars
		PShape sh = makeShape(transVec.x, transVec.y, transVec.z);			
		sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
		sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
		sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
		sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
		sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
		sh.rotate(rotAra3[0],rotAra3[1],rotAra3[2],rotAra3[3]);		
		return sh;
	}

	public PShape buildPole(int poleNum, float rad, float height, boolean drawBottom, myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec, float[] rotAra3){
		float theta, theta2, rsThet, rcThet, rsThet2, rcThet2;
		float numTurns = 6.0f;
		PShape shRes = p.createShape(PConstants.GROUP), sh;
		for(int i = 0; i <numTurns; ++i){
			theta = (i/numTurns) * PConstants.TWO_PI;
			theta2 = (((i+1)%numTurns)/numTurns) * PConstants.TWO_PI;
			rsThet = rad*PApplet.sin(theta);
			rcThet = rad*PApplet.cos(theta);
			rsThet2 = rad*PApplet.sin(theta2);
			rcThet2 = rad*PApplet.cos(theta2);

			sh = setRotVals(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);
			sh.beginShape(PConstants.QUAD);				      
				setStrokeAndFillPole(sh);
				shgl_vertexf(sh,rsThet, 0, rcThet );
				shgl_vertexf(sh,rsThet, height,rcThet);
				shgl_vertexf(sh,rsThet2, height,rcThet2);
				shgl_vertexf(sh,rsThet2, 0, rcThet2);
			sh.endShape(PConstants.CLOSE);	
			shRes.addChild(sh);

			sh = setRotVals(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);
			sh.beginShape(PConstants.TRIANGLE);				      
				setStrokeAndFillPole(sh);
				shgl_vertexf(sh,rsThet, height, rcThet );
				shgl_vertexf(sh,0, height, 0 );
				shgl_vertexf(sh,rsThet2, height, rcThet2 );
			sh.endShape(PConstants.CLOSE);
			shRes.addChild(sh);
			
			if(drawBottom){
				sh = setRotVals(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);				
				sh.beginShape(PConstants.TRIANGLE);
					setStrokeAndFillPole(sh);
					shgl_vertexf(sh,rsThet, 0, rcThet );
					shgl_vertexf(sh,0, 0, 0 );
					shgl_vertexf(sh,rsThet2, 0, rcThet2);
				sh.endShape(PConstants.CLOSE);
				shRes.addChild(sh);
			}
		}//for i
		return shRes;
	}//drawPole
	public int buildQuadShape(float[] transVal, int numX, int btPt){
		PShape sh = makeShape(transVal[0],transVal[1],transVal[2]);
		sh.beginShape(PConstants.QUAD);
			setStrokeAndFillBody(sh);
			for(int i = 0; i < numX; ++i){
				shgl_vertex(sh,boatRndr[btPt][0]);shgl_vertex(sh,boatRndr[btPt][1]);shgl_vertex(sh,boatRndr[btPt][2]);shgl_vertex(sh,boatRndr[btPt][3]);btPt++;
			}//for i				
		sh.endShape(PConstants.CLOSE);
		boat.addChild(sh);		
		return btPt;
	}
	//create shape and set up initial configuration
	private PShape makeShape(float tx, float ty, float tz){
		PShape sh = p. createShape();
		sh.getVertexCount(); 
		sh.translate(tx,ty,tz);		
		return sh;
	}
		

	private void setStrokeAndFillPole(PShape sh){
		p.setColorValFillAmbSh(sh, mastColor);
		sh.stroke(mastStClr[0],mastStClr[1],mastStClr[2],mastStClr[3]);
		sh.emissive(mastStClr[0],mastStClr[1],mastStClr[2]);
		sh.shininess(1.0f);
	}
	
	private void setStrokeAndFillBody(PShape sh){
		p.setColorValFillAmbSh(sh, boatColor);
		sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeColor[3]);
		sh.emissive(strokeColor[0],strokeColor[1],strokeColor[2]);
		sh.shininess(1.0f);
	}	

	//public void shgl_vTextured(PShape sh, myPointf P, float u, float v) {sh.vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);}                          // vertex with texture coordinates
	public void shgl_vertexf(PShape sh, float x, float y, float z){sh.vertex(x,y,z);}	 // vertex for shading or drawing
	public void shgl_vertex(PShape sh, myPointf P){sh.vertex(P.x,P.y,P.z);}	 // vertex for shading or drawing
	public void shgl_normal(PShape sh, myVectorf V){sh.normal(V.x,V.y,V.z);	} // changes normal for smooth shading
	
	public void buildBoat(){
		int numZ = boatVerts[0].length, numX = boatVerts.length;
		int btPt = 0;
		float[] tmpAra = new float[]{0,1,0}, tmpAra2 = new float[]{0,1.5f,0};
		for(int j = 0; j < numZ-1; ++j){
			btPt = buildQuadShape(tmpAra, numX, btPt);
		}//for j
		for(int i = 0; i < numX; ++i){	
			buildBodyBottom(boatVerts,i, numZ, numX);	
		}//for i	
		for(int j = 0; j < numZ-1; ++j){
			btPt = buildQuadShape( tmpAra, 1, btPt);
			btPt = buildQuadShape( tmpAra, 1, btPt);		
		}//for j		
		//draw rear and front castle
		for(int j = 0; j < 27; ++j){
			btPt = buildQuadShape( tmpAra2, 1, btPt);
		}
		
	}//buildShape
	
	public void buildBodyBottom(myPointf[][] boatVerts, int i, int numZ, int numX){
		PShape sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.TRIANGLE);			
			setStrokeAndFillBody(sh);
			sh.vertex(boatVerts[i][numZ-1].x, boatVerts[i][numZ-1].y, 	boatVerts[i][numZ-1].z);	sh.vertex(0, 1, numZ-2);	sh.vertex(boatVerts[(i+1)%numX][numZ-1].x, boatVerts[(i+1)%numX][numZ-1].y, 	boatVerts[(i+1)%numX][numZ-1].z);	
		sh.endShape(PConstants.CLOSE);
		boat.addChild(sh);			

		sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.QUAD);		
			setStrokeAndFillBody(sh);
			sh.vertex(boatVerts[i][0].x, boatVerts[i][0].y, boatVerts[i][0].z);sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z -.5f);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z -.5f);sh.vertex(boatVerts[(i+1)%numX][0].x, boatVerts[(i+1)%numX][0].y, 	boatVerts[(i+1)%numX][0].z );
		sh.endShape(PConstants.CLOSE);
		boat.addChild(sh);			
		
		sh = makeShape(0,1,0);		
		sh.beginShape(PConstants.TRIANGLE);		
			setStrokeAndFillBody(sh);
			sh.vertex(boatVerts[i][0].x * .75f, boatVerts[i][0].y * .75f, boatVerts[i][0].z  -.5f);	sh.vertex(0, 0, boatVerts[i][0].z - 1);	sh.vertex(boatVerts[(i+1)%numX][0].x * .75f, boatVerts[(i+1)%numX][0].y * .75f, 	boatVerts[(i+1)%numX][0].z  -.5f);	
		sh.endShape(PConstants.CLOSE);		
		boat.addChild(sh);
	}
	
	//build boat's body points
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
	

	
}
