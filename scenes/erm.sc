
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/proto.sc".load;
/*var rain = ~proto.deepCopy;*/

// META
scene.vol.erm = 1;

scene.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
	// unusual timings...:
	self.runtime = 600;	// kind of arbitrary:
	self.starttime = 2/10;	// multipliers for runtime... Routine
	self.sustime = 6/10;
	self.endtime = 2/10;
	
	// load buffers
	self.buffers = ();
	self.buffers.erms = List();
	PathName(~basepath++"data/erm/erms/").files.do { |file|
		self.buffers.erms.add(Buffer.read(s, file.fullPath));
		self.server.sync;
	};
	
	
	// init synth-defs that are actually playing...
	self.pfiff = Synth(\pfiff, [\amp, 0]);

	// init controlling buses
	self.busses = ();
	self.busses.amp = Bus.control(self.server, 1);
	self.busses.pfiffamp = Bus.control(self.server, 1);
	
	self.server.sync();

	// map busses to sdef-controls
	self.pfiff.map(\amp, self.busses.pfiffamp);
	
	self.server.sync;
	self.bootedUp.unhang;
};
scene.haltSelf = { |self|
	while({self.buffers.erms.size > 0}, {
		self.buffers.erms.pop.free;
	});
	self.pfiff.free;
	self.busses.do {|bus|
		bus.free;
	};
	self.sustainer.ctrlr.stop.free;
	self.sustainer.task.stop.free;
	self = nil;
};


scene.run = { |self, runtime = nil, runtimemod = 1|
	
	if(runtime.isNil.not) {
		self.runtime = runtime;
	};
	
	/* 
		1. (fade in background-noise-thingy?? for 120 sec oder so)
		2. sustain-mode - erms beginnen, langsam von tief/lang/wenig zu hoch/schnell/viel und zurück, bis 0
		3. fade out - like fade in
	*/
	
	self.runner = Task({
		self.state = "running";

		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;
		
		self.sustainer = ();
		self.sustainer.running = true;
		self.sustainer.wait = 5;
		self.sustainer.mod = 0;
		self.sustainer.ctrlr = Task({
			var sustime = self.runtime * self.sustime * runtimemod;
			3141.do { |step|
				var osc = (step/1000).sin; // sin-wave from 0 to 1 to 0 (phase 0..pi) during sustime
				var waitmin, waitmax;
				var range;
				
				waitmin = osc.linlin(0, 1, 2, 0.01);
				waitmax = osc.linlin(0, 1, 8, 0.5);
				self.sustainer.wait = 1.0.rand.linlin(0, 1, waitmin, waitmax);
				
				self.sustainer.mod = osc;
				
				(sustime/3141).wait;
			}
		}).play;
		self.sustainer.task = Task({
			{self.sustainer.running}.while{
				Synth(\erm, [\buf, self.buffers.erms.choose, \mod, self.sustainer.mod]);
				self.sustainer.wait.wait;
			}
		}).play;
		(self.runtime * self.sustime * runtimemod).wait;
		self.sustainer.running = false;
		
		
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 120, runtimemod = 1|
	
	self.starter = Task({
		self.startRamp = {
			Out.kr(self.busses.pfiffamp, Line.kr(0, 1, runtime/2 * runtimemod, doneAction: 2));
		}.play;
	}).play;
	(runtime * runtimemod);
	
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	self.ender = Task({
		self.endRamp = {
			Out.kr(self.busses.pfiffamp, Line.kr(1, 0, runtime, doneAction: 2));
		}.play;
		runtime.wait;
		self.haltSelf(); 
	}).play;
};

scene.loadSdefs = { |self|
	SynthDef(\erm, { |buf, mod = 1|
	/*	BufRd*/
		var dur = BufDur.ir(buf) * Rand(0.5, 1.5) * mod.linlin(0, 1, 8, 0.5);
	/*	var dur = 1;*/
		var phase = EnvGen.ar(Env([0, BufFrames.ir(buf)], [dur], Rand(-2, 2)/mod.linlin(0, 1, 1, 3)));
		var snd = BufRd.ar(1, buf, phase, 0, 4);
		var att = mod.linlin(0, 1, 8, 0.1);
		snd = snd * EnvGen.kr(Env([0, 1, 1, 0], [att, dur - att, 0.1]));
		snd = snd + FreeVerb.ar(snd, 1, mod.linlin(0, 1, 2, 0.5), mod.linlin(0, 1, 0.8, 0.2), mul: 0.8);
		DetectSilence.ar(snd, doneAction: 2);
		Out.ar(Rand(0, self.channels-1).round, snd);
	}).add;
	
	SynthDef(\pfiff, { |buf, amp = 0|
		var c = self.channels;
		var pointer = {Ramp.ar(Latch.ar(WhiteNoise.ar(0.5, 0.5), Dust.kr(1/5)), 5)}!c;
		var snd = Warp1.ar(1, ~buffer, pointer, 1, 0.1, -1, 8, 0.3, 2);
		var modenv = LFSaw.ar(1/30, 1, 0.5, 0.5).lag(0.01);
		var ringfreqs = {LFNoise0.kr(1/30).range(900, 1900)}!c;
		var freqmods = { LFNoise0.kr(1/30, mul: modenv**4).range(0.7, 1.3) + LFNoise1.kr(1, mul:modenv).range(0.95, 1.05)}!c;
		snd = Ringz.ar(snd, ringfreqs * freqmods, 0.5, mul: {Decay.ar(Dust.ar(20), 0.5)}!c, add: snd * 80)/10;
		Out.ar(0, snd * amp);
	}).play();
}

