


/////////// TESTING STUFF HERE!!! /////////////
Server.default.options.numOutputBusChannels_(24)


ServerOptions.devices;
s.options.device = "8chan-Setup";
s.options.numOutputBusChannels;
s.quit;
s.boot;


(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0, 1, 1],
// State 1:
	\storm, #[0],
]).asStream;

)
~states.next

(
~scenes = Dictionary();
~scenes.put(\rain, Rain());
~scenes.put(\storm, Storm());
)
(
~runner = Task({
	inf.do{
		var waittime;
		var state = ~states.next;
		state.postln;
		waittime = ~scenes[state.asSymbol].run(runtimemod: 0.1);
		("Waiting "++waittime).postln;
		waittime.wait;
	}
});
~runner.play
)
~runner.stop;

~scenes[\rain].debug("on")
~scenes[\rain].end()

Task{ inf.do{ a.next.postln; 1.wait; } }
~channels = 2;


~rain = Rain(2)

~rain.debug("on")
~rain.debug("off")
~rain.run(0.01);
~rain.start(10)
~rain.end(3);



~storm = Storm(8);

~storm.run(0.1)


~test = false
~test.isBool
nil.if



~test = List();
~test.size == 0

~test = Task({
	100.do{|n|
		n.postln;
		1.wait;
	};
}).play;

~a = ~b = 13
~b


//////////////// Testing Storm-brrr ///////////////////
(
		SynthDef(\brrr, { |out=0, upperlimit = 200, amp = 0.5, flashness = 0.1|
			var snd = BrownNoise.ar();
			snd = snd * Decay2.ar(Dust.ar(LFNoise0.kr(13).range(1, 100)), 0.05, 0.5);
			snd = RLPF.ar(snd, LFNoise0.kr(13).range(100, upperlimit), 0.8);
			snd = Compander.ar(snd, snd, 0.7, 1, 1/3, 10, 10) * 0.3;				// In, Ctrl, Thresh, Below, Above, Attack, Release
			snd = snd.softclip * 0.4;
			FreeVerb.ar(snd, 0.45, 8, 0.4);		// Mix, Room, Damp
			Out.ar(out, snd * amp);
		//	Out.ar(~revbus, snd * 0.5 * ~bus[30].kr);
		}).add; 
		SynthDef(\flash, { |out = 0, amp = 0.5|
			var snd, flashes;
			var flashTrig = Dust.kr(flashness);
			var flashDecay = Decay2.kr(flashTrig, 0.08, 0.95);
			var flashshsh = Latch.kr(WhiteNoise.kr, flashTrig).range(7000, 13000);
			snd = snd * Decay2.ar(Dust.ar(LFNoise0.kr(13).range(1, 100)), 0.05, 0.5);
			flashes = RLPF.ar(snd, flashDecay.linlin(0, 1, 1000, flashshsh),  // freq
					flashDecay.linlin(0,1,0.1,2.8), // rq
					mul: Decay2.kr(flashTrig, 0.05, 1) * 2 // envelope
				);
			flashes = flashes.softclip;
			Out.ar(out, flashes * amp);
		});
		)
		
~brr.free
~brr = Synth(\brrr);
~brr.set(\flashness, 0.5)

~a = ~b = List()
~a.size
~b
~a.add(~a.add(3))

~a.do{ |n|
	n.postln;
}

///////////////////////
proto.vol = ();
proto.vol.global = 1;
proto.vol.voices = ();
proto.vol.set = { |self|
	self.vol.voices.do { |voice|
		
	};
};
proto.vol.addVoice{ |self, name, vol| 
	self.vol.voices.put(name.asSymbol, vol);
};


~vol = ();
~vol.voices = ();
~vol.voices.put(\testvol3, 8)

~vol.voices.do { |voice|
	voice.postln;
}

~test = false
t = Task({
	{~test}.while{
		1.wait;
		"bla".postln;
	}
}).play;
t
t.shit

~buff = Buffer.alloc(s, 44100)
~buff



//////////////// Testing the woods!

(
var self = ();
self.channels = 2;
self.vol = ();
self.vol.dings = 1;
SynthDef(\ding, { |amp = 0, freq, freq2, combBuf|
	var snd,snd2, combSnd, combTime, env, synth, synth2;
	synth = VarSaw.ar([freq, freq*3.6]).sum;
	synth2 = VarSaw.ar([freq2, freq2*1.6]).sum;
	env = EnvGen.ar(Env.perc(0.01, 8));
	snd = BPF.ar(synth*env, 1200, 10.reciprocal);
	snd2 = BPF.ar(synth2*env, 9000, 10.reciprocal);
	snd = snd + snd2;
	snd = GVerb.ar(snd+snd2, 160, 50, 0.8).sum;
	snd = Compander.ar(snd, snd, 0.5, 1, 1/6, 0.041, 20) * 2;
	combTime = Rand(0.5, 1.5);
/*		combSnd = BufCombL.ar(combBuf, snd, combTime, 10);*/
	snd = Compander.ar(snd, snd, 0.5, 1, 1/6, 0.041, 20) * 2;
	DetectSilence.ar(snd, 0.1, 5, doneAction:2);
/*		Out.ar(Latch.kr(WhiteNoise.kr, Impulse.kr(1/combTime)).range(0, self.channels-1).round.poll, combSnd * amp * self.vol.dings);*/
	Out.ar(Rand(0,self.channels-1), snd*amp * self.vol.dings);
}).add;
)
c = Synth(\ding, [\amp, 1]);



///////////////// Testing silence

b = Buffer.alloc(s, 3 * s.sampleRate)
(
SynthDef(\del, { |in = 0, out = 0, delay = 1, buf, amp = 1|
	var snd = BufDelayN.ar(buf, SoundIn.ar(in), delay);
	Out.ar(out, snd * amp * 1);
}).play(s, [\in, 0, \out, 0, \delay, 1, \buf, b]);
)

/////////////////////

Buffer.cachedBuffersDo(s, { |buf| buf.free });
Buffer.cachedBuffersDo(s, { |buf| buf.postln });
Server


~tmpBus = Bus.audio(s, 10);

~tmpS = { 
	var snd = ~tmpBus.ar.sum;
	Out.ar(0, snd!2) }.play
~tmpS.free

(
SynthDef(\bass, { |amp=1, freq, trig, sus, bassAmp|
	var snd, env;
	env = EnvGen.ar(Env.perc(0.04, sus), doneAction:2) + EnvGen.kr(Env.perc(0.02, 0.1));
	snd = SinOsc.ar(freq) * SinOsc.ar(2).range(0.8,1) * env;
	snd = snd.round(0.5**6);
	snd = snd.clip2(MouseY.kr(2, 0.2));
	snd = RLPF.ar(snd, [188, 155], 0.1).sum/2;
	snd = (snd*0.26).softclip;
	Out.ar(0, snd*amp!2)
}).add;
)
~p.stop
(
~p = Pbind(
	\instrument, \bass,
	\scale, Scale.minor,
	\degree, Pseq([Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, \, \, \]),
				Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, 6, \, \])], inf),
	\mtranspose, -3,
	\octave, 3,
	\dur, Pseq([1/4], inf),
	\sus, Pseq([Pseq([1.5, \, 3, \, \, \, \, 1.5, \, \, 3.5, \, \, \, \, \]),
			   Pseq([1.5, \, 3, \, \, \, \, 1.5, \, \, 1.2, \, \, 2, \, \])], inf)
).play(quant:4);
)