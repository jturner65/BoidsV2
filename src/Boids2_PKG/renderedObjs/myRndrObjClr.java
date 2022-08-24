package Boids2_PKG.renderedObjs;

import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_UI_Objects.my_procApplet;
import processing.core.PShape;

//class that will hold the relevant information for a particular color 
//configuration for rendering, with functions to render to a PShape, as well as a passed PApplet instance
public class myRndrObjClr{
	protected static IRenderInterface p;	
	protected final static int[] tmpInit = new int[]{255,255,255,255};
	//alpha values for fill and stroke colors
	protected int[] alphas;
	//all values for various colors as hex
	protected int[] hexColors;
	protected float shininess, strkWt;
	
	protected int[] flags;		//bit flags for color
	public static final int 
				fillIDX 		= 0,
				strokeIDX 		= 1,
				emitIDX 		= 2,
				specIDX 		= 3,
				ambIDX 			= 4,
	//idxs above for colors array of arrays
				shnIDX			= 5;
	protected static final int numColors = 5;
	
	protected static final int numFlags = 6;
	
	public myRndrObjClr(IRenderInterface _p){
		p=_p;
		shininess = 1.0f;
		strkWt = 1.0f;
		hexColors = new int[numColors];
		alphas = new int[numColors];
		//RGBA (alpha ignored as appropriate) - init all as white
		for(int i=0; i<numColors;++i) {
			//initialize colors to be either ARGB or RGB white
			setColorsFromArray(tmpInit, i, i < 2);
		}
		//init all flags as true
		initFlags();
	}
	
	private void setColorsFromArray(int[] _srcClr, int _idx, boolean _hasAlpha) {
		if(_hasAlpha) {			alphas[_idx] = _srcClr[3];} 
		else {					alphas[_idx] = -1;}
		hexColors[_idx] = p.getClrAsHex(_srcClr, alphas[_idx]);	
	}

	public void setClrVal(String type, int[] _clr){	setClrVal(type, _clr, -1);	}
	public void setClrVal(String type, float _val){	setClrVal(type, null, _val);}
	protected void setClrVal(String type, int[] clr, float _val){
		switch(type){
		case "fill" : 		{setColorsFromArray(clr, fillIDX, true); break;}
		case "stroke" :		{setColorsFromArray(clr, strokeIDX, true); break;}
		case "emit" : 		{setColorsFromArray(clr, emitIDX, false); break;}
		case "spec" : 		{setColorsFromArray(clr, specIDX, false); break;}
		case "amb" :  		{setColorsFromArray(clr, ambIDX, false); break;}
		case "shininess" : 	{shininess = _val; break;}
		case "strokeWt" :	{strkWt = _val; break;}
		default : {break;}
		}		
	}
	
	public float getStrkWt(){return strkWt;}
	public float getShine(){return shininess;}
	public int[] getClrAraByType(String type){
		switch(type){
		case "fill" : 		{return p.getClrFromHex(hexColors[fillIDX]);}
		case "stroke" :		{return p.getClrFromHex(hexColors[strokeIDX]);}
		case "emit" : 		{return p.getClrFromHex(hexColors[emitIDX]);}
		case "spec" : 		{return p.getClrFromHex(hexColors[specIDX]);}
		case "amb" :  		{return p.getClrFromHex(hexColors[ambIDX]);}
		default : {return null;}
		}	
	}
	public int getHexClrByType(String type){
		switch(type){
		case "fill" : 		{return hexColors[fillIDX];}
		case "stroke" :		{return hexColors[strokeIDX];}
		case "emit" : 		{return hexColors[emitIDX];}
		case "spec" : 		{return hexColors[specIDX];}
		case "amb" :  		{return hexColors[ambIDX];}
		default : {return 0x0;}
		}	
	}
	/**
	 * instance all activated colors in passed PShape for constructed PShape, set all colors.  This is called 
	 * between "beginShape/endShape"
	 * @param sh
	 */
	public void shPaintColors(PShape sh){
		if(getFlags(fillIDX)){sh.fill(hexColors[fillIDX], alphas[fillIDX]);}
		else {		sh.noFill();}
		if(getFlags(strokeIDX)){
			sh.strokeWeight(strkWt);
			sh.stroke(hexColors[strokeIDX],alphas[strokeIDX]);
		} else {			sh.noStroke();		}
		if(getFlags(specIDX)){sh.specular(hexColors[specIDX]);}
		if(getFlags(emitIDX)){sh.emissive(hexColors[emitIDX]);}
		if(getFlags(ambIDX)){sh.ambient(hexColors[ambIDX]);}
		if(getFlags(shnIDX)){sh.shininess(shininess);}
	}

	
	/**
	 * instance all activated colors in passed PShape for constructed PShape, set all colors.  Not between "beginShape/endShape"
	 * @param sh
	 */
	public void shSetShapeColors(PShape sh){
		if(getFlags(fillIDX)){sh.setFill(hexColors[fillIDX]);}
		else {		sh.setFill(false);}
		if(getFlags(strokeIDX)){
			sh.setStrokeWeight(strkWt);			
			sh.setStroke(hexColors[strokeIDX]);
		} else {	sh.setStroke(false);}
		if(getFlags(specIDX)){sh.setSpecular(hexColors[specIDX]);}
		if(getFlags(emitIDX)){sh.setEmissive(hexColors[emitIDX]);}
		if(getFlags(ambIDX)){sh.setAmbient(hexColors[ambIDX]);}
		if(getFlags(shnIDX)){sh.setShininess(shininess);}
	}
	
	/**
	 * instance all activated colors globally
	 */
	public void paintColors(){
		if(getFlags(fillIDX)){p.setFill(hexColors[fillIDX]);}
		else {		p.setNoFill();}
		if(getFlags(strokeIDX)){
			p.setStrokeWt(strkWt);
			p.setStroke(hexColors[strokeIDX]);
		} else {			p.noStroke();		}
		if(getFlags(specIDX)){((my_procApplet) p).specular(hexColors[specIDX]);}
		if(getFlags(emitIDX)){((my_procApplet) p).emissive(hexColors[emitIDX]);}
		if(getFlags(ambIDX)){((my_procApplet) p).ambient(hexColors[ambIDX]);}
		if(getFlags(shnIDX)){((my_procApplet) p).shininess(shininess);}
	}
	
	//apply this color's fill color to applet
	public void fillMenu(float mult){
		int[] clrAra = p.getClrFromHex(hexColors[fillIDX]);
		p.setFill((int)(mult*clrAra[0]),(int)(mult*clrAra[1]),(int)(mult*clrAra[2]),255);
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
