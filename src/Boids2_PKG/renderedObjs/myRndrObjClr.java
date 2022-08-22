package Boids2_PKG.renderedObjs;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.my_procApplet;
import processing.core.PShape;

//class that will hold the relevant information for a particular color 
//configuration for rendering, with functions to render to a PShape, as well as a passed PApplet instance
public class myRndrObjClr{
	protected static IRenderInterface p;	
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
	
	public myRndrObjClr(IRenderInterface _p){
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
		//init all flags as true
		initFlags();
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
		else {		sh.noFill();}
		if(getFlags(strokeIDX)){
			sh.strokeWeight(strkWt);
			sh.stroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeAlpha);
		} else {			sh.noStroke();		}
		if(getFlags(specIDX)){sh.specular(specColor[0],specColor[1],specColor[2]);}
		if(getFlags(emitIDX)){sh.emissive(emitColor[0],emitColor[1],emitColor[2]);}
		if(getFlags(ambIDX)){sh.ambient(ambColor[0],ambColor[1],ambColor[2]);}
		if(getFlags(shnIDX)){sh.shininess(shininess);}
	}
	//return hex rep of passed color
	private int getHexClr(int[] vals, int alpha){
		int res = 0;
		if (alpha >= 0){res = ((alpha & 0xFF)<<24);}//A
		res += (vals[0] & 0xFF) << 16;//R
		res += (vals[1] & 0xFF) << 8;//G
		res += (vals[2] & 0xFF) ;//B		
		return res;		
	}
	
	/**
	 * instance all activated colors in passed PShape for constructed PShape, set all colors
	 * @param sh
	 */
	public void shSetPaintColors(PShape sh){
		if(getFlags(fillIDX)){sh.setFill(getHexClr(fillColor,fillAlpha));}
		else {		sh.setFill(false);}
		if(getFlags(strokeIDX)){
			sh.setStrokeWeight(strkWt);			
			sh.setStroke(getHexClr(strokeColor,strokeAlpha));
		} else {	sh.setStroke(false);}
		if(getFlags(specIDX)){sh.setSpecular(getHexClr(specColor,-1));}
		if(getFlags(emitIDX)){sh.setEmissive(getHexClr(emitColor,-1));}
		if(getFlags(ambIDX)){sh.setAmbient(getHexClr(ambColor,-1));}
		if(getFlags(shnIDX)){sh.setShininess(shininess);}
	}
	//instance all activated colors globally
	public void paintColors(){
		if(getFlags(fillIDX)){p.setFill(fillColor[0],fillColor[1],fillColor[2],fillAlpha);}
		else {		p.setNoFill();}
		if(getFlags(strokeIDX)){
			p.setStrokeWt(strkWt);
			p.setStroke(strokeColor[0],strokeColor[1],strokeColor[2],strokeAlpha);
		} else {			p.noStroke();		}
		if(getFlags(specIDX)){((my_procApplet) p).specular(specColor[0],specColor[1],specColor[2]);}
		if(getFlags(emitIDX)){((my_procApplet) p).emissive(emitColor[0],emitColor[1],emitColor[2]);}
		if(getFlags(ambIDX)){((my_procApplet) p).ambient(ambColor[0],ambColor[1],ambColor[2]);}
		if(getFlags(shnIDX)){((my_procApplet) p).shininess(shininess);}
	}
	//apply this color's fill color to applet
	public void fillMenu(float mult){
		p.setFill((int)(mult*fillColor[0]),(int)(mult*fillColor[1]),(int)(mult*fillColor[2]),255);
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
	
	private void initFlags(){flags = new int[1 + numFlags/32];for(int i =0; i<numFlags;++i){setFlags(i,true);}}
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
