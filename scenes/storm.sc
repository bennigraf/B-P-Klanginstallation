
var scene = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/scenes/proto.sc".load;
/*var rain = ~proto.deepCopy;*/

// META
scene.vol.rain = 0.7;
scene.vol.brrr = 0.6;
scene.vol.flashes = 1;


scene.bootUp = { |self|
	// set up busses, helper-sdefs, ... here
	
	// unusual timings...:
	self.runtime = 600;	// kind of arbitrary:
	self.starttime = 3/10;	// multipliers for runtime... Routine
	self.sustime = 4/10;
	self.endtime = 3/10;
	
	// needed to store ramps later:
	self.rain = ();
	self.brrr = ();
	self.flashes = ();
	
	// init synth-defs that are actually playing...
	self.sdefs = ();
	self.sdefs.rain = List();
	self.channels.do{ |n|
		8.do{ |o|
			self.sdefs.rain.add( Synth(\rain, [\out, n, \amp, 0]).play );
		}
	};
	self.sdefs.brrr = List();
	self.channels.do {|n|
		self.sdefs.brrr.add( Synth(\brrr, [\out, n, \amp, 0]).play );
	};
	self.sdefs.flashes = List();
	((self.channels/4).round.max(1)).do {|n|
		self.sdefs.flashes.add( Synth(\flash, [\amp, 0, \channels, self.channels]).play );
	};
	
	self.server.sync();

	// init controlling buses
	self.busses = ();
	self.busses.rain = ();
	self.busses.rain.intensity = Bus.control(self.server, 1);
	self.busses.rain.amp = Bus.control(self.server, 1);
	self.busses.brrr = ();
	self.busses.brrr.amp = Bus.control(self.server, 1);
	self.busses.brrr.upperLimit = Bus.control(self.server, 1);
	self.busses.flash = ();
	self.busses.flash.amp = Bus.control(self.server, 1);
	self.busses.flash.flashness = Bus.control(self.server, 1);
	self.server.sync();

	// map busses to sdef-controls
	self.sdefs.rain.do{ |sdef|
		sdef.map(\amp, self.busses.rain.amp, \intensity, self.busses.rain.intensity);
	};
	self.sdefs.brrr.do{ |sdef|
		sdef.map(\amp, self.busses.brrr.amp, \upperlimit, self.busses.brrr.upperLimit);
	};
	self.sdefs.flashes.do{ |sdef|
		sdef.map(\amp, self.busses.flash.amp, \flashness, self.busses.flash.flashness);
	};
	self.server.sync;
	self.bootedUp.unhang;
};

scene.haltSelf = { |self|
	self.sdefs.do{ |list|
		while({list.size > 0}, {
			list.pop.free;
		});
	};
	self.busses.do { |busses|
		busses.do {|bus|
			bus.free;
		};
	};
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
		2. after 60 sec, fade in brrr over 120 sec
		3. after 120 sec, start flashes/sustain mode
		4. sustain-mode rain intensity lfnoised for runtime, brrr vary upper limit...
		5. end brrr over 120 seconds
		6. after 60 seconds, end flashes over 30 or so seconds, end rain over 120 seconds
	*/
	
	self.runner = Task({
		self.state = "running";
		
		self.start(self.runtime * self.starttime, runtimemod);
		(self.runtime * self.starttime * runtimemod).wait;
		self.rain.sustainer = {
			var lvl = LFNoise1.kr(1/2).range(4, 6) + LFNoise1.kr(1/20);
			Out.kr(self.busses.rain.intensity, lvl);
		}.play;
		self.brrr.sustainer = {
			var upperLimit = 550 - LFNoise1.kr(1/8).range(0, 250) + Decay2.kr(Dust.kr(1/5), 2, 4, 400);
			Out.kr(self.busses.brrr.upperLimit, upperLimit);
		}.play;
		self.flashes.sustainer = {
			var flashness = LFNoise1.kr(1/5).range(0.01, 0.25);
			var flashAmp = LFNoise1.kr(1/6.32551).range(0.7, 1);
			Out.kr(self.busses.flash.flashness, flashness);
			Out.kr(self.busses.flash.amp, flashAmp);
		}.play;
		(self.runtime * self.sustime * runtimemod).wait;
		
		self.rain.sustainer.free; self.flashes.sustainer.free; self.brrr.sustainer.free;
	}).play;
	
	(self.runtime * (self.starttime + self.sustime) * runtimemod);
};

scene.start = {|self, runtime = 180, runtimemod = 1|
	
	self.starter = Task({
		self.rain.startRamp = ();
		self.rain.startRamp.amp = {
			Out.kr(self.busses.rain.amp, Line.kr(0.5, 1, runtime/3 * runtimemod, doneAction: 2));
		}.play;
		self.rain.startRamp.intensity = {
			Out.kr(self.busses.rain.intensity, Line.kr(0, 5, runtime * (2/3) * runtimemod, doneAction: 2));
		}.play;
		(runtime/3 * runtimemod).wait;
		
		self.brrr.startRamp = ();
		self.brrr.startRamp.amp = {
			Out.kr(self.busses.brrr.amp, Line.kr(0, 1, runtime * (2/3) * runtimemod, doneAction: 2));
		}.play;
		self.brrr.startRamp.upperLimit = {
			Out.kr(self.busses.brrr.upperLimit, Line.kr(200, 550, runtime * (2/3) * runtimemod, doneAction: 2));
		}.play;
		
	}).play;
	(runtime * runtimemod);
	
};

scene.end = { |self, runtime = nil, runtimemod = 1|
	if(runtime.isNil) {
		runtime = self.runtime * self.endtime * runtimemod;
	};
	
//	5. end brrr over 120 seconds
//	6. after 60 seconds, end flashes over 30 or so seconds, end rain over 120 seconds
	
	self.ender = Task({
		self.brrr.endRamp = {
			Out.kr(self.busses.brrr.upperLimit, Line.kr(800, 200, runtime * (2/3)));
			Out.kr(self.busses.brrr.amp, Line.kr(1, 0, runtime * (2/3), doneAction: 2) );
		}.play;
		
		(runtime/3).wait;		
		self.flashes.endRamp = {
			Out.kr(self.busses.flash.amp, Line.kr(1, 0, runtime/6, doneAction: 2))
		}.play;
		
		self.rain.endRamp = ();
		self.rain.endRamp.intensity = {
			Out.kr(self.busses.rain.intensity, Line.kr(5, 0, runtime * (2/3), doneAction: 2));
		}.play;
		
		(runtime/3).wait;
		self.rain.endRamp.amp = {
			Out.kr(self.busses.rain.amp, Line.kr(1, 0.2, runtime/3, doneAction: 2));
		}.play;
		(runtime/3).wait;
		self.haltSelf();
	}).play;
};

scene.loadSdefs = {
	SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 0.8, intensity = 1|
		var snd, trig, filtfreq, distmod;
		trig = Dust.ar(intensity);
		snd = PinkNoise.ar() * Decay.ar(trig, 0.4).lag(0.02);
		distmod = LFNoise1.kr(1).range(0.2, 1);
		filtfreq = distmod.linexp(0.2, 1, 520, 2390);
		snd = BPF.ar(snd, [filtfreq/2, filtfreq, filtfreq*2], [0.2, 2.5, 0.3], mul: [0.5, 1, 0.3]).sum;
		snd = snd * distmod;
	//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
		Out.ar(out, snd*amp * scene.vol.rain);
	}).add;
	
	SynthDef(\brrr, { |out=0, upperlimit = 200, amp = 0.5|
		var snd = BrownNoise.ar();
		snd = snd * Decay2.ar(Dust.ar(LFNoise0.kr(13).range(1, 100)), 0.05, 0.5);
		snd = RLPF.ar(snd, LFNoise0.kr(13).range(100, upperlimit), 0.8);
		snd = Compander.ar(snd, snd, 0.7, 1, 1/3, 10, 10) * 0.3;				// In, Ctrl, Thresh, Below, Above, Attack, Release
		snd = snd.softclip * 0.4;
		snd = FreeVerb.ar(snd, 0.45, 8, 0.4);		// Mix, Room, Damp
		Out.ar(out, snd * amp * scene.vol.brrr);
	}).add; 
	
	SynthDef(\flash, { |amp = 0.5, flashness = 0.1, channels = 8|
		var snd, flashes;
		var flashTrig = Dust.kr(flashness);
		var flashDecay = Decay2.kr(flashTrig, 0.08, 0.95);
		var flashshsh = Latch.kr(WhiteNoise.kr, flashTrig).range(7000, 13000);
		snd = BrownNoise.ar() * Decay2.ar(Dust.ar(LFNoise0.kr(13).range(1, 100)), 0.05, 0.5);
		flashes = RLPF.ar(snd, flashDecay.linlin(0, 1, 1000, flashshsh),  // freq
				flashDecay.linlin(0,1,0.1,2.8), // rq
				mul: Decay2.kr(flashTrig, 0.05, 1) * 2 // envelope
			);
		flashes = flashes.softclip;
		Out.ar(Latch.kr(WhiteNoise.kr, flashTrig).range(0, channels-1).round, flashes * amp * scene.vol.flashes);
	}).add;
}

