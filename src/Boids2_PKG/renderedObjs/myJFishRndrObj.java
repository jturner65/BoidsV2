package Boids2_PKG.renderedObjs;

import Boids2_PKG.boids.myBoid;
import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.myDispWindow;
import processing.core.*;

//jellyfish pshape, with multiple component shapes that are animated
public class myJFishRndrObj extends myRenderObj {
	//static vals set in here because we want boid "species"-wide color settings
	//if overall geometry has been made or not
	private static boolean made;
	
	private PShape[] bodyAra;
	private int numTentacles = 5;
	
	//primary object color (same across all types of boids); 
	private static myRndrObjClr mainColor;	
	//base IDX - this is idx to main color for all jellyfish
	private static final int baseFishIDX = 0;
	//divisors for stroke color from fill color
	private static float[] clrStrkDiv = new float[]{.8f,.8f,.8f,.8f,.8f};
	//boid colors - get from load? TODO
	private static int[][] 
			jFishFillClrs = new int[][]{{180, 125, 100,255},	{90, 130, 80, 255},	{180, 82, 90,255},	{190, 175, 60,255},	{50, 90, 240,255}},
			jFishStrokeClrs = new int[5][4],//overridden to be fraction of fill color19
			jFishEmitClrs = new int[][]{jFishFillClrs[0],		jFishFillClrs[1],	jFishFillClrs[2],	jFishFillClrs[3],	jFishFillClrs[4]};
	private static final int[] jFishSpecClr = new int[]{255,255,255,255};
	
	private static final float strkWt = .1f;
	private static final float shn = 5.0f;

	public myJFishRndrObj(my_procApplet _p, myDispWindow _win, int _type)  {	
		super(_p, _win, _type);	 
		emitMod = .85f;
		made = initGeometry(made);
	}//ctor
	
	@Override
	protected void initMainColor(){
		mainColor = makeColor(jFishFillClrs[baseFishIDX], jFishStrokeClrs[baseFishIDX], jFishEmitClrs[baseFishIDX], new int[]{0,0,0,0}, jFishSpecClr,clrStrkDiv[baseFishIDX], strkWt, shn);
		//mainColor.disableStroke();
		mainColor.disableAmbient();
	}			

	@Override
	protected void initFlkColor() {
		flockColor = makeColor(jFishFillClrs[type], jFishStrokeClrs[type], jFishEmitClrs[type], new int[]{0,0,0,0}, jFishSpecClr,clrStrkDiv[type], strkWt, shn);
		//flockColor.disableStroke();
		flockColor.disableAmbient();
	}
	
	//builds geometry for object to be instanced - only perform once per object type 
	@Override
	protected void initObjGeometry() {

	}
	//any instance specific, jelly-fish specific geometry setup goes here (textures, sizes, shapes, etc)
	@Override
	protected void initInstObjGeometryIndiv() {
		//make all bodies of a cycle of animation - make instances in buildObj
		bodyAra = new PShape[myBoid.numAnimFrames];
		for(int a=0; a<myBoid.numAnimFrames; ++a){
			bodyAra[a] = p.createShape(PConstants.GROUP); 
		}	
		//build instance
	
	}//initInstObjGeometry

	@Override
	protected void buildObj() {
		//build the boid's body geometry here - called at end of initInstObjGeometry
		float sclMult;		//vary this based on animation frame
		float radAmt=  (p.TWO_PI/(1.0f*myBoid.numAnimFrames));
		p.sphereDetail(20);
		for(int a=0; a<myBoid.numAnimFrames; ++a){//for each frame of animation			
			PShape indiv = p.createShape(PConstants.SPHERE, 5.0f);
			sclMult = (p.sin(a * radAmt) * .25f) +1.0f;
			indiv.scale(sclMult, sclMult, 1.0f/(sclMult * sclMult));
			//p.outStr2Scr("a : " + a + " sclMult : " + sclMult);
			//call shSetPaintColors since we need to use set<type> style functions of Pshape when outside beginShape-endShape
			flockColor.shSetPaintColors(indiv);		
			bodyAra[a].addChild(indiv);
		}	
		p.sphereDetail(5);
	}

	@Override
	protected void drawMeIndiv(int animIDX) {
		//int idx = calcAnimIDX(animCntr);
		p.shape(bodyAra[animIDX]);

	}

}
