package Boids2_PKG.renderedObjs;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_Utils_Objects.vectorObjs.myPointf;
import base_Utils_Objects.vectorObjs.myVectorf;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;

public abstract class myRenderObj {
	protected static my_procApplet p;	
	protected myDispWindow win;
	protected static final float pi4thrds = 4*PConstants.PI/3.0f, pi100th = .01f*PConstants.PI, pi6ths = PConstants.PI/6.0f, pi3rds = PConstants.PI/3.0f;
	//individual objRep-type pshapes	
	protected PShape objRep;										//1 shape for each type of objRep
	protected int type;												//type of flock this objRep represents
	//color defined for this particular boid-type - also query for menu color
	protected myRndrObjClr flockColor;
	protected float emitMod = 1.0f;
	//class to allow for prebuilding complex rendered representations of boids as pshapes
	public myRenderObj(my_procApplet _p, myDispWindow _win, int _type) {
		p=_p; win=_win; type = _type;
	}
	
	//initialize base and flock/team colors for this object
	protected abstract void initMainColor();
	protected abstract void initFlkColor();	
	//build geometry of object
	protected boolean initGeometry(boolean isMade){
		//global setup for this object type
		if(!isMade){		
			//set up species-wide geometry
			initObjGeometry();
			//base colors for all boids of this species
			initMainColor();			
		}//if not made yet initialize geometry to build this object
		//individual per-flock-type setup - need to not be static since window can change
		initInstObjGeometry();		
		return true;		
	}
	//builds geometry for object to be instanced - only perform once per object type 
	protected abstract void initObjGeometry();
	//builds flock-specific geometry and instances
	protected void initInstObjGeometry(){
		objRep = p.createShape(PConstants.GROUP); 
		
		//any per-flock (child class) setup required
		initInstObjGeometryIndiv();
		
		initFlkColor();	
		buildObj();			
	}//	initInstObjGeometry
	//builds flock specific instance of boid render rep, including colors, textures, etc.
	protected abstract void initInstObjGeometryIndiv();	
	
	//build the instance of a particular object
	protected abstract void buildObj();
	
	//create an individual shape and set up initial configuration - also perform any universal initial shape code
	protected PShape makeShape(float tx, float ty, float tz){
		PShape sh = p.createShape();
		//sh.getVertexCount(); 
		sh.translate(tx,ty,tz);		
		return sh;
	}//makeShape

	protected myRndrObjClr makeColor(int[] fill, int[] stroke, int[] emit, int[] amb, int[] spec, float divis, float stWt, float shn){
		for(int j=0;j<3;++j){
			stroke[j] = (int) (fill[j]/divis);	
			emit[j] = (int)(emitMod * emit[j]);
		}
		stroke[3] = 255;			//stroke alpha
		myRndrObjClr clr = new myRndrObjClr(p);
		clr.setClrVal("fill", fill);
		clr.setClrVal("stroke", stroke);
		clr.setClrVal("spec", spec);
		clr.setClrVal("emit", emit);
		clr.setClrVal("amb", amb);
		clr.setClrVal("strokeWt", stWt);
		clr.setClrVal("shininess", shn);
		return clr;
	}
	
	//build shape from object points
	protected int buildQuadShape(float[] transVal, int numX, int btPt, myPointf[][] objRndr){
		PShape sh = makeShape(transVal[0],transVal[1],transVal[2]);
		sh.beginShape(PConstants.QUAD);
			flockColor.shPaintColors(sh);
			for(int i = 0; i < numX; ++i){
				shgl_vertex(sh,objRndr[btPt][0]);shgl_vertex(sh,objRndr[btPt][1]);shgl_vertex(sh,objRndr[btPt][2]);shgl_vertex(sh,objRndr[btPt][3]);btPt++;
			}//for i				
		sh.endShape(PConstants.CLOSE);
		objRep.addChild(sh);		
		return btPt;
	}
	
	protected PShape setRotVals(myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec, float[] rotAra3){	
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
	
	//build a pole
	protected PShape buildPole(int poleNum, myRndrObjClr clr, float rad, float height, boolean drawBottom, myVectorf transVec, myVectorf scaleVec, float[] rotAra, myVectorf trans2Vec, float[] rotAra2, myVectorf trans3Vec, float[] rotAra3){
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
				clr.shPaintColors(sh);
				shgl_vertexf(sh,rsThet, 0, rcThet );
				shgl_vertexf(sh,rsThet, height,rcThet);
				shgl_vertexf(sh,rsThet2, height,rcThet2);
				shgl_vertexf(sh,rsThet2, 0, rcThet2);
			sh.endShape(PConstants.CLOSE);	
			shRes.addChild(sh);

			sh = setRotVals(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);
			sh.beginShape(PConstants.TRIANGLE);				      
				clr.shPaintColors(sh);
				shgl_vertexf(sh,rsThet, height, rcThet );
				shgl_vertexf(sh,0, height, 0 );
				shgl_vertexf(sh,rsThet2, height, rcThet2 );
			sh.endShape(PConstants.CLOSE);
			shRes.addChild(sh);
			
			if(drawBottom){
				sh = setRotVals(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);				
				sh.beginShape(PConstants.TRIANGLE);
					clr.shPaintColors(sh);
					shgl_vertexf(sh,rsThet, 0, rcThet );
					shgl_vertexf(sh,0, 0, 0 );
					shgl_vertexf(sh,rsThet2, 0, rcThet2);
				sh.endShape(PConstants.CLOSE);
				shRes.addChild(sh);
			}
		}//for i
		return shRes;
	}//drawPole	
	
	//set background for menu color, darkening a bit so that bright colors are still visible on white background
	public void setMenuColor(){
		flockColor.fillMenu(.9f);
	}
	//instance a pshape and draw it
	public void drawMe(int animIDX, int objID){
		p.shape(objRep);
		drawMeIndiv(animIDX);
	}
	//draw object
	protected abstract void drawMeIndiv(int animIDX);
	
	//public void shgl_vTextured(PShape sh, myPointf P, float u, float v) {sh.vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);}                          // vertex with texture coordinates
	public void shgl_vertexf(PShape sh, float x, float y, float z){sh.vertex(x,y,z);}	 // vertex for shading or drawing
	public void shgl_vertex(PShape sh, myPointf P){sh.vertex(P.x,P.y,P.z);}	 // vertex for shading or drawing
	public void shgl_normal(PShape sh, myVectorf V){sh.normal(V.x,V.y,V.z);	} // changes normal for smooth shading

}//abstract class myRenderObj

