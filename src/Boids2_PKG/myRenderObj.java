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
	
	//primary object color (same across all types of boids); individual type colors defined in instance class
	public static myRndrObjClr mainColor;	
	
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

//facilitate handling fill, stroke, etc, colors for render objects
class myRndrObjClr{
	protected static Boids_2 p;	
	protected final static int[] tmpInit = new int[]{255,255,255};
	//values for color
	protected int[] fillColor, strokeColor, emitColor, specColor, ambColor;
	protected int fillAlpha, strokeAlpha;
	protected float shininess, strkWt;
	
	protected int[] flags;		//bit flags for color
	public static final int 
				fillIDX 		= 0,
				strokeIDX 		= 1,
				emitIDX 		= 2,
				specIDX 		= 3,
				shnIDX			= 4,
				ambIDX 			= 5;
	
	protected int numFlags = 6;
	
	public myRndrObjClr(Boids_2 _p){
		p=_p;
		shininess = 1.0f;
		strkWt = 1.0f;
		fillAlpha = 255;
		//RGBA (alpha ignored as appropriate) - init all as white
		fillColor = new int[3]; cpyClr(tmpInit, fillColor);
		strokeColor = new int[3];cpyClr(tmpInit, strokeColor);
		emitColor = new int[3];cpyClr(tmpInit, emitColor);
		specColor = new int[3];cpyClr(tmpInit, specColor);
		ambColor = new int[3];cpyClr(tmpInit, ambColor);
		//init all flags as false
		initFlags();
		//enable fill and stroke by default
		enableFill();
		enableStroke();
	}
	

	public void setClrVal(String type, int[] _clr){	setClrVal(type, _clr, -1);	}
	public void setClrVal(String type, float _val){	setClrVal(type, null, _val);}
	protected void setClrVal(String type, int[] clr, float _val){
		switch(type){
		case "fill" : 		{cpyClr(clr, fillColor); fillAlpha = clr[3]; break;}
		case "stroke" :		{cpyClr(clr, strokeColor); strokeAlpha = clr[3];break;}
		case "shininess" : 	{shininess = _val; break;}
		case "strokeWt" :	{strkWt = _val; break;}
		case "spec" : 		{cpyClr(clr, specColor); break;}
		case "emit" : 		{cpyClr(clr, emitColor); break;}
		case "amb" :  		{cpyClr(clr, ambColor); break;}
		default : {break;}
		}		
	}
	private void cpyClr(int[] src, int[] dest){	System.arraycopy(src, 0, dest, 0, dest.length);}

	public float getStrkWt(){return strkWt;}
	public float getShine(){return shininess;}
	public int[] getClrByType(String type){
		switch(type){
		case "fill" : 		{return fillColor;}
		case "stroke" :		{return strokeColor;}
		case "spec" : 		{return specColor;}
		case "emit" : 		{return emitColor;}
		case "amb" :  		{return ambColor;}
		default : {return null;}
		}	
	}

	//instance all activated colors in passed PShape
	public void shPaintColors(PShape sh){
		if(getFlags(fillIDX)){sh.fill(fillColor[0],fillColor[1],fillColor[2],fillAlpha);}
		if(getFlags(strokeIDX)){
			sh.strokeWeight(strkWt);
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeAlpha);
		}
		if(getFlags(specIDX)){sh.specular(specColor[0],specColor[1],specColor[2]);}
		if(getFlags(emitIDX)){sh.emissive(emitColor[0],emitColor[1],emitColor[2]);}
		if(getFlags(ambIDX)){sh.ambient(ambColor[0],ambColor[1],ambColor[2]);}
		if(getFlags(shnIDX)){sh.shininess(shininess);}
	}
	//instance all activated colors globally
	public void paintColors(){
		if(getFlags(fillIDX)){p.fill(fillColor[0],fillColor[1],fillColor[2],fillAlpha);}
		if(getFlags(strokeIDX)){
			p.strokeWeight(strkWt);
			p.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeAlpha);
		}
		if(getFlags(specIDX)){p.specular(specColor[0],specColor[1],specColor[2]);}
		if(getFlags(emitIDX)){p.emissive(emitColor[0],emitColor[1],emitColor[2]);}
		if(getFlags(ambIDX)){p.ambient(ambColor[0],ambColor[1],ambColor[2]);}
		if(getFlags(shnIDX)){p.shininess(shininess);}
	}
	
	public void setFlags(int idx, boolean val){setPrivFlag(flags, idx, val);}
	public boolean getFlags(int idx){return getPrivFlag(flags, idx);}
	
	public void enableFill(){setPrivFlag(flags, fillIDX, true);}
	public void enableStroke(){setPrivFlag(flags, strokeIDX, true);}
	public void enableEmissive(){setPrivFlag(flags, emitIDX, true);}
	public void enableSpecular(){setPrivFlag(flags, specIDX, true);}
	public void enableAmbient(){setPrivFlag(flags, ambIDX, true);}
	public void enableShine(){setPrivFlag(flags, shnIDX, true);}
	
	public void disableFill(){setPrivFlag(flags, fillIDX, false);}
	public void disableStroke(){setPrivFlag(flags, strokeIDX, false);}
	public void disableEmissive(){setPrivFlag(flags, emitIDX, false);}
	public void disableSpecular(){setPrivFlag(flags, specIDX, false);}
	public void disableAmbient(){setPrivFlag(flags, ambIDX, false);}
	public void disableShine(){setPrivFlag(flags, shnIDX, false);}
	
	private void initFlags(){flags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,false);}}
	//class-wide boolean flag accessor methods - static for size concerns
	protected static void setPrivFlag(int[] flags, int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		flags[flIDX] = (val ?  flags[flIDX] | mask : flags[flIDX] & ~mask);
		switch(idx){
			case fillIDX 	: { break;}	
			case strokeIDX 	: {	break;}	
			case emitIDX 	: {	break;}	
			case specIDX 	: {	break;}	
			case ambIDX 	: {	break;}	
		}				
	}//setFlags
	protected static boolean getPrivFlag(int[] flags, int idx){int bitLoc = 1<<(idx%32);return (flags[idx/32] & bitLoc) == bitLoc;}	
}//myRndrObjClr
