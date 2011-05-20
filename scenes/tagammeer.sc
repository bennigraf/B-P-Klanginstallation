
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/_proto.sc".load;

// META
scene.vol.meer = 1;
scene.vol.bass = 1;
scene.vol.rhodes = 1;
scene.vol.snare = 0.8;
scene.vol.tikik = 1;

scene.bootUp = { |self|
	"booting stuff up".postln;
	self.runtime = 600;	// kind of arbitrary:
	self.starttime = 2/10;	// multipliers for runtime... Routine
	self.sustime = 5/10;
	self.endtime = 3/10;
	
	"setting buffers".postln;
	self.buffers = ();
	self.buffers.tikik = Buffer.alloc(s,44100*15,1);
	self.server.sync();
	
	"setting sdefs".postln;
	// set up busses, helper-sdefs, ... here
	self.sdefs = ();
	self.sdefs.waves = List();
	self.channels.do{ |n|
		5.do { |o|
			self.sdefs.waves.add( Synth(\waves, [\amp, 0]) );
		};
	};
	/*
	SynthDef(\waves, { |out, amp=1, freq=900, rq=1.25|
		var snd, env, trig;
		trig = Dust.ar(0.2);
		snd = BPF.ar(BrownNoise.ar(0.4) * Decay.ar(trig, 3, mul:0.1).lag(1.5), freq, rq);
		snd = PanAz.ar(self.channels, snd, LFNoise2.ar(0.05), 1, 8);
	Out.ar(out, snd*amp * self.vol.meer);
	}).add;
	*/
	self.server.sync();
	
	"some busses".postln;
	// init controlling buses
	self.busses = ();
	self.busses.wavesAmp = Bus.control(self.server, 1);
	self.busses.tikik = Bus.audio(self.server, 1);
	self.busses.tikikAmp = Bus.control(self.server, 1);
	self.busses.tickAmp = Bus.control(self.server, 1);
	self.busses.rhodesAmp = Bus.control(self.server, 1);
	self.busses.bassAmp = Bus.control(self.server, 1);
	self.server.sync();
	
	"tikikikik".postln;
	self.sdefs.tikik = Synth(\tikikikik, [\echoBus, self.busses.tikik, \combBuf, self.buffers.tikik, \amp, 0]);
	
	"map busses".postln;
	// map busses to sdef-controls
	self.sdefs.waves.do{ |sdef|
		sdef.map(\amp,  self.busses.wavesAmp);
	};
	self.sdefs.tikik.map(\amp, self.busses.tikikAmp);
	self.server.sync;
	
	"done booting!".postln;
	self.bootedUp.unhang;
};
scene.haltSelf = { |self|
	self.sustainer.rhodes.stop;
	self.sustainer.bass.stop;
	self.sustainer.tick.stop;
	self.sdefs.waves.do{|sdef|
		sdef.free;
	};
	self.sdefs.tikik.free;
	
	self.buffers.do{|buffer|
		buffer.free;
	};
	
	self.busses.do{|bus|
		bus.free;
	};
	self = nil;
};


scene.run = { |self, runtime = nil, runtimemod = 1|
	
	if(runtime.isNil.not) {
		self.runtime = runtime;
	};
	
	/* 
		1. waves fade in over starttime.
		2. tikikik fades in 
		3. Bass + Rhodes kick in
		4. end: everything fades out
	*/
	"running...".postln;
	self.runner = Task({
		var runtime;
		
		self.state = "running";
		"starting stuff".postln;
		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;
		
		runtime = self.runtime * self.sustime * runtimemod;
		self.sustainer = ();
		"tiktik sustainer".postln;
		self.sustainer.tikik = {
			Out.kr(self.busses.tikikAmp, Line.kr(0, 1, runtime * (1/5), doneAction: 2));
		}.play;
		"tick pbind".postln;
		self.sustainer.tick = Pbind(
			\instrument, \ticketick,
			\amp, Pseq([0, 0.7, 0, 0.2], inf),
			\ampBus, self.busses.tickAmp,
			\echoBus, self.busses.tikik,
			\dur, 1
		).play(quant:4);
		
		(runtime * (1/5)).wait;
		
		"tik and bass to 1".postln;
		self.busses.tickAmp.set(1);
		self.busses.bassAmp.set(1);
		"bass pbind".postln;
		self.sustainer.bass = Pbind(
			\instrument, \bass,
			\scale, Scale.minor,
			\degree, Pseq([Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, \, \, \]),
						Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, 6, \, \])], inf),
			\mtranspose, -3,
			\octave, 3,
			\dur, Pseq([1/4], inf),
			\sus, Pseq([Pseq([1.5, \, 3, \, \, \, \, 3, \, \, 3.5, \, \, \, \, \]),
					   Pseq([1.5, \, 3, \, \, \, \, 3, \, \, 3.5, \, \, 2, \, \])], inf),
			\ampBus, self.busses.bassAmp
		).play(quant:4);
		
		"rhodesAmp 1".postln;
		self.busses.rhodesAmp.set(1);
		"rhodes pdbind".postln;
		self.sustainer.rhodes = Pbind(
			\instrument, \rhodes,
			\scale, Scale.minor,
			\degree, Pseq([
						Pn(Pxrand([0,1,2,3,4,5,6,7, [0,2], [0,4], [4,7]]), 160),
							Pseq([0,\,\,[4,7],\,\,\,[0,5],\,\,\,[0,2],\,\,\,\]),
							Pseq([0,2,4,\,6,\,5,\,\,3,\,\,1,0,\,\]),
							Pseq([7,\,5,\,6,\,4,\,2,\,0,\,\,\,\,\]),
							Pseq([[0,4],\,\,\,\,\,\,\,\,\,\,\,\,[3,6],\,\])
							],inf),
			\amp,  Pseq([
						Pxrand([Pseq([0.4,0,0,0,0.4,0,0,0,0.4,0,0,0,0.4,0,0,0]),
							Pseq([0.4,0,0.4,0,0,0,0,0,0.4,0,0,0,0.4,0.4,0,0]),
							Pseq([0.4,0,0.4,0,0.4,0,0,0,0,0,0,0,0.4,0,0,0])
						], 10),
						Pseq([0.4,0,0,0.4,0,0,0,0.4,0,0,0,0.4,0,0,0,0]),
						Pseq([0.4,0.4,0.4,0,0.4,0,0.4,0,0,0.4,0,0,0.4,0.4,0,0]),
						Pseq([0.4,0,0.4,0,0.4,0,0.4,0,0.4,0,0.4,0,0,0,0,0]),
						Pseq([0.4,0,0,0,0,0,0,0,0,0,0,0,0,0.4,0,0])
						],inf),
			\speakertrig, 1,
			\mtranspose, -3,
			\octave, 6,
			\dur, Pseq([Pn(1/4,15), 4.25], inf),
			\sus, Pseq([1],inf),
			\ampBus, self.busses.rhodesAmp
		).play(quant:4);
		
		(self.runtime * self.sustime * runtimemod).wait;
		
		
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 120, runtimemod = 1|
	self.startRamp = ();
	"start ramp".postln;
	self.startRamp.waves = {
		Out.kr(self.busses.wavesAmp, Line.kr(0, 1, runtime * runtimemod, doneAction: 2));
	}.play;
	(runtime * runtimemod);
	
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
	self.ender = Task({
		{
			Out.kr(self.busses.bassAmp, Line.kr(1, 0, runtime, doneAction: 2));
			Out.kr(self.busses.rhodesAmp, Line.kr(1, 0, runtime, doneAction: 2));
			Out.kr(self.busses.wavesAmp, Line.kr(1, 0, runtime, doneAction: 2));
			Out.kr(self.busses.tickAmp, Line.kr(1, 0, runtime, doneAction: 2));
			Out.kr(self.busses.tikikAmp, Line.kr(1, 0, runtime, doneAction: 2));
		}.play;
		runtime.wait;
		self.haltSelf();
	}).play;
};

scene.loadSdefs = { |self|
	SynthDef(\waves, { |out, amp=1, freq=900, rq=1.25|
		var snd, env, trig;
		trig = Dust.ar(0.2);
		snd = BPF.ar(BrownNoise.ar(0.4) * Decay.ar(trig, 3, mul:0.1).lag(1.5), freq, rq);
		snd = PanAz.ar(self.channels, snd, LFNoise2.ar(0.05), 1, 8);
	Out.ar(out, snd*amp * self.vol.meer);
	}).add;
	
	SynthDef(\rhodes, { |out, amp=0.4, freq, speakertrig, ampBus|
			var snd, env;
			snd = SinOsc.ar(freq, mul: 0.2) * SinOsc.ar(16).range(0.8,1);
			env = EnvGen.ar(Env.perc(0.05, 2), doneAction:2);
//			snd = snd/2 + GVerb.ar(snd/2, 40, 2, 0.8, earlyreflevel: 0,taillevel:1).sum;
			Out.ar(TIRand.kr(0,self.channels-1,speakertrig), Pan2.ar(snd*env*amp * In.kr(ampBus) * self.vol.rhodes, SinOsc.kr(4)) );
	}).add;
	
	SynthDef(\bass, { |out, amp=1, freq, trig, sus, bassAmp|
		var snd, env;
		env = EnvGen.ar(Env.perc(0.04, sus), doneAction:2) + EnvGen.kr(Env.perc(0.02, 0.1));
		snd = SinOsc.ar(freq) * SinOsc.ar(2).range(0.8,1) * env;
		snd = snd.round(0.5**6);
		snd = snd.clip2(MouseY.kr(2, 0.2));
		snd = RLPF.ar(snd, [188, 155], 0.1).sum/2;
		snd = (snd*0.26).softclip;
		Out.ar(0, snd*env*amp * In.kr(bassAmp) * self.vol.bass!self.channels)
	}).add;
	
	SynthDef(\ticketick, { |out, amp=1, trig, echoAmp = 0.5, echoBus, ampBus|
			var snd, env;
			env = EnvGen.ar(Env.perc(0.01,0.1));
			snd = PinkNoise.ar()*env;
			snd = RHPF.ar(snd, 1204, 0.12);
			snd = FreeVerb.ar(snd, 0.5, 0.8, 0.3);
			EnvGen.kr(Env.perc(1, 4), doneAction: 2);
			Out.ar(TIRand.kr(0, self.channels, trig), snd*amp * In.kr(ampBus) * self.vol.snare);
			Out.ar(echoBus, snd * echoAmp * amp);
	}).add;
	
	SynthDef(\tikikikik, { |in, amp = 1, echoBus, combBuf|
			var snd, buf, trig;
			snd = In.ar(echoBus);
			snd = snd + BufCombC.ar(combBuf, snd, LFNoise2.kr(1/7.6).range(0.2475, 0.25), 15);
			snd = RHPF.ar(snd, SinOsc.kr(0.05).range(5000, 10000), 0.1);
//			snd = GVerb.ar(snd, 40, 2).sum;
			snd = PanAz.ar(self.channels, snd, LFNoise2.ar(0.1), 1, 4);
			Out.ar(0, snd * amp * (1-Decay2.kr(Dust.kr(0.031), 2, 2, mul: 0.77)) * self.vol.tikik);
	}).add;
	
}

