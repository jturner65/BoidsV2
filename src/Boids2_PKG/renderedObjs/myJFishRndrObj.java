package Boids2_PKG.renderedObjs;

import Boids2_PKG.flocks.boids.myBoid;
import Boids2_PKG.renderedObjs.base.myRenderObj;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import processing.core.PConstants;
import processing.core.PShape;

//jellyfish pshape, with multiple component shapes that are animated
public class myJFishRndrObj extends myRenderObj {
	//static vals set in here because we want boid "species"-wide color settings
	//if overall geometry has been made or not
	private static boolean made;
	
	private static PShape[][] bodyAra = new PShape[5][];
	private int numTentacles = 5;
	
	//primary object color (same across all types of boids); 
	private static myRndrObjClr mainColor;	
	
	private static myRndrObjClr[] allFlockColors = new myRndrObjClr[5];
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

	public myJFishRndrObj(IRenderInterface _p, Base_DispWindow _win, int _type)  {	
		super(_p, _win, _type);	 
		emitMod = .85f;
	}//ctor
	
	/**
	 * Get per-species boolean defining whether or not species-wide geometry has been completed. 
	 * Each species should (class inheriting from this class) should have its own static 'made' boolean,
	 * which this provides access to.
	 */
	@Override
	protected boolean getObjMade() {return made;}

	/**
	 * Set per-species boolean defining whether or not species-wide geometry has been completed. 
	 * Each species should (class inheriting from this class) should have its own static 'made' boolean,
	 * which this provides access to.
	 */
	@Override
	protected void setObjMade(boolean isMade) {made = isMade;}
	
	/**
	 * Get the type of the main mesh to be created
	 * @return a constant defining the type of PShape being created
	 */	
	@Override
	protected int getMainMeshType() {return PConstants.GROUP;}
	
	@Override
	protected void initMainColor(){
		mainColor = makeColor(jFishFillClrs[baseFishIDX], jFishStrokeClrs[baseFishIDX], jFishEmitClrs[baseFishIDX], new int[]{0,0,0,0}, jFishSpecClr,clrStrkDiv[baseFishIDX], strkWt, shn);
		//mainColor.disableStroke();
		mainColor.disableAmbient();
		// have all flock colors available initially to facilitate first-time creation
		for (int i=0;i<allFlockColors.length;++i) {
			allFlockColors[i] =  makeColor(jFishFillClrs[i], jFishStrokeClrs[i], jFishEmitClrs[i], new int[]{0,0,0,0}, jFishSpecClr,clrStrkDiv[i], strkWt, shn);
			//allFlockColors[i].disableStroke();
			allFlockColors[i].disableAmbient();
		}
	}			

	@Override
	protected void initFlkColor() {
		flockColor = allFlockColors[type];
	}
	
	/**
	 * builds geometry for object to be instanced - only perform once per object type 
	 */
	@Override
	protected void initObjGeometry() {
		//make all bodies of a cycle of animation - make instances in buildObj
		for(int i=0;i<bodyAra.length;++i) {
			bodyAra[i] = new PShape[myBoid.numAnimFrames];
			float sclMult;		//vary this based on animation frame
			float radAmt = (MyMathUtils.TWO_PI_F/(1.0f*myBoid.numAnimFrames));
			p.setSphereDetail(20);
			for(int a=0; a<myBoid.numAnimFrames; ++a){//for each frame of animation			
				bodyAra[i][a] = createBaseShape(PConstants.GROUP);
				PShape indiv = createBaseShape(PConstants.SPHERE, 5.0f);
				sclMult = (float) ((Math.sin(a * radAmt) * .25f) +1.0f);
				indiv.scale(sclMult, sclMult, 1.0f/(sclMult * sclMult));
				//win.getMsgObj().dispInfoMessage("myJFishRndrObj","buildObj","a : " + a + " sclMult : " + sclMult);
				//call shSetPaintColors since we need to use set<type> style functions of Pshape when outside beginShape-endShape
				allFlockColors[i].shSetShapeColors(indiv);		
				bodyAra[i][a].addChild(indiv);
			}	
		}
		p.setSphereDetail(5);			
	}
	//any instance specific, jelly-fish specific geometry setup goes here (textures, sizes, shapes, etc)
	@Override
	protected void initInstObjGeometryIndiv() {
		//build instance	
	}//initInstObjGeometry

	@Override
	protected void buildObj() {
		//build the boid's body geometry here - called at end of initInstObjGeometry
	}

	@Override
	protected void drawMeIndiv(int animIDX) {
		//draw animation index-specified deformed "jellyfish"
		((my_procApplet) p).shape(bodyAra[type][animIDX]);
	}

}
