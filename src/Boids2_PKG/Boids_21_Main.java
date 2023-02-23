package Boids2_PKG;

import java.util.HashMap;

import Boids2_PKG.ui.Boids_2DWin;
import Boids2_PKG.ui.Boids_3DWin;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import base_UI_Objects.windowUI.sidebar.SidebarMenu;
import base_Utils_Objects.io.messaging.MsgCodes;

/**
 * Flocking boids sim version 2.1
 * @author john turner
 */
public class Boids_21_Main extends GUI_AppManager {
	//project-specific variables
	public final String prjNmLong = "Boids Version 2.0";
	public final String prjNmShrt = "Boids2";
	public final String projDesc = "Multiple boid flock predator/prey simulation";
	//use sphere background for this program
	private boolean useSphereBKGnd = true;
	
	private final String bkSkyBox = "bkgrndTex.jpg";
	/**
	 * size of 3d grid cube side
	 */
	private final int GridDim_3D = 1500;
		
	/**
	 * idx's in dispWinFrames for each window - 0 is always left side menu window
	 * Side menu is dispMenuIDX == 0
	 */
	private static final int disp3DResIDX = 1,
							disp2DResIDX = 2;	
	/**
	 * # of visible windows including side menu (always at least 1 for side menu)
	 */
	private static final int numVisWins = 3;

	public final int[] bground = new int[]{244,244,255,255};		//bground color
	
	//////////////////////////////////////////////// code
	public static void main(String[] passedArgs) {
	    Boids_21_Main me = new Boids_21_Main();
	    Boids_21_Main.invokeProcessingMain(me, passedArgs);
	 }
	
	protected Boids_21_Main(){super();}
	
	/**
	 * Set various relevant runtime arguments in argsMap
	 * @param _passedArgs command-line arguments
	 */
	@Override
	protected HashMap<String,Object> setRuntimeArgsVals(HashMap<String, Object> _passedArgsMap) {
		return  _passedArgsMap;
	}
	/**
	 * Called in pre-draw initial setup, before first init
	 * potentially override setup variables on per-project basis.
	 * Do not use for setting background color or Skybox anymore.
	 *  	(Current settings in my_procApplet) 	
	 *  	strokeCap(PROJECT);
	 *  	textSize(txtSz);
	 *  	textureMode(NORMAL);			
	 *  	rectMode(CORNER);	
	 *  	sphereDetail(4);	 * 
	 */
	@Override
	protected void setupAppDims_Indiv() {
		//modify default grid dims to be 1500x1500x1500
		setDesired3DGridDims(GridDim_3D);		
	}
	@Override
	protected boolean getUseSkyboxBKGnd(int winIdx) {	return useSphereBKGnd;}
	@Override
	protected String getSkyboxFilename(int winIdx) {	return bkSkyBox;}
	@Override
	protected int[] getBackgroundColor(int winIdx) {return bground;}
	@Override
	protected int getNumDispWindows() {	return numVisWins;	}

	@Override
	protected void setSmoothing() {		ri.setSmoothing(0);		}
	
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
	public String getPrjNmShrt() {return prjNmShrt;}
	@Override
	public String getPrjNmLong() {return prjNmLong;}
	@Override
	public String getPrjDescr() {return projDesc;}

	/**
	 * Set minimum level of message object console messages to display for this application. If null then all messages displayed
	 * @return
	 */
	@Override
	protected final MsgCodes getMinConsoleMsgCodes() {return null;}
	/**
	 * Set minimum level of message object log messages to save to log for this application. If null then all messages saved to log.
	 * @return
	 */
	@Override
	protected final MsgCodes getMinLogMsgCodes() {return null;}

	@Override
	protected void initBaseFlags_Indiv() {
		setBaseFlagToShow_debugMode(false);
		setBaseFlagToShow_saveAnim(true); 
		setBaseFlagToShow_runSim(true);
		setBaseFlagToShow_singleStep(true);
		setBaseFlagToShow_showRtSideMenu(true);		
	}

	@Override
	protected void initAllDispWindows() {
		showInfo = true;
		String[] _winTitles = new String[]{"","Boids ver2.0 3D","Boids ver2.0 2D"},
				_winDescr = new String[] {"", "Multi Flock Predator/Prey Boids 3D Simulation","Multi Flock Predator/Prey Boids 2D Simulation"};
		setWinTitlesAndDescs(_winTitles, _winDescr);
		//instanced window dimensions when open and closed - only showing 1 open at a time
		float[] _dimOpen  = getDefaultWinDimOpen(), 
				_dimClosed  = getDefaultWinDimClosed();	

		//Builds sidebar menu button config
		//application-wide menu button bar titles and button names
		String[] menuBtnTitles = new String[]{"Functions 1","Functions 2","Functions 3"};
		String[][] menuBtnNames = new String[][] { // each must have literals for every button defined in side bar menu, or ignored
			{"Func 00", "Func 01", "Func 02"},				//row 1
			{"Func 10", "Func 11", "Func 12", "Func 13"},	//row 2
			{"Func 10", "Func 11", "Func 12", "Func 13"}	//row 3
			};
		String [] dbgBtns = {"Debug 0", "Debug 1", "Debug 2", "Debug 3","Debug 4"};
		buildSideBarMenu(menuBtnTitles, menuBtnNames, dbgBtns, true, false);
		
		//define windows
		//idx 0 is menu, and is ignored	
		//setInitDispWinVals : use this to define the values of a display window
		//int _winIDX, 
		//float[] _dimOpen, float[] _dimClosed  : dimensions opened or closed
		//boolean[] _dispFlags 					: 
		//   flags controlling display of window :  idxs : 0 : canDrawInWin; 1 : canShow3dbox; 2 : canMoveView; 3 : dispWinIs3d
		//int[] _fill, int[] _strk, 			: window fill and stroke colors
		//int _trajFill, int _trajStrk)			: trajectory fill and stroke colors, if these objects can be drawn in window (used as alt color otherwise)
		//			//display window initialization	
		int wIdx = disp3DResIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,true,true,true}, new int[]{255,255,255,255},new int[]{0,0,0,255},new int[]{180,180,180,255},new int[]{100,100,100,255}); 
		dispWinFrames[wIdx] = new Boids_3DWin(ri, this, wIdx);
		wIdx = disp2DResIDX;
		setInitDispWinVals(wIdx, _dimOpen, _dimClosed,new boolean[]{false,false,true,false}, new int[]{50,40,20,255}, new int[]{255,255,255,255},new int[]{180,180,180,255},new int[]{100,100,100,255});
		dispWinFrames[wIdx] = new Boids_2DWin(ri, this, wIdx);

		//specify windows that cannot be shown simultaneously here
		initXORWins(new int[]{disp3DResIDX, disp2DResIDX}, new int[]{disp3DResIDX, disp2DResIDX});
	
	}//initAllDispWindows

	@Override
	protected void initOnce_Indiv() {
		setVisFlag(disp3DResIDX, true);
	}

	@Override
	protected void initProgram_Indiv() {	}

	@Override
	public String[] getMouseOverSelBtnLabels() {
		return new String[0];
	}
	
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
			case 'S' : {break;}//{saveSS(prjNmShrt);break;}//save picture of current image			
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

	/**
	 * Individual extending Application Manager post-drawMe functions
	 * @param modAmtMillis
	 * @param is3DDraw
	 */
	@Override
	protected void drawMePost_Indiv(float modAmtMillis, boolean is3DDraw) {}
	

	@Override
	//gives multiplier based on whether shift, alt or cntl (or any combo) is pressed
	public double clickValModMult(){return ((altIsPressed() ? .1 : 1.0) * (shiftIsPressed() ? 10.0 : 1.0));}	

	@Override
	public boolean isClickModUIVal() {
		//TODO change this to manage other key settings for situations where multiple simultaneous key presses are not optimal or convenient
		return altIsPressed() || shiftIsPressed();		
	}

	@Override
	public float[] getUIRectVals_Indiv(int idx, float[] menuRectVals) {
			//this.pr("In getUIRectVals for idx : " + idx);
		switch(idx){
			case disp3DResIDX 		: {return menuRectVals;}
			case disp2DResIDX 		: {return menuRectVals;}
			default :  return menuRectVals;
		}
	}

	@Override
	public void handleShowWin(int btn, int val, boolean callFlags) {
		if(!callFlags){//called from setflags - only sets button state in UI to avoid infinite loop
			setMenuBtnState(SidebarMenu.btnShowWinIdx,btn, val);
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
	public int getNumVisFlags() {return numVisWins;}
	@Override
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	protected void setVisFlag_Indiv(int idx, boolean val ){
		switch (idx){
			case disp3DResIDX		: {setWinFlagsXOR(disp3DResIDX, val); break;}
			case disp2DResIDX		: {setWinFlagsXOR(disp2DResIDX, val); break;}
			default : {break;}
		}
	}//setFlags  
	/**
	 * custom boid colors
	 */
	public static final int gui_boatBody1 = IRenderInterface.gui_nextColorIDX + 0;
	public static final int gui_boatBody2 = IRenderInterface.gui_nextColorIDX + 1;
	public static final int gui_boatBody3 = IRenderInterface.gui_nextColorIDX + 2;	
	public static final int gui_boatBody4 = IRenderInterface.gui_nextColorIDX + 3;	
	public static final int gui_boatBody5 = IRenderInterface.gui_nextColorIDX + 4;	
	public static final int gui_boatStrut = IRenderInterface.gui_nextColorIDX + 5;

	/**
	 * any instancing-class-specific colors - colorVal set to be higher than IRenderInterface.gui_OffWhite
	 * @param colorVal
	 * @param alpha
	 * @return
	 */
	@Override
	public int[] getClr_Custom(int colorVal, int alpha) {
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


}//class Boids_21_Main extends GUI_AppManager
