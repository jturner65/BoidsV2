package Boids2_PKG;

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

	public mySphereRndrObj(Boids_2 _p, myBoids3DWin _win, int _type) {	super(_p, _win, _type);	}
	@Override
	protected void initMainColor() {/**no shared colors across all spheres**/}
	@Override
	protected void initFlkColor() {
		for(int j=0;j<3;++j){sphrStrkClrs[type][j] = (int) (sphrFillClrs[type][j]/clrStrkDiv[type]);	}
		sphrStrkClrs[type][3] = 255;			//stroke alpha	
		flockColor = new myRndrObjClr(p);		
		flockColor.setClrVal("fill", sphrFillClrs[type]);
		flockColor.setClrVal("stroke", sphrStrkClrs[type]);
		flockColor.setClrVal("spec", specClr);
		flockColor.setClrVal("emit", sphrEmitClrs[type]);
//		flockColor.setClrVal("amb", _clrs[4]);
		flockColor.setClrVal("strokeWt", 1.0f);
		flockColor.setClrVal("shininess", 5.0f);
		flockColor.enableFill();
		flockColor.disableStroke();//flockColor.enableStroke();//
		flockColor.enableEmissive();
		flockColor.enableSpecular();
		//flockColor.enableAmbient();
		flockColor.enableShine();	
	}

	//build geometry of object
	@Override
	protected void initGeometry(){
		//global setup for this object type
		if(!made){				initObjGeometry();}//if not made yet initialize geometry to build this object
		//individual per-flock-type instancing - need to not be static since window can change (can call same type of flock in different myDispWindows)
		initInstObjGeometry();		
	}

	@Override
	protected void initObjGeometry() {		
		//base colors for all spheres, if any exist
		//initMainColor();
		made = true;
	}

	@Override
	protected void initInstObjGeometry() {
		p.sphereDetail(5);
		objRep = p.createShape(PConstants.SPHERE, 5.0f); 
		initFlkColor();
		//call shSetPaintColors since we need to use set<type> style functions of Pshape when outside beginShape-endShape
		flockColor.shSetPaintColors(objRep);
	}

	//no need for specific object-building function for spheres
	@Override
	protected void buildObj() {	}
	//nothing special (per-frame) for sphere render object
	@Override
	protected void drawMeIndiv(float animCntr) {}

}//class mySphereRndrObj
