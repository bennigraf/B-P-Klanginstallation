
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/proto.sc".load;
/*var rain = ~proto.deepCopy;*/

// META
scene.vol.tock = 0.7;
scene.vol.drops = 1;
scene.vol.stream = 1;
scene.vol.insect = 1;


scene.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
	// unusual timings...:
	self.runtime = 600;	// kind of arbitrary:
	self.starttime = 1/10;	// multipliers for runtime... Routine
	self.sustime = 6/10;
	self.endtime = 3/10;
	
	// init synth-defs that are actually playing...
	self.sdefs = ();
	self.sdefs.drops = List();
	self.sdefs.drops.add( Synth(\drops, [\out, self.gdc(4, 0), \amp, 0.6, \freq, 400, \rate, 0.3, \ctlamp, 0] ) );
	self.sdefs.drops.add( Synth(\drops, [\out, self.gdc(4, 1), \amp, 0.6, \freq, 100, \rate, 0.2, \ctlamp, 0] ) );
	self.sdefs.drops.add( Synth(\drops, [\out, self.gdc(4, 2), \amp, 0.4, \freq, 200, \rate, 0.2, \ctlamp, 0] ) );
	self.sdefs.drops.add( Synth(\drops, [\out, self.gdc(4, 3), \amp, 0.4, \freq, 230, \rate, 0.2, \ctlamp, 0] ) );
	
	self.sdefs.tock = nil;
	self.sdefs.stream = nil;
	self.sdefs.insect = nil;
	
	self.server.sync();

	// init controlling buses
	self.busses = ();
	self.busses.ctlAmp = Bus.control(self.server, 1);
	self.server.sync();

	// map busses to sdef-controls
	self.sdefs.drops.do {|sdef|
		sdef.map(\ctlamp, self.busses.ctlAmp);
	};
	self.server.sync;
	
	self.bootedUp.unhang;
};
// get drop channels
scene.gdc = { |self, items=2, n=1|
	(((self.channels-1)/items)*n).round;
};
scene.makeCaveDefs = { |self|
	self.sdefs.tock = Synth(\tock, [\ctlamp, 1]);
	self.sdefs.stream = Synth(\stream, [\ctlamp, 1]);
	self.sdefs.insect = Synth(\insect, [\ctlamp, 1]);
	self.server.sync;
	self.busses.streamDynamics = Bus.control(self.server, 1);
	self.sdefs.stream.map(\dynamics, self.busses.streamDynamics);
	[self.sdefs.tock, self.sdefs.stream, self.sdefs.insect].do{ |sdefs|
		sdefs.map(\ctlamp, self.busses.ctlAmp);
	};
};

scene.haltSelf = { |self|
	self.sdefs.drops.do { |sdef|
		sdef.free;
	};
	self.sdefs.tock.free;
	self.sdefs.stream.free;
	self.sdefs.insect.free;
	self.busses.streamDynamics.free;
	self.busses.ctlAmp.free;
	
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
		0. do nothing on fadeIn
		1. start instantly (tock fades in automatically)
		2. rise streamDynamics until end
		3. fade stuff out
	*/
	
	self.runner = Task({
		self.state = "running";
		(self.runtime * self.starttime * runtimemod).wait;
		self.makeCaveDefs();
		self.busses.ctlAmp.set(1);
		self.sustainer = {
			var dynamics = Line.kr(0, 1, self.runtime * self.sustime * runtimemod, doneAction: 2);
			Out.kr(self.busses.streamDynamics, dynamics);
		}.play;
		(self.runtime * self.sustime * runtimemod).wait;
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	self.ender = Task({
		self.endRamp = {
			Out.kr(self.busses.ctlAmp, Line.kr(1, 0, runtime, doneAction: 2));
		}.play;
		runtime.wait;
		self.haltSelf();
	}).play;
};

scene.loadSdefs = { |self|
	SynthDef(\tock, { |out, amp=1, freq=140, mfreq=170, ctlamp = 0|
		var snd, trig, env, etrig;
		etrig = Impulse.kr(0.01);
		trig = Impulse.kr(0.08);
		snd = SinOsc.ar([freq, freq*1.6]).sum * EnvGen.ar(Env.perc(0.001, 0.5), trig);
		snd = BPF.ar(snd, TRand.kr(160, 250, etrig), 8.reciprocal);
		snd = GVerb.ar(snd, 40, 5, 0.8);
		env = EnvGen.ar(Env.new([0.001,1,1,0.001], [60,20,75], 'exponential'), etrig);
		Out.ar(TRand.kr(0, self.channels-1, trig).round, snd*amp*env * self.vol.tock * ctlamp);
	}).add;
	
	SynthDef(\drops, { |out, amp, freq, rate, ctlamp = 0|
		var trig = Dust.ar(0.3, mul:0.7);
		var snd = SinOsc.ar(freq * Decay.ar(trig, 1/8).linlin(0, 1, 5, 0)) ;
		snd = snd * Decay.ar(trig, 0.1, 0.1).lag(0.05);
		snd = GVerb.ar(snd, 60, 4, 0.8);
		Out.ar(out, snd * amp * self.vol.drops * ctlamp);
	}).add;
	
	SynthDef(\stream, { |out, amp=0.0002, ctlamp = 0|
		var snd, freq, trig;
		trig = Dust.kr(150);
		freq = TExpRand.kr(400, 1500, trig) + LFNoise2.kr(20, mul: 100);
		snd = SinOsc.ar(freq);
		snd = GVerb.ar(snd, 40, 0.5, 0.8);
		Out.ar(out, snd*amp!self.channels * self.vol.stream * ctlamp);
	}).add;
	
	SynthDef(\insect, { |out,amp=0.01,rel, ctlamp = 0| 
		var env,noise1,noise2,snd,trig;
		trig = Dust.kr(0.04);
		noise1 = BPF.ar(WhiteNoise.ar(), LFNoise2.kr(2, 800, TRand.kr(2000, 5000, trig)),0.2);
		noise2 = BPF.ar(WhiteNoise.ar(), LFNoise2.kr(2, 800, TRand.kr(2000, 5000, trig)),0.15);
		snd = (noise1 + noise2) * SinOsc.kr(TRand.kr(4, 15, trig));
		env = EnvGen.ar(Env.linen(TRand.kr(0.1, 0.4, trig),TRand.kr(0.3, 1.5, trig),0.1,0.6), trig) * TRand.kr(0.05, 0.3, trig);
		snd = GVerb.ar(snd*env, 40, 8, 0.5);
		Out.ar(TRand.kr(0, self.channels-1, trig), snd * amp * self.vol.insect * ctlamp);
	}).add;
}

