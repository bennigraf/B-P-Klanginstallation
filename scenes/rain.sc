
var rain = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/proto.sc".load;
/*var rain = ~proto.deepCopy;*/

rain.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
/*	Routine({*/
		// init synth-defs that are actually playing...
		self.sdefs = List();
		self.channels.do{ |n|
			8.do{ |o|
				self.sdefs.add( Synth(\rain, [\out, n, \amp, 0]).play );
			}
		};
		self.server.sync();
	
		// init controlling buses
		self.busses = ();
		self.busses.intensity = Bus.control(self.server, 1);
		self.busses.amp = Bus.control(self.server, 1);
		self.server.sync();
	
		// map busses to sdef-controls
		self.sdefs.do{ |sdef|
			sdef.map(\amp, self.busses.amp, \intensity, self.busses.intensity);
		};
		self.server.sync;
		self.bootedUp.unhang;
/*	}).play;*/
};
rain.haltSelf = { |self|
	while({self.sdefs.size > 0}, {
		self.sdefs.pop.free;
	});
	self.busses.do {|bus|
		bus.free;
	};
	if(self.sustainer.isKindOf(Synth)) {
		self.sustainer.free;
	};
	self = nil;
};


rain.run = { |self, runtime = nil, runtimemod = 1|
	
	if(runtime.isNil.not) {
		self.runtime = runtime;
	};
	
	/* 
		1. fade in rain - loudness 0.5 to 1 in 60 seconds, intensity 0 to 5 in 120 seconds
		2. sustain-mode - intensity lfnoised for runtime...
		3. fade out - intensity in 120 seconds, loudness in 60 seconds to 0.5
			(while that, allow next scene to start)
	*/
	
	self.runner = Task({
		self.state = "running";

		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;
		
		self.sustainer = {
			var lvl = LFNoise1.kr(1/2).range(4, 6) + LFNoise1.kr(1/20);
			Out.kr(self.busses.intensity, lvl);
		}.play;
		(self.runtime * self.sustime * runtimemod).wait;
		
		self.sustainer.free;
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

rain.start = {|self, runtime = 120, runtimemod = 1|
	
	self.starter = Task({
		self.startRamp = ();
		self.startRamp.amp = {
			Out.kr(self.busses.amp, Line.kr(0.5, 1, runtime/2 * runtimemod, doneAction: 2));
		}.play;
		self.startRamp.intensity = {
			Out.kr(self.busses.intensity, Line.kr(0, 5, runtime * runtimemod, doneAction: 2));
		}.play;
	}).play;
	(runtime * runtimemod);
	
};

rain.end = { |self, runtime = 0, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	self.ender = Task({
		self.endRamp = ();
		self.endRamp.intensity = {
			Out.kr(self.busses.intensity, Line.kr(5, 0, runtime, doneAction: 2));
		}.play;
		(runtime/2).wait;		
		self.endRamp.amp = {
			Out.kr(self.busses.amp, Line.kr(1, 0.2, runtime/2, doneAction: 2));
		}.play;
		(runtime/2).wait;
		self.haltSelf();
	}).play;
};

rain.loadSdefs = {
	SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 0.8, intensity = 1|
		var snd, trig, filtfreq, distmod;
		trig = Dust.ar(intensity);
		snd = PinkNoise.ar() * Decay.ar(trig, 0.4).lag(0.02);
		distmod = LFNoise1.kr(1).range(0.2, 1);
		filtfreq = distmod.linexp(0.2, 1, 520, 2390);
		snd = BPF.ar(snd, [filtfreq/2, filtfreq, filtfreq*2], [0.2, 2.5, 0.3], mul: [0.5, 1, 0.3]).sum;
		snd = snd * distmod;
	//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
		Out.ar(out, snd*amp);
	}).add;
}

