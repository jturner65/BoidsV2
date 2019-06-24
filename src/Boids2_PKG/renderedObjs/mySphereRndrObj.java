package Boids2_PKG.renderedObjs;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.myDispWindow;
import processing.core.PConstants;

public class mySphereRndrObj extends myRenderObj {
	//if overall geometry has been made or not
	private static boolean made;
	//divisors for stroke color from fill color
	private static float[] clrStrkDiv = new float[]{.8f,5.0f,.75f,4.0f,.3f};
	//boat colors - get from load? TODO
	private static int[][] 
			sphrFillClrs = new int[][]{{110, 65, 30,255},	{30, 30, 30,255},	{130, 22, 10,255},	{22, 230, 10,255},	{22, 10, 130,255}},
			sphrStrkClrs = new int[5][4],//overridden to be fraction of fill color
			sphrEmitClrs = new int[][]{sphrFillClrs[0],		sphrFillClrs[1],	sphrFillClrs[2],	sphrFillClrs[3],	sphrFillClrs[4]};
	private static final int[] specClr = new int[]{255,255,255,255};
	private static final float strkWt = 1.0f;
	private static final float shn = 5.0f;

	public mySphereRndrObj(my_procApplet _p, myDispWindow _win, int _type) {	
		super(_p, _win, _type);	 
		made = initGeometry(made);
	}//ctor
	
	@Override
	protected void initMainColor() {/**no shared colors across all spheres**/}
	@Override
	protected void initFlkColor() {
		flockColor = makeColor(sphrFillClrs[type], sphrStrkClrs[type], sphrEmitClrs[type], new int[]{0,0,0,0}, specClr,clrStrkDiv[type], strkWt, shn);
		flockColor.disableAmbient();
		flockColor.disableStroke();
	}
	
	//no custom geometry for sphere
	@Override
	protected void initObjGeometry() {	}
	//since this is a sphere, override default to create a different object type (instead of group)
	@Override
	protected void initInstObjGeometry() {
		p.sphereDetail(5);
		objRep = p.createShape(PConstants.SPHERE, 5.0f); 
		initFlkColor();
		//call shSetPaintColors since we need to use set<type> style functions of Pshape when outside beginShape-endShape
		flockColor.shSetPaintColors(objRep);
	}
	
	@Override
	protected void initInstObjGeometryIndiv(){}

	//no need for specific object-building function for spheres
	@Override
	protected void buildObj() {	}
	//nothing special (per-frame) for sphere render object
	@Override
	protected void drawMeIndiv(int idx) {}

}//class mySphereRndrObj
