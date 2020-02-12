package Boids2_PKG;

import java.io.File;

import base_UI_Objects.my_procApplet;
import base_UI_Objects.windowUI.base.myDispWindow;
import base_UI_Objects.windowUI.sidebar.mySideBarMenu;
import base_Math_Objects.MyMathUtils;
import processing.core.PShape;
import processing.core.PImage;
/**
 * Flocking boids sim version 2.1
 * @author john turner
 */

public class Boids_21_Main extends my_procApplet {
	//project-specific variables
	public String prjNmLong = "Boids Version 2.0", prjNmShrt = "Boids2";
	
	//platform independent path separator
	public String dirSep = File.separator;
	//don't use sphere background for this program
	private boolean useSphereBKGnd = true;	
	
	private final int
		showUIMenu = 0,
		show3DWin = 1,
		show2DWin = 2;
	public final int numVisFlags = 3;
	
	//idx's in dispWinFrames for each window - 0 is always left side menu window
	private static final int disp3DResIDX = 1,
							disp2DResIDX = 2;	

	//private boolean cyclModCmp;										//comparison every draw of cycleModDraw			
	public final int[] bground = new int[]{244,244,255,255};		//bground color
	private PShape bgrndSphere;										//giant sphere encapsulating entire scene

	
	
	//////////////////////////////////////////////// code
	public static void main(String[] passedArgs) {
	    String[] appletArgs = new String[] { "Boids2_PKG.Boids_21_Main" };
	    my_procApplet._invokedMain(appletArgs, passedArgs);
	 }
	
	@Override
	protected final void setSmoothing() {		
		noSmooth();
	}
	/**
	 * whether or not we want to restrict window size on widescreen monitors
	 * 
	 * @return 0 - use monitor size regardless
	 * 			1 - use smaller dim to be determine window 
	 * 			2+ - TBD
	 */
	@Override
	protected int setAppWindowDimRestrictions() {	return 1;}	
	
	@Override
	protected void setBkgrnd(){
		//TODO move to myDispWindow	
		if(useSphereBKGnd) {shape(bgrndSphere);		} else {background(bground[0],bground[1],bground[2],bground[3]);		}
	}


	@Override
	protected void setup_indiv() {
		//modify default grid dims to be 1500x1500x1500
		setDesired3DGridDims(1500);
		//TODO move to window to set up specific background for each different "scene" type
		//PImage bgrndTex = loadImage("bkgrndTex.jpg");
		//PImage bgrndTex = loadImage("sky_1.jpg");
		if(useSphereBKGnd) {			setBkgndSphere();	} else {		setBkgrnd();	}

	}
	
	private void setBkgndSphere() {
		sphereDetail(100);
		//TODO move to window to set up specific background for each different "scene" type
		PImage bgrndTex = loadImage("bkgrndTex.jpg");
		bgrndSphere = createShape(SPHERE, 10000);
		bgrndSphere.setTexture(bgrndTex);
		bgrndSphere.rotate(MyMathUtils.halfPi_f,-1,0,0);
		bgrndSphere.setStroke(false);	
		//TODO move to myDispWindow
		background(bground[0],bground[1],bground[2],bground[3]);		
		shape(bgrndSphere);	
		sphereDetail(10);
	}


	@Override
	protected void initMainFlags_Priv() {
		setMainFlagToShow_debugMode(false);
		setMainFlagToShow_saveAnim(true); 
		setMainFlagToShow_runSim(true);
		setMainFlagToShow_singleStep(true);
		setMainFlagToShow_showRtSideMenu(true);		
	}

	@Override
	protected void initVisOnce_Priv() {
		showInfo = true;
		drawnTrajEditWidth = 10;
		int numWins = numVisFlags;//includes 1 for menu window (never < 1)
		String[] _winTitles = new String[]{"","Boids ver2.0 3D","Boids ver2.0 2D"},
				_winDescr = new String[] {"", "Multi Flock Predator/Prey Boids 3D Simulation","Multi Flock Predator/Prey Boids 2D Simulation"};
		initWins(numWins,_winTitles, _winDescr);
		//call for menu window
		buildInitMenuWin(showUIMenu);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  =  new float[]{menuWidth, 0, width-menuWidth, height}, _dimClosed  =  new float[]{menuWidth, 0, hideWinWidth, height};	
		System.out.println("Width : " + width + " | Height : " + height);
		int wIdx = dispMenuIDX,fIdx=showUIMenu;
		dispWinFrames[wIdx] = this.buildSideBarMenu(wIdx, fIdx, new String[]{"Functions 1","Functions 2","Functions 3","Functions 4"}, new int[] {3,4,4,4}, 5, true, false);//new mySideBarMenu(this, winTitles[wIdx], fIdx, winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);			
		
		//define windows
		//idx 0 is menu, and is ignored	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//String _ttl, String _desc 			: window title and description
		//boolean[] _dispFlags 					: 
		//   flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)
		//			//display window initialization	
		wIdx = disp3DResIDX; fIdx = show3DWin;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{255,255,255,255},new int[]{0,0,0,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new myBoids3DWin(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);
		wIdx = disp2DResIDX; fIdx = show2DWin;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,true,false}, new int[]{50,40,20,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255});
		dispWinFrames[wIdx] = new myBoids3DWin(this, winTitles[wIdx], fIdx,winFillClrs[wIdx], winStrkClrs[wIdx], winRectDimOpen[wIdx], winRectDimClose[wIdx], winDescr[wIdx]);

		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{show3DWin,show2DWin}, new int[]{disp3DResIDX, disp2DResIDX});
	
	}//initVisOnce_Priv

	@Override
	protected void initOnce_Priv() {
		setVisFlag(showUIMenu, true);					//show input UI menu	
		setVisFlag(show3DWin, true);
	}

	@Override
	protected void initVisProg_Indiv() {}

	@Override
	protected void initProgram_Indiv() {	}

	@Override
	public String[] getMouseOverSelBtnNames() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	protected String getPrjNmLong() {	return prjNmLong;}
	@Override
	protected String getPrjNmShrt() {	return prjNmShrt;}
	
	@Override
	protected void handleKeyPress(char key, int keyCode) {
		switch (key){
			case '1' : {break;}
			case '2' : {break;}
			case '3' : {break;}
			case '4' : {break;}
			case '5' : {break;}							
			case '6' : {break;}
			case '7' : {break;}
			case '8' : {break;}
			case '9' : {break;}
			case '0' : { break;}							
			case ' ' : {toggleSimIsRunning(); break;}							//run sim
			case 'f' : {dispWinFrames[curFocusWin].setInitCamView();break;}//reset camera
			case 'a' :
			case 'A' : {toggleSaveAnim();break;}						//start/stop saving every frame for making into animation
			case 's' :
			case 'S' : {saveSS(prjNmShrt);break;}//save picture of current image			
//			case ';' :
//			case ':' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(-1); break;}//decrease the number of cycles between each draw, to some lower bound
//			case '\'' :
//			case '"' : {((mySideBarMenu)dispWinFrames[dispMenuIDX]).guiObjs[((mySideBarMenu)dispWinFrames[dispMenuIDX]).gIDX_cycModDraw].modVal(1); break;}//increase the number of cycles between each draw to some upper bound		
			default : {	}
		}//switch	
	}
	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////


	@Override
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){return ((altIsPressed() ? .1 : 1.0) * (shiftIsPressed() ? 10.0 : 1.0));}	

	@Override
	public boolean isClickModUIVal() {
		//TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or conventient
		return altIsPressed() || shiftIsPressed();		
	}

	@Override
	public float[] getUIRectVals(int idx) {
			//this.pr("In getUIRectVals for idx : " + idx);
		switch(idx){
			case dispMenuIDX 		: {return new float[0];}			//idx 0 is parent menu sidebar
			case disp3DResIDX 		: {return dispWinFrames[dispMenuIDX].uiClkCoords;}
			case disp2DResIDX 		: {	return dispWinFrames[dispMenuIDX].uiClkCoords;}
			default :  return dispWinFrames[dispMenuIDX].uiClkCoords;
		}
	}

	@Override
	public void handleShowWin(int btn, int val, boolean callFlags) {
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			setMenuBtnState(mySideBarMenu.btnShowWinIdx,btn, val);
		} else {//called from clicking on buttons in UI
			//val is btn state before transition 
			boolean bVal = (val == 1?  false : true);
			//each entry in this array should correspond to a clickable window
			setVisFlag(winFlagsXOR[btn], bVal);
		}
	}
	
	/**
	 * return the number of visible window flags for this application
	 * @return
	 */
	@Override
	public int getNumVisFlags() {return numVisFlags;}
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	protected void setVisFlag_Indiv(int idx, boolean val ){
		switch (idx){
			case showUIMenu 	    : { dispWinFrames[dispMenuIDX].setFlags(myDispWindow.showIDX,val);    break;}											//whether or not to show the main ui window (sidebar)			
			case show3DWin		: {setWinFlagsXOR(disp3DResIDX, val); break;}
			case show2DWin		: {setWinFlagsXOR(disp2DResIDX, val); break;}
			default : {break;}
		}
	}//setFlags  

	/**
	 * any instancing-class-specific colors - colorVal set to be higher than IRenderInterface.gui_OffWhite
	 * @param colorVal
	 * @param alpha
	 * @return
	 */
	@Override
	protected int[] getClr_Custom(int colorVal, int alpha) {
		switch(colorVal) {
	    	case gui_boatBody1 	  	: {return new int[] {80, 40, 25,alpha};}
	    	case gui_boatBody2 	  	: {return new int[] {0, 0, 0,alpha};}
	    	case gui_boatBody3 	  	: {return new int[] {40, 0, 0,alpha};}
	    	case gui_boatBody4 	  	: {return new int[] {0, 80, 0,alpha};}
	    	case gui_boatBody5 	  	: {return new int[] {40, 0, 80,alpha};}
	    	case gui_boatStrut 	  	: {return new int[] {80, 40, 25,alpha};}
			default 				: {return new int[] {255,255,255,alpha};}
		}
	}

	/**
	 * custom boid colors
	 */
	public static final int gui_boatBody1 = gui_nextColorIDX + 0;
	public static final int gui_boatBody2 = gui_nextColorIDX + 1;
	public static final int gui_boatBody3 = gui_nextColorIDX + 2;	
	public static final int gui_boatBody4 = gui_nextColorIDX + 3;	
	public static final int gui_boatBody5 = gui_nextColorIDX + 4;	
	public static final int gui_boatStrut = gui_nextColorIDX + 5;

}
