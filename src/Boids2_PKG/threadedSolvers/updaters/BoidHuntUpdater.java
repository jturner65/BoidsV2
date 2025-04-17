package Boids2_PKG.threadedSolvers.updaters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import Boids2_PKG.flocks.boids.Boid;

public class BoidHuntUpdater implements Callable<Boolean> {
	private List<Boid> bAra;
	public BoidHuntUpdater(List<Boid> _bAra) {
		bAra = _bAra;
	}
	//check kill chance, remove boid if succeeds
	private void hunt(Boid b){
		float chance;
		for(Boid dinner : b.preyFlk.values()){
			chance = ThreadLocalRandom.current().nextFloat();
			//kill him next update by setting dead flag
			if(chance < b.flk.flv.killPct){b.eat(dinner.mass);dinner.killMe("Eaten by predator : "+b.ID);return;}
		}
	}//kill
	
	public void run(){for(Boid b : bAra){	if(!b.isDead()){		hunt(b);	}		}}
	
	@Override
	public Boolean call() throws Exception {
		run();
		return true;
	}

}
