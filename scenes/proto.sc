var proto = ();

// some "env-vars"...
proto.server = Server.default;
proto.channels = 8;		// good point to start

proto.runtime = 420;	// kind of arbitrary:
proto.starttime = 2/7;	// multipliers for runtime... Routine
proto.sustime = 3/7;
proto.endtime = 2/7;

proto.state = nil;
proto.bootedUp = Condition(false);

proto.init = { |self, channels = nil|
	Routine({
		if(self.server.serverRunning.not){
			"Booting Server first, give me a second!".postln;
			self.server.bootSync;
		};
	
		// set up synthdefs, busses
	//	Server.default.waitForBoot({}); // used bootsync above instead...
	
		if(channels.isNil.not) {
			self.channels = channels;
		};
	
		self.loadSdefs();
		self.server.sync;
		
		self.bootUp();
		self.bootedUp.wait;
		self.server.sync;
	
		self.state = "idle";
		
		// unhang thread which called this routine - don't know which way of the following I should prefer
		//either:
		self.bootedUp.test = true;
		self.bootedUp.signal
		// or:
/*		self.bootedUp.unhang;*/
	}).play;
};

proto.loadSdefs = nil;
proto.bootUp = nil;

proto.run = { |self, runtime, runtimemod = 1|
	(runtime * runtimemod);
};

proto.end = {
	
};

proto.getStartingTime = { |self, runtimemod = 1|
	(proto.runtime * proto.starttime * runtimemod);
};
proto.getSusTime = { |self, runtimemod = 1|
	(proto.runtime * proto.sustime * runtimemod);
};
proto.getEndingTime = { |self, runtimemod = 1|
	var endingtime = (proto.runtime * proto.endtime * runtimemod);
	endingtime;
};