

Storm {
	
	var <>state;		// holds current state: "idle", "starting", "running", "ending"
	var <>debugmode = false, <>debugger;	// holds debugmode (bool), debugger (Task)
	var <>runtime = 600; // default timings
	var <>runner; // holds run-Task
	var <>rainquantbus, <>busvalue, <>rainSdefs, <>rainRamp; // various rain helper
	var <>brrrUpper, <>brrrAmp, <>brrrSdefs, <>brrrRamp; // various brrr helper
	
	var <>channels;		// number of channels used
	
	*initClass {
		// hmm...
	}
	
	*new { |channels = 8|
		^super.new.init(channels);
	}
	
	init { |channels = 8|
		// boot server if necessary
		if(Server.default.serverRunning.not){
			"Booting Server first, give me a second!".postln;
		};
		// set up synthdefs, busses
		Server.default.waitForBoot({
			this.channels = channels;
			this.loadSdefs;
			this.rainSdefs = List();
			this.brrrSdefs = List();
			this.rainquantbus = Bus.control(Server.default, 1).set(0);
			this.brrrUpper = Bus.control(Server.default, 1).set(200);
			this.brrrAmp = Bus.control(Server.default, 1).set(0);
		});
		
		this.state = "idle";
		^this;
	}
	
	/*
	 * Main routine. Runs rain once depending on given or default times.
	 */
	run { |runtimemod = 1|
		var run; // Task
		
		/* META
		 *
		 *	1. Fade in rain for 120 sec or so
		 *	2. Fade in brrrring after 60 sec or so
		 *	3. bring both to max intensity
		 *	4. reduce both a bit
		 *	5. introduce wandering drones
		 *	6. keep for a while
		 *	7. fade stuff out / go to next
		 */
		
		this.state = "running";
		
		run = Task({
			var flash, flashRamp, flashBus;
			
			// 1. Fade in rain for 120 sec or so...
			this.setupRain();
			this.rainRamp = {
				var ramp = EnvGen.kr(Env([0, 5, 5, 4, 4, 0], [120, 60, 60, 180, 120] * runtimemod), doneAction: 2);
				ramp = ramp + LFNoise1.kr(1/20, mul:EnvGen.kr(Env([0, 0, 1, 1, 0], [180, 60, 180, 60])) );
				Out.kr(this.rainquantbus, ramp);
			}.play;
			(60 * runtimemod).wait;
			
			this.setupBrrr();
			this.brrrRamp = {
				var ampRamp, upperRamp;
				ampRamp = EnvGen.kr(Env([0, 1, 1, 0.7, 0.7, 0]/4, [60, 60, 60, 120, 120] * runtimemod), doneAction: 2);
				Out.kr(this.brrrAmp, ampRamp);
				upperRamp = EnvGen.kr(Env([200, 893, 388, 283], [120, 60, 180] * runtimemod));
				Out.kr(this.brrrUpper, upperRamp);
			}.play;
			
			(60 * runtimemod).wait;
			
			flashBus = Bus.control(Server.default, 1);
			flash = Synth(\flash, [\flashness, 0, \channels, this.channels, \amp, 1]).play.map(\flashness, flashBus);
			flashRamp = {
				var ramp = EnvGen.kr(Env([0, 0.4, 0], [60, 120] * runtimemod), doneAction: 2);
				Out.kr(flashBus, ramp);
			}.play;
			(180 * runtimemod).wait;
			flash.free; flashBus.free;
			
			(240*runtimemod).wait;
			this.stop;
			
		}).play; // Yay!
		
		^(540 * runtimemod);
	}
	
	// Stop the whole thing immediatly
	stop {
		this.clearRain();
		this.clearBrrr();
		[this.rainRamp, this.brrrRamp].do{ |obj|
			if(obj.isKindOf(Synth)) {
				obj.free;
			};
		};
		this.rainRamp = this.brrrRamp = nil;
		this.rainquantbus.set(0);
		this.brrrAmp.set(0);
		this.brrrUpper.set(200);
		this.state = "idle";
	}
	
	setupRain {
		if(this.rainSdefs.size == 0) {
			this.channels.do{ |n|
				12.do{ |o|
					this.rainSdefs.add(Synth(\rain, [\out, n, \quantbus, this.rainquantbus]).play );
				}
			};
		}
	}
	clearRain {
		while({this.rainSdefs.size > 0}, {
			this.rainSdefs.pop.free;
		});
	}
	
	setupBrrr {
		if(this.brrrSdefs.size == 0) {
			this.channels.do{ |n|
				this.brrrSdefs.add(Synth(\brrr, [\out, n, \amp, 0]).play
					.map(\amp, this.brrrAmp, \upperlimit, this.brrrUpper) );
			};
		};
	}
	clearBrrr {
		while({this.brrrSdefs.size > 0}, {
			this.brrrSdefs.pop.free;
		});
	}
	
	
	// Debug mode. Turn on with .debug("on") and off with .debug("off").
	debug { |debug|
		if(debug.asString=="on"){
			this.debugger.stop;
/*			this.debugger = SynthDef(\debugger, {
					this.quantbus.kr.poll;
				}).play;*/
			this.debugger = Task({
					loop{
						this.quantbus.get({|val|
							("Quantbus: "++val).postln;
						});
						("State: "++this.state).postln;
						("Synthdefs existent: "++this.sdefs.size).postln;
						if( this.state == "idle", {
							2.wait;
						}, {
							0.5.wait;	
						} );
					}
				}).play;
			this.debugmode = true;
		};
		if(debug.asString=="off"){
			this.debugger.stop;
			this.debugmode = false;
		};
		
		"Use \"on\" or \"off\" to turn debugging on or off.".postln;

		this.debugmode.if{
			"Debug-Mode is on.".postln;
		} {
			"Debug-Mode is off".postln;
		};
		^this.debugmode;
	}
	
	
	// load necessary SynthDefs. Feels strange to do this in a class...
	loadSdefs {
		SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 0.8, quantbus|
			var snd, trig, filtfreq, distmod;
			trig = Dust.ar(In.kr(quantbus));
			snd = PinkNoise.ar() * Decay.ar(trig, 0.4).lag(0.02);
			distmod = LFNoise1.kr(1).range(0.2, 1);
			filtfreq = distmod.linexp(0.2, 1, 520, 2390);
			snd = BPF.ar(snd, [filtfreq/2, filtfreq, filtfreq*2], [0.2, 2.5, 0.3], mul: [0.5, 1, 0.3]).sum;
			snd = snd * distmod;
		//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
			Out.ar(out, snd*amp);
		}).add;
		
		SynthDef(\brrr, { |out=0, upperlimit = 200, amp = 0.5, flashness = 0.1|
			var snd = BrownNoise.ar();
			snd = snd * Decay2.ar(Dust.ar(LFNoise0.kr(13).range(1, 100)), 0.05, 0.5);
			snd = RLPF.ar(snd, LFNoise0.kr(13).range(100, upperlimit), 0.8);
			snd = Compander.ar(snd, snd, 0.7, 1, 1/3, 10, 10) * 0.3;				// In, Ctrl, Thresh, Below, Above, Attack, Release
			snd = snd.softclip * 0.4;
			snd = FreeVerb.ar(snd, 0.45, 8, 0.4);		// Mix, Room, Damp
			Out.ar(out, snd * amp);
		//	Out.ar(~revbus, snd * 0.5 * ~bus[30].kr);
		}).add; 
		
		SynthDef(\flash, { |out = 0, amp = 0.5, flashness = 0.1, channels = 8|
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
			Out.ar(Latch.kr(WhiteNoise.kr, flashTrig).range(0, channels-1).round, flashes * amp);
		}).add;
	}
	
	
}