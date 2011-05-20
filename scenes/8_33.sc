
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/_proto.sc".load;

// META
scene.vol.silence = 1;
scene.vol.dels = 0.5;

scene.bootUp = { |self|
	
	self.runtime = 513;	// kind of arbitrary:
	self.starttime = 0;	// multipliers for runtime... Routine
	self.sustime = 1;
	self.endtime = 0;
	
	self.buffers = List();
	self.channels.do{ |n|
		self.buffers.add(Buffer.alloc(self.server, 2 * self.server.sampleRate));
	};
	self.server.sync();
	
	// set up busses, helper-sdefs, ... here
	self.sdefs = List();
	self.channels.do{ |n|
		self.sdefs.add(Synth(\delay, [\in, 0, \out, n, \delay, 1 + (0.1 * n), \buf, self.buffers[n], \amp, 0]));
	};
	self.server.sync();

	// init controlling buses
	self.busses = ();
	self.busses.amp = Bus.control(self.server, 1);
	self.server.sync();

	// map busses to sdef-controls
	self.sdefs.do{ |sdef|
		sdef.map(\amp,  self.busses.amp);
	};
	self.server.sync;
	
	self.bootedUp.unhang;
};
scene.haltSelf = { |self|
	self.sdefs.do{|sdef|
		sdef.free;
	};
	self.buffers.do{|buffer|
		buffer.free;
	};
	self.busses.amp.free;
	self = nil;
};


scene.run = { |self, runtime = nil, runtimemod = 1|
	
	if(runtime.isNil.not) {
		self.runtime = runtime;
	};
	
	/* 
		1. silence.
	*/
	
	self.runner = Task({
		var runtime;
		
		self.state = "running";

		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;

		runtime = self.runtime * self.sustime * runtimemod;
		self.sustainer = {
			var lvl = Amplitude.kr(SoundIn.ar(0));
			var amp = 1 - LagUD.kr(lvl, 0.001, 30);
			var env = EnvGen.kr(Env([0, 1, 1, 0], [runtime/5, runtime * 3/4, runtime/5]), doneAction: 2);
			Out.kr(self.busses.amp, amp * env);
		}.play;
		
		(self.runtime * self.sustime * runtimemod).wait;
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 120, runtimemod = 1|
	
	(runtime * runtimemod);
	
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	self.ender = Task({
		runtime.wait;
		self.haltSelf();
	}).play;
};

scene.loadSdefs = { |self|
	(
	SynthDef(\delay, { |in = 0, out = 0, delay = 1, buf, amp = 0|
		var snd = BufDelayN.ar(buf, SoundIn.ar(in), delay);
/*		amp.poll;*/
		Out.ar(out, snd * amp * self.vol.dels);
	}).add;
	)
}

