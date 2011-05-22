
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/_proto.sc".load;
/*var rain = ~proto.deepCopy;*/

// META
scene.vol.leaves = 0.6;
scene.vol.birds = 0.25;
scene.vol.wood2 = 1;

scene.runtime = 480;	// 8 mins
scene.starttime = 1/8;	// 1 mins
scene.sustime = 5/8;	// 5 mins
scene.endtime = 2/8;	// 2 mins


scene.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
	
	// init synth-defs that are actually playing...
	self.sdefs = ();
	self.sdefs.leaves = List();
	self.channels.do{ |n|
		self.sdefs.leaves.add (Synth(\leaves, [\amp, 0]) )
	};
	
	self.sdefs.birds = List();
	(1..7).do{ |n|
		self.sdefs.birds.add( Synth(("bird"++n).asSymbol, [\amp, 0]) );
	};
	
	self.sdefs.wood2 = Synth(\wood2, [\amp, 0]);
	self.server.sync();

	// init controlling buses
	self.busses = ();
	self.busses.leaves = ();
	self.busses.leaves.amp = Bus.control(self.server, 1);
	self.busses.birds = ();
	self.busses.birds.dens = Bus.control(self.server, 1);
	self.busses.birds.amp = Bus.control(self.server, 1).set(0);
	self.busses.wood2Amp = Bus.control(self.server, 1).set(0);
	self.server.sync();

	// map busses to sdef-controls
	self.sdefs.leaves.do{ |sdef|
		sdef.map(\amp, self.busses.leaves.amp);
	};
	self.sdefs.birds.do { |sdef|
		sdef.map(\amp, self.busses.birds.amp, \dens, self.busses.birds.dens);
	};
	self.sdefs.wood2.map(\amp, self.busses.wood2Amp);
	self.server.sync;
	self.bootedUp.unhang;
};
scene.haltSelf = { |self|
	while({self.sdefs.leaves.size > 0}, {
		self.sdefs.leaves.pop.free;
	});
	self.sdefs.leaves = nil;
	while({self.sdefs.birds.size > 0}, {
		self.sdefs.birds.pop.free;
	});
	self.sdefs.wood2.free;
	self.busses.leaves.do {|bus|
		bus.free;
	};
	self.busses.birds.do {|bus|
		bus.free;
	};
	self.busses.wood2Amp.free;
	self.birdBufs.do {|buf|
		buf.free;
	};
	self.busses.wood2Amp.free;
	self = nil;
};


scene.run = { |self, runtime = nil, runtimemod = 1|
	
	if(runtime.isNil.not) {
		self.runtime = runtime;
	};
	
	/* 
		1. fade in leaves, 120 sec
		2. after 120 sec in sus mode, fade in birds/tocktock/..., alter intensity
		3. end; stop birds, fade out over 120 sec
	*/
	
	self.runner = Task({
		var sustime;
		self.state = "running";

		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;
		
		sustime = self.runtime * self.sustime * runtimemod;
		
		self.sustainer = ();
		self.sustainer.birds = {
			var amp, dens;
			amp = Line.kr(0.2, 1, sustime * 0.2);
			Out.kr(self.busses.birds.amp, amp);
			dens = Line.kr(0.2, 1, sustime * 0.2) ** 2;
			dens = dens + LFNoise1.kr(1/3, mul: dens/3);
			Out.kr(self.busses.birds.dens, dens);
		}.play;
		self.sustainer.wood2 = {
			var amp = Line.kr(0, 1, sustime, doneAction: 2);
			Out.kr(self.busses.wood2Amp, amp);
		}.play;
		(self.runtime * self.sustime * runtimemod).wait;
		self.sustainer.birds.free;
		
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 120, runtimemod = 1|
	self.starter = Task({
		self.startRamp = {
			Out.kr(self.busses.leaves.amp, Line.kr(0, 1, runtime * 2 * runtimemod, doneAction: 2));
		}.play;
	}).play;
	(runtime * runtimemod);
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	self.ender = Task({
		self.endRamp = ();
		self.endRamp.leaves = {
			Out.kr(self.busses.leaves.amp, Line.kr(1, 0, runtime, doneAction: 2));
		}.play;
		self.endRamp.birds = {
			var sig = Line.kr(1, 0, runtime/3, doneAction: 2);
			Out.kr(self.busses.birds.dens, sig);
			Out.kr(self.busses.birds.amp, sig);
		}.play;
		self.endRamp.wood2 = {
			var amp = Line.kr(1, 0, runtime, doneAction: 2);
			Out.kr(self.busses.wood2Amp, amp);
		}.play;
		runtime.wait;
		self.busses.birds.dens.set(0);
		self.busses.birds.amp.set(0);
		self.endRamp.wood2.set(0);
		self.haltSelf();
	}).play;
};

scene.loadSdefs = { |self|
	
	self.birdBufs = List(); // hack, need to access them later to delete them again...
	
	SynthDef(\leaves, { |out, amp|
		var snd, env, trig;
		trig = Dust.ar(1/2);
		snd = BPF.ar(BrownNoise.ar(1) * Decay.ar(trig, 18, mul:0.051).lag(2.5), 3200 * LFNoise1.kr(1/5).range(0.7, 1.1), 0.8.reciprocal);
		snd = PanAz.ar(self.channels, snd, LFNoise2.ar(0.05), 1, (self.channels/3).max(1)*2);	
	Out.ar(out, snd*amp * self.vol.leaves);
	}).add;

	SynthDef(\bird1, { |amp = 0, dens = 0, densmod = 1|
		var envarr = Env([0.8,0.2,1], [1,1], 2).asSignal(1378).asArray;
		var envbuff = Buffer.loadCollection(s, envarr);
		var env, snd;
		var rate = 3;
		var att = 0.1;
		var dec = 0.1;
		var trig = Dust.kr(dens * densmod);
		var elength = 1/rate - (att + dec);
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.5, trig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + [500,600] + 2000).sum);
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird2, { |amp = 0, dens = 0, densmod = 1|
		var envarr = Env([0.01,1], [1], 2).asSignal(1378).asArray;
		var envbuff = Buffer.loadCollection(s, envarr);
		var env, snd;
		var rate = 6;
		var att = 0.1;
		var dec = 0.1;
		var itrig = Dust.kr(dens * densmod);
		var trig = Trig1.kr(itrig, TChoose.kr(itrig, [2,4])) * Impulse.kr(2);
		var elength = 1/rate - (att + dec);
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.8, itrig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + TRand.kr(900, 1300, itrig)))!2;
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird3, { |amp = 0, dens = 0, densmod = 1|
		var envarr = Env([1, 0.01], [1], 'sine').asSignal(1378).asArray;
		var envbuff = Buffer.loadCollection(s, envarr);
		var env, snd;
		var rate = 4;
		var att = 0.01;
		var dec = 0.1;
		var itrig = Dust.kr(dens * densmod);
		var trig = Trig1.kr(itrig, TChoose.kr(itrig, [1,2])) * Impulse.kr(2.5);
		var elength = 1/rate - (att + dec) - 0.05;
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.5, trig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + [500, 550]).sum);
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird4, { |amp = 0, dens = 0, densmod = 1|
		var envarr = Env([0.01, 0.8, 0.01], [1,0.5], 2).asSignal(1378).asArray;
		var envbuff = Buffer.loadCollection(s, envarr);
		var env, snd;
		var rate = 3;
		var att = 0.1;
		var dec = 0.1;
		var itrig = Dust.kr(dens * densmod);
		var trig = Trig1.kr(itrig, TChoose.kr(itrig, [1,2])) * Impulse.kr(0.5);
		var elength = 1/rate - (att + dec);
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.3, itrig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + TRand.kr(400, 600, itrig)))!2;
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird5, { |amp = 0, dens = 0, densmod = 1|
		var envarr, envbuff, env, snd, rate, att, dec, trig, elength;
		 envarr = Signal.newClear(1378);
			envarr.waveFill({ arg x, i; sin(x**9).max(0) },4 ,2pi).asArray;
		 envbuff = Buffer.loadCollection(s, envarr);
		 rate = 3;
		 att = 0.1;
		 dec = 0.1;
		 trig = Dust.kr(dens * densmod);
		 elength = 1/rate - (att + dec);
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.5, trig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + [500,600] + 2000).sum);
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird6, { |amp = 0, dens = 0, densmod = 1|
		var envarr, envbuff, env, snd, rate, att, dec, trig, elength;
		envarr = Signal.newClear(1378);
			envarr.waveFill({ arg x, i; sin(x**9).max(0) },4 ,2pi).asArray;
		envbuff = Buffer.loadCollection(s, envarr);
		rate = 0.05;
		att = 0.1;
		dec = 0.1;
		trig = Dust.kr(dens * densmod);
		elength = 0.5 - (att + dec);
		env = EnvGen.ar(Env.linen(att, elength, dec, 0.6), trig) * TRand.kr(0.1, 0.5, trig);
		snd = env * (SinOsc.ar(PlayBuf.kr(1, envbuff, (rate*2), trig, loop:0) * 900 + TRand.kr(900, 1900, trig)));
		Out.ar((Latch.ar(WhiteNoise.ar, trig)*self.channels).round, snd*amp * self.vol.birds);
		
		self.birdBufs.add(envbuff);
	}).add;
	
	SynthDef(\bird7, { |amp = 0, freq=400, dens = 0, densmod = 1|
		var snd, env, trig, itrig;
		itrig = Dust.kr(dens * densmod);
		trig = Trig1.kr(itrig, TChoose.kr(itrig, [2,4])) * Impulse.kr(5);
		snd = SinOsc.ar([freq, freq*1.6]).sum;
		env = EnvGen.ar(Env.perc(0.0001, 0.3), trig) * TRand.kr(0.4, 0.8, trig);
		snd = BPF.ar(snd*env, 1500, 8.reciprocal);
		Out.ar((Latch.ar(WhiteNoise.ar, itrig)*self.channels).round, snd*amp * self.vol.birds);
	}).add;
	
	SynthDef(\wood2, { |out, amp = 0, freq|
		var snd, env, synth, trig;
		trig = Impulse.kr(1/10) + Impulse.kr(1/10, 7/8) + Impulse.kr(1/20, 31/32) + Impulse.kr(1/40, 7/16);
		synth = SinOsc.ar(TRand.kr(250, 800, trig) * [1, 1.6]).sum/2;
		env = EnvGen.ar(Env.perc(0.0001, 0.3), trig);
		snd = BPF.ar(synth*env, 1200, 10.reciprocal);
		snd = GVerb.ar(snd, 30, 5, 0.7).sum;
	//	DetectSilence.ar(snd, doneAction:2);
		Out.ar(self.channels.rand, snd * amp * self.vol.wood2);
	}).add;
	
}
