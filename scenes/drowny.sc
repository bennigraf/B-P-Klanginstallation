
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/_proto.sc".load;
/*var rain = ~proto.deepCopy;*/

// META
scene.vol.rain = 0.5;
scene.vol.drone = 0.05;
scene.vol.dings = 0.1;

scene.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
	self.dingBuf = Buffer.alloc(s, 15 * self.server.sampleRate);
	self.server.sync;
		
	// init synth-defs that are actually playing...
	self.sdefs = ();
	self.sdefs.rain = List();
	self.channels.do{ |n|
		8.do{ |o|
			self.sdefs.rain.add( Synth(\rain, [\out, n, \amp, 0]));
		}
	};
	self.server.sync();
	
	self.sdefs.drone = Synth(\drone, [\amp, 0]);

	// init controlling buses
	self.busses = ();
	self.busses.rain = ();
	self.busses.rain.intensity = Bus.control(self.server, 1).set(0);
	self.busses.rain.amp = Bus.control(self.server, 1).set(0);
	self.busses.droneAmp = Bus.control(self.server, 1).set(0);
	self.server.sync();
	
	// map busses to sdef-controls
	self.sdefs.rain.do{ |sdef|
		sdef.map(\amp, self.busses.rain.amp, \intensity, self.busses.rain.intensity);
	};
	self.sdefs.drone.map(\amp, self.busses.droneAmp);
	self.server.sync;
	self.bootedUp.unhang;
};
scene.haltSelf = { |self|
	while({self.sdefs.rain.size > 0}, {
		self.sdefs.rain.pop.free;
	});
	self.sdefs.drone.free;
	self.busses.rain.do {|bus|
		bus.free;
	};
	self.busses.droneAmp.free;
	self.dingBuf.free;
	if(self.sustainer.isKindOf(Synth)) {
		self.sustainer.free;
	};
	self = nil;
};


scene.run = { |self, runtime = nil, runtimemod = 1|
	
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
			Out.kr(self.busses.rain.intensity, lvl);
		}.play;
		
		self.dingsPattern = Pbind(
			\instrument, \ding,
			\freq, Pxrand([200,400,600,800,1000,1600,2000,],inf),
			\freq2, Pxrand([300,500,700,900,1100,1300,1900,],inf),
			\dur, Pwhite(8, 18),
			\amp, Prand([0.2,0.4,0.6,0.9], inf),
			\combBuf, self.dingBuf
		).play;
		
		(self.runtime * self.sustime * runtimemod).wait;
		
		self.dingsPattern.stop; self.dingsPattern = nil;
		
		self.sustainer.free;
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 120, runtimemod = 1|
	
	self.starter = Task({
		self.startRamp = ();
		self.startRamp.amp = {
			Out.kr(self.busses.rain.amp, Line.kr(0.5, 1, runtime/2 * runtimemod, doneAction: 2));
		}.play;
		self.startRamp.intensity = {
			Out.kr(self.busses.rain.intensity, Line.kr(0, 5, runtime * runtimemod, doneAction: 2));
		}.play;
		self.startRamp.droneAmp = {
			Out.kr(self.busses.droneAmp, Line.kr(0, 1, runtime * runtimemod, doneAction: 2));
		}.play
	}).play;
	(runtime * runtimemod);
	
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	
	self.ender = Task({
		self.endRamp = ();
		self.endRamp.droneAmp = {
			Out.kr(self.busses.droneAmp, Line.kr(1, 0, runtime, doneAction: 2));
		}.play;
		self.endRamp.intensity = {
			Out.kr(self.busses.rain.intensity, Line.kr(5, 0, runtime, doneAction: 2));
		}.play;
		(runtime/2).wait;		
		self.endRamp.amp = {
			Out.kr(self.busses.rain.amp, Line.kr(1, 0.2, (runtime/2), doneAction: 2));
		}.play;
		(runtime/2).wait;
		self.haltSelf();
	}).play;
};

scene.loadSdefs = { |self|
	SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 0.8, intensity = 1|
		var snd, trig, filtfreq, distmod;
		trig = Dust.ar(intensity);
		snd = PinkNoise.ar() * Decay.ar(trig, 0.4).lag(0.02);
		distmod = 0.8.rand + 0.2;
		filtfreq = distmod.linexp(0.2, 1, 520, 2390);
		snd = LPF.ar(snd, 4000);
		snd = snd * distmod;
		Out.ar(out, snd*amp * self.vol.rain);
	}).add;
	
	SynthDef(\drone, { |amp = 0| 
		var snd, env;
		snd = Klang.ar(`[ {exprand(40, 210)}!12 * LFNoise2.kr(0.04).range(0.8, 1.2), nil, nil],1,0);
		snd = RLPF.ar(snd, SinOsc.kr(0.02, add: 400, mul:250), 4.reciprocal);
		snd = snd * (1 - SinOsc.ar(LFNoise1.ar(0.02).range(2, 8), mul: LFNoise1.ar(0.05).range(0.01, 0.3), add: 0.5));
		snd = PanAz.ar(self.channels, snd, LFNoise2.ar(0.05), 1, 3);
		Out.ar(0, snd * amp * self.vol.drone);
	}).add;
	
	SynthDef(\ding, { |amp = 0, freq = 200, freq2 = 300, combBuf|
		var snd,snd2, combSnd, combTime, env, synth, synth2, out;
		synth = VarSaw.ar([freq, freq*3.6]).sum;
		synth2 = VarSaw.ar([freq2, freq2*1.6]).sum;
		env = EnvGen.ar(Env.perc(0.01, 8));
		snd = BPF.ar(synth*env, 1200, 10.reciprocal);
		snd2 = BPF.ar(synth2*env, 9000, 10.reciprocal);
		snd = snd + snd2;
		snd = GVerb.ar(snd+snd2, 160, 50, 0.8).sum;
		snd = Compander.ar(snd, snd, 0.5, 1, 1/6, 0.041, 20) * 2;
		combTime = Rand(0.5, 1.5);
		combSnd = BufCombL.ar(combBuf, snd, combTime, 10);
		snd = Compander.ar(snd, snd, 0.5, 1, 1/6, 0.041, 20) * 2;
		out = Rand(0,self.channels-1);
		Out.ar(out, combSnd * amp * self.vol.dings);
		Out.ar(out, snd * amp * self.vol.dings);
		DetectSilence.ar(snd, 0.1, 5, doneAction:2);
	}).add;
	
}

