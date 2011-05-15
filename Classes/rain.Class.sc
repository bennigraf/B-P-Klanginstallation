

Rain {
	
	var <>state;		// holds current state: "idle", "starting", "running", "ending"
	var <>debugmode = false, <>debugger;	// holds debugmode (bool), debugger (Task)
	var <>runtime = 425, <>starttime = 120, <>sustime = 185, <>endtime = 120; // default timings
	var <>startersynth, <>sustainersynth, <>endersynth;	// will hold ramp-synths
	var <>startertask, <>sustainertask, <>endertask; // holds tasks that control rampsynths
	var <>quantbus, <>busvalue, <>sdefs; // various
	
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
			this.sdefs = List();			
			this.quantbus = Bus.control(Server.default, 1).set(0);
		});
		
		this.state = "idle";
		^this;
		
	}
	
	/*
	 * Main routine. Runs rain once depending on given or default times.
	 */
	run { |runtime = 0, starttime = 0, sustime = 0, endtime = 0|
		var run; // Task
		
		// set times if given, otherwise just use defaults.
		if (starttime != 0) { this.starttime = starttime };
		if (sustime != 0) { this.sustime = sustime };
		if (endtime != 0) { this.endtime = endtime };
		if (runtime != 0) {
			this.runtime = runtime;
			this.starttime = runtime * (2/7);
			this.sustime = runtime * (3/7);
			this.endtime = runtime * (2/7);
		};
		
		run = Task({
			this.start(this.starttime);
			this.starttime.wait;
				// sustain-mode starts automatically after start
			this.sustime.wait;
			this.end(this.endtime);
				// .end cleans up automatically
		});
		run.play; // Yay!
	}
	
	// Stop the whole thing immediatly
	stop {
		this.prepareStateChange();
		this.freesynths();
		this.quantbus.set(0);
		this.state = "idle";
	}
	
	// starting stuff up: ramp-synth
	start { |ramptime = 0|
		this.state = "starting";
		this.checkforsynths();
		
		this.prepareStateChange();
		
		// use default ramptime is none was given
		if (ramptime == 0) {
			ramptime = this.starttime;
		};
		
		this.startertask = Task({
			this.startersynth = {
				var ramp = Line.kr(0, 5, ramptime, doneAction: 2);
				Out.kr(this.quantbus, ramp);
			}.play;
			ramptime.wait;
			this.sus;	// start sus automatically after start
		}).play;
		
	}
	
	
	// sustainer - controls synths while running in normal mode - could run forever ;-)
	sus {
		this.state = "running";
		this.checkforsynths();
		
		this.prepareStateChange();
		
		this.sustainertask = Task({
			this.sustainersynth = {
				var lvl = LFNoise1.kr(1/2).range(4, 6) + LFNoise1.kr(1/20);
				Out.kr(this.quantbus, lvl);
			}.play;
		}).play;
	}
	
	// end stuff slowly, fade out
	end { |ramptime = 0|
		this.state = "ending";
		this.checkforsynths();
		
		this.prepareStateChange();
		
		// use default ramptime is none is given
		if (ramptime == 0) {
			ramptime = this.endtime;
		};
		
		this.endertask = Task({
			this.endersynth = {
				var ramp = Line.kr(5, 0, ramptime, doneAction: 2);
				Out.kr(this.quantbus, ramp);
			}.play;
			ramptime.wait;
			this.stop();	// clean everything up
		}).play;
	}
	
	// Freeing ramp-synths, remove tasks
	//  => states can be startet even though other states are still running...
	prepareStateChange {
		[this.startersynth, this.sustainersynth, this.endersynth].do{ |obj|
			if(obj.isKindOf(Synth)) {
				obj.free;
				obj = nil;
			};
		};
		this.startersynth = this.sustainersynth = this.endersynth = nil;
		this.startertask.stop;
		this.sustainertask.stop;
		this.endertask.stop;
	}
	
	// get total runtime of current "instance"
	getRuntime {
		^(this.starttime + this.sustime + this.endtime);
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
	
	
	// check if raindrop-synths are there and running, if not, create them
	checkforsynths {
		if(this.sdefs.size == 0) {
			this.bootsynths();
		}
	}
	// create raindrop-synths on the server
	bootsynths {
		this.channels.do{ |n|
			8.do{ |o|
				this.sdefs.add(Synth(\rain, [\out, n, \quantbus, quantbus]).play );
			}
		};
	}
	// remove raindrop-synths from server
	freesynths {
		while({this.sdefs.size > 0}, {
			this.sdefs.pop.free;
		});
	}
	
	// load necessary SynthDefs. Feels strange to do this in a class...
	loadSdefs {
		SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 2, quantbus|
			var snd, trig;
			trig = Dust.ar(In.kr(quantbus));
			snd = BrownNoise.ar() * Decay.ar(trig, 0.5).lag(0.02);
			snd = BPF.ar(snd, LFNoise1.kr(1).range(800, 3300), rq);
		//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
			Out.ar(out, snd*amp);
		}).add;
	}
	
	
}