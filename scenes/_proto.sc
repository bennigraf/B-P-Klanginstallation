var proto = ();

// some "env-vars"...
proto.server = Server.default;
proto.channels = 8;		// good point to start

proto.runtime = 420;	// kind of arbitrary:
proto.starttime = 2/7;	// multipliers for runtime... Routine
proto.sustime = 3/7;
proto.endtime = 2/7;

/* TODO: Implement some kind of global volume controlling later...
proto.vol = ();
proto.vol.global = 1;
proto.vol.voices = ();
proto.vol.set = { |self|
	self.vol.voices.keyValuesDo { |voice, vol|
		
	};
};
proto.vol.addVoice{ |self, name, vol| 
	self.vol.voices.put(name.asSymbol, vol);
};
*/
// until then, use this:
proto.vol = ();
proto.vol.global = 1;
//proto.vol.voice1 = 0.3;proto.vol.voice2 = 0.8; etc.pp.

proto.state = nil;
proto.bootedUp = Condition(false);

proto.init = { |self, channels = nil|
	Routine({
		if(self.server.serverRunning.not){
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