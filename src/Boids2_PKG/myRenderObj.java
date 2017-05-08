package Boids2_PKG;

import processing.core.PConstants;
import processing.core.PShape;

public abstract class myRenderObj {
	protected static Boids_2 p;	
	protected myBoids3DWin win;
	protected static final float pi4thrds = 4*PConstants.PI/3.0f, pi100th = .01f*PConstants.PI, pi6ths = PConstants.PI/6.0f, pi3rds = PConstants.PI/3.0f;
	//individual objRep-type pshapes	
	protected PShape objRep;										//1 shape for each type of objRep
	protected int type;												//type of flock this objRep represents
	
	//class to allow for prebuilding complex rendered representations of boids as pshapes
	public myRenderObj(Boids_2 _p, myBoids3DWin _win, int _type) {
		p=_p; win=_win; type = _type;
		initGeometry();
	}
	
	//initialize base and flock/team colors for this object
	protected abstract void initMainColor();
	protected abstract void initFlkColor();
	
	//build geometry of object
	protected abstract void initGeometry();
	//builds geometry for object to be instanced - only perform once per object type 
	protected abstract void initObjGeometry();
	//builds flock specific instance of boid render rep, including colors, textures, etc.
	protected abstract void initInstObjGeometry();
	
	//build the instance of a particular object
	protected abstract void buildObj();
	
	//create an individual shape and set up initial configuration - also perform any universal initial shape code
	protected PShape makeShape(float tx, float ty, float tz){
		PShape sh = p. createShape();
		sh.getVertexCount(); 
		sh.translate(tx,ty,tz);		
		return sh;
	}//makeShape

	
	//instance a pshape and draw it
	public void drawMe(float animCntr){
		p.shape(objRep);
		drawMeIndiv(animCntr);
	}
	//draw object
	protected abstract void drawMeIndiv(float animCntr);
	
	//public void shgl_vTextured(PShape sh, myPointf P, float u, float v) {sh.vertex((float)P.x,(float)P.y,(float)P.z,(float)u,(float)v);}                          // vertex with texture coordinates
	public void shgl_vertexf(PShape sh, float x, float y, float z){sh.vertex(x,y,z);}	 // vertex for shading or drawing
	public void shgl_vertex(PShape sh, myPointf P){sh.vertex(P.x,P.y,P.z);}	 // vertex for shading or drawing
	public void shgl_normal(PShape sh, myVectorf V){sh.normal(V.x,V.y,V.z);	} // changes normal for smooth shading

}//abstract class myRenderObj

