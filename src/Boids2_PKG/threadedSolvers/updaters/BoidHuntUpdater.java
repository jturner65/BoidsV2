package Boids2_PKG.threadedSolvers.updaters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.boids.myBoid;

public class BoidHuntUpdater implements Callable<Boolean> {
	private List<myBoid> bAra;
	public BoidHuntUpdater(List<myBoid> _bAra) {
		bAra = _bAra;
	}
	//check kill chance, remove boid if succeeds
	private void hunt(myBoid b){
		float chance;
		for(myBoid dinner : b.preyFlk.values()){
			chance = ThreadLocalRandom.current().nextFloat();
			//kill him next update by setting dead flag
			if(chance < b.flk.flv.killPct){b.eat(dinner.mass);dinner.killMe("Eaten by predator : "+b.ID);return;}
		}
	}//kill
	
	public void run(){for(myBoid b : bAra){	if(!b.isDead()){		hunt(b);	}		}}
	
	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}

}
