package Boids2_PKG.renderedObjs.base;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.base.Base_DispWindow;
import base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import processing.core.PShape;
import processing.core.PConstants;

/**
 * Base class describing a rendered mesh object
 * @author John Turner
 *
 */
public abstract class Base_RenderObj {
	protected static IRenderInterface p;	
	protected Base_DispWindow win;
	protected static final float
		pi4thrds = 4*MyMathUtils.THIRD_PI_F, 
		pi100th = .01f*MyMathUtils.PI_F, 
		pi6ths = .5f*MyMathUtils.THIRD_PI_F;
	//individual objRep-type pshapes	
	protected PShape objRep;										//1 shape for each type of objRep
	protected int type;												//type of flock this objRep represents
	//color defined for this particular boid-type - also query for menu color
	protected RenderObj_Clr flockColor;
	protected float emitMod = 1.0f;
	
	/**
	 * # of animation frames to use for this rendered object
	 */
	public static int numAnimFrames = 90;
	
	
	//class to allow for prebuilding complex rendered representations of boids as pshapes
	public Base_RenderObj(IRenderInterface _p, Base_DispWindow _win, int _type) {
		p=_p; win=_win; type = _type;
		setObjMade(initGeometry());
	}
	
	/**
	 * build geometry of object, including 1-time species-specific setup
	 * @return true, denoting 1-time setup is complete
	 */
	protected final boolean initGeometry(){
		//global setup for this object type
		if(!getObjMade()){		
			//base colors for all boids/flocks of this species
			initMainColor();
			//set up species-wide geometry
			initObjGeometry();
		}//if not made yet initialize geometry to build this object
		//individual per-obj instance-type setup - need to not be static since window can change
		initInstObjGeometry();		
		return true;		
	}
	
	/**
	 * Get per-species boolean defining whether or not species-wide geometry has been completed. 
	 * Each species should (class inheriting from this class) should have its own static 'made' boolean,
	 * which this provides access to.
	 */
	protected abstract boolean getObjMade();
	/**
	 * Set per-species boolean defining whether or not species-wide geometry has been completed. 
	 * Each species should (class inheriting from this class) should have its own static 'made' boolean,
	 * which this provides access to.
	 */
	protected abstract void setObjMade(boolean isMade);

	/**
	 * initialize base colors for this object - only perform once per object type/species  
	 */
	protected abstract void initMainColor();
	
	/**
	 * Builds geometry for species of object to be instanced - only perform once per object type/species 
	 */
	protected abstract void initObjGeometry();

	
	/**
	 * Builds instance of rendered object
	 */
	protected final void initInstObjGeometry(){
		objRep = createBaseShape(getMainMeshType()); 
		//set per-flock color 
		initFlkColor();
		//any per-flock (child class) setup required	
		initInstObjGeometryIndiv();
		buildObj();			
	}//	initInstObjGeometry
	
	/**
	 * Get the type of the main mesh to be created
	 * @return a constant defining the type of PShape being created
	 */	
	protected abstract int getMainMeshType();

	/**
	 * builds flock specific instance of boid render rep, including colors, textures, etc.
	 */
	protected abstract void initInstObjGeometryIndiv();		
	
	/**
	 * initialize flock/team colors for an instance of this object
	 */
	protected abstract void initFlkColor();	
	
	/**
	 * build the instance of a particular object
	 */
	protected abstract void buildObj();
	
	/**
	 * Create and return a processing shape (PShape) with passed arg list. 
	 * TODO : replace with agnostic mesh someday.
	 * @param meshType PConstants-defined constant specifying the type of shape to create
	 * @param args a (possibly empty) list of arguments
	 * @return a Processing PShape with specified criteria
	 */	
	protected PShape createBaseShape(int meshType, float... args) {
		return createBaseShape(false, meshType, args);
	}
	/**
	 * Create and return a processing shape (PShape) with no args. 
	 * TODO : replace with agnostic mesh someday.
	 * @return a Processing PShape with specified criteria
	 */	
	protected PShape createBaseShape() {
		return createBaseShape(true, -1);
	}
	
	/**
	 * Create and return a processing shape (PShape) with passed arg list. 
	 * TODO : replace with agnostic mesh someday.
	 * @param isEmtpy If no meshType is provided
	 * @param meshType PConstants-defined constant specifying the type of shape to create
	 * @param args a (possibly empty) list of arguments
	 * @return a Processing PShape with specified criteria
	 */	
	private PShape createBaseShape(boolean isEmpty, int meshType, float... args) {
		if (isEmpty) {
			return ((my_procApplet) p).createShape();
		}
		if (args.length == 0) {
			return ((my_procApplet) p).createShape(meshType);
		}
		return ((my_procApplet) p).createShape(meshType, args);
	}
	
	/**
	 * create an individual shape at a particular location and 
	 * set up initial configuration - also perform any universal initial shape code
	 * @param initTransVec initial translation
	 * @return
	 */
	protected PShape makeShape(myPointf initTransVec){
		PShape sh = createBaseShape();
		sh.translate(initTransVec.x,initTransVec.y,initTransVec.z);		
		return sh;
	}//makeShape
	
	/**
	 * Build a myRndrObjClr object, with passed values for various color settings
	 * @param fill
	 * @param stroke
	 * @param emit
	 * @param amb
	 * @param spec
	 * @param divis
	 * @param stWt
	 * @param shn
	 * @return
	 */
	protected RenderObj_Clr makeColor(int[] fill, int[] stroke, int[] emit, int[] amb, int[] spec, float divis, float stWt, float shn){
		for(int j=0;j<3;++j){
			stroke[j] = (int) (fill[j]/divis);	
			emit[j] = (int)(emitMod * emit[j]);
		}
		stroke[3] = 255;			//stroke alpha
		RenderObj_Clr clr = new RenderObj_Clr(p);
		clr.setClrVal("fill", fill);
		clr.setClrVal("stroke", stroke);
		clr.setClrVal("spec", spec);
		clr.setClrVal("emit", emit);
		clr.setClrVal("amb", amb);
		clr.setClrVal("strokeWt", stWt);
		clr.setClrVal("shininess", shn);
		return clr;
	}
	
	/**
	 * build quad shape from object points
	 * @param transVec
	 * @param numX
	 * @param btPt
	 * @param objRndr
	 * @return
	 */
	protected int buildQuadShape(myPointf transVec, int numX, int btPt, myPointf[][] objRndr){
		PShape sh = makeShape(transVec);
		sh.beginShape(PConstants.QUAD);
			flockColor.shPaintColors(sh);
			for(int i = 0; i < numX; ++i){
				shgl_vertex(sh,objRndr[btPt][0]);shgl_vertex(sh,objRndr[btPt][1]);shgl_vertex(sh,objRndr[btPt][2]);shgl_vertex(sh,objRndr[btPt][3]);btPt++;
			}//for i				
		sh.endShape(PConstants.CLOSE);
		objRep.addChild(sh);		
		return btPt;
	}
	
	/**
	 * Create a PShape and set it's initial transformations.
	 * @param transVec First applied transform - initial translation
	 * @param scaleVec Scale applied after translate
	 * @param rotAra First applied rotation
	 * @param trans2Vec 2nd Applied translation
	 * @param rotAra2 2nd Applied rotation
	 * @param trans3Vec 3rd Applied translation
	 * @param rotAra3 3rd Applied rotation
	 * @return PShape created and transformed using passed transforms
	 */
	protected PShape createAndSetInitialTransform(myPointf transVec, myPointf scaleVec, float[] rotAra, myPointf trans2Vec, float[] rotAra2, myPointf trans3Vec, float[] rotAra3){	
		PShape sh = makeShape(transVec);			
		sh.scale(scaleVec.x,scaleVec.y,scaleVec.z);
		sh.rotate(rotAra[0],rotAra[1],rotAra[2],rotAra[3]);
		sh.translate(trans2Vec.x, trans2Vec.y, trans2Vec.z);
		sh.rotate(rotAra2[0],rotAra2[1],rotAra2[2],rotAra2[3]);
		sh.translate(trans3Vec.x, trans3Vec.y, trans3Vec.z);
		sh.rotate(rotAra3[0],rotAra3[1],rotAra3[2],rotAra3[3]);		
		return sh;
	}
	
	/**
	 * build a pole
	 * @param poleNum
	 * @param clr
	 * @param rad
	 * @param height
	 * @param drawBottom
	 * @param transVec
	 * @param scaleVec
	 * @param rotAra
	 * @param trans2Vec
	 * @param rotAra2
	 * @param trans3Vec
	 * @param rotAra3
	 * @return
	 */
	protected PShape buildPole(
			int poleNum, RenderObj_Clr clr, 
			float rad, float height, 
			boolean drawBottom, 
			myPointf transVec, myPointf scaleVec, float[] rotAra, 
			myPointf trans2Vec, float[] rotAra2, 
			myPointf trans3Vec, float[] rotAra3){
		float theta, rsThet, rcThet, rsThet2, rcThet2;
		int numTurns = 6;
		float twoPiOvNumTurns = MyMathUtils.TWO_PI_F/numTurns;
		PShape shRes = createBaseShape(PConstants.GROUP), sh;
		//pre-calc rad-theta-sin and rad-theta-cos
		float[] rsThetAra = new float[1 + numTurns];
		float[] rcThetAra = new float[1 + numTurns];
		for(int i = 0; i <numTurns; ++i){
			theta = i * twoPiOvNumTurns;
			rsThetAra[i] = (float) (rad*Math.sin(theta));
			rcThetAra[i] = (float) (rad*Math.cos(theta));
		}
		//wrap around value - theta == 0
		rsThetAra[numTurns] = 0.0f;
		rcThetAra[numTurns] = rad;
		
		for(int i = 0; i <numTurns; ++i){
			rsThet = rsThetAra[i];
			rcThet = rcThetAra[i];
			rsThet2 = rsThetAra[i+1];
			rcThet2 = rcThetAra[i+1];

			sh = createAndSetInitialTransform(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);
			sh.beginShape(PConstants.QUAD);				      
				clr.shPaintColors(sh);
				shgl_vertexf(sh,rsThet, 0, rcThet );
				shgl_vertexf(sh,rsThet, height,rcThet);
				shgl_vertexf(sh,rsThet2, height,rcThet2);
				shgl_vertexf(sh,rsThet2, 0, rcThet2);
			sh.endShape(PConstants.CLOSE);	
			shRes.addChild(sh);
			//caps
			sh = createAndSetInitialTransform(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);
			sh.beginShape(PConstants.TRIANGLE);				      
				clr.shPaintColors(sh);
				shgl_vertexf(sh,rsThet, height, rcThet );
				shgl_vertexf(sh,0, height, 0 );
				shgl_vertexf(sh,rsThet2, height, rcThet2 );
			sh.endShape(PConstants.CLOSE);
			shRes.addChild(sh);
			
			if(drawBottom){
				sh = createAndSetInitialTransform(transVec, scaleVec, rotAra, trans2Vec, rotAra2, trans3Vec, rotAra3);				
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
	}//buildPole	
	
	//set background for menu color, darkening a bit so that bright colors are still visible on white background
	public void setMenuColor(){
		flockColor.fillMenu(.9f);
	}
	/**
	 * instance a shape and draw it with given animation phase
	 * @param animPhase
	 * @param objID
	 */
	public final void drawMe(double animPhase, int objID){
		((my_procApplet) p).shape(objRep);
		drawMeIndiv((int)(animPhase * numAnimFrames));
	}
	/**
	 * draw object
	 * @param animIDX index in animation array to use
	 */
	protected abstract void drawMeIndiv(int animPhase);
	
	//public void shgl_vTextured(PShape sh, myPointf P, float u, float v) {sh.vertex(P.x,P.y,P.z,u,v);}                          // vertex with texture coordinates
	public void shgl_vertexf(PShape sh, float x, float y, float z){sh.vertex(x,y,z);}	 // vertex for shading or drawing
	public void shgl_vertex(PShape sh, myPointf P){sh.vertex(P.x,P.y,P.z);}	 // vertex for shading or drawing
	public void shgl_normal(PShape sh, myVectorf V){sh.normal(V.x,V.y,V.z);	} // changes normal for smooth shading

}//abstract class myRenderObj

