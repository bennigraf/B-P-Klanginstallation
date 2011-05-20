(
SynthDef(\waves, { |out, amp=1, freq=900, rq=1.25|
	var snd, env, trig;
	trig = Dust.ar(0.2);
	snd = BPF.ar(BrownNoise.ar(0.4) * Decay.ar(trig, 3, mul:0.1).lag(1.5), freq, rq);
	snd = PanAz.ar(24, snd, LFNoise2.ar(0.05), 1, 8);
Out.ar(out, snd*amp);
}).add;
)

(
70.do{
Synth(\waves);
}
)

(
SynthDef(\rhodes, { |out, amp=0.4, freq, speakertrig|
		var snd, env;
		snd = SinOsc.ar(freq, mul: 0.2) * SinOsc.ar(16).range(0.8,1);
		env = EnvGen.ar(Env.perc(0.05, 2), doneAction:2);
		snd = snd/2 + GVerb.ar(snd/2, 40, 2, 0.8, earlyreflevel: 0,taillevel:1).sum;
		Out.ar(TIRand.kr(0,8,speakertrig), Pan2.ar(snd*env*amp, SinOsc.kr(4)) );
}).add;
)


(
 Pbind(
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
	\sus, Pseq([1],inf)
	).play(quant:4);
)



(
SynthDef(\bass, { |out, amp=1, freq, trig, sus|
	var snd, env;
	snd = SinOsc.ar(freq) * SinOsc.ar(2).range(0.8,1);
	snd = LPF.ar(snd, 400);
	env = EnvGen.ar(Env.perc(0.04, sus), doneAction:2);
	Out.ar(out, snd*env*amp!10)
	}).add;
	)
	


(
 Pbind(
	\instrument, \bass,
	\scale, Scale.minor,
	\degree, Pseq([Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, \, \, \]),
				Pseq([7, \, 7, \, \, \, \, 4, \, \, 4, \, \, 6, \, \])], inf),
	\mtranspose, -3,
	\octave, 3,
	\dur, Pseq([1/4], inf),
	\sus, Pseq([Pseq([1.5, \, 3, \, \, \, \, 3, \, \, 3.5, \, \, \, \, \]),
			   Pseq([1.5, \, 3, \, \, \, \, 3, \, \, 3.5, \, \, 2, \, \])], inf)

	).play(quant:4);
)
(
SynthDef(\ticketick, { |out, amp=1, trig, echoAmp = 0.5|
		var snd, env;
		env = EnvGen.ar(Env.perc(0.01,0.1));
		snd = PinkNoise.ar()*env;
		snd = RHPF.ar(snd, 1204, 0.12);
		snd = FreeVerb.ar(snd, 0.5, 0.8, 0.3);
		EnvGen.kr(Env.perc(1, 4), doneAction: 2);
		Out.ar(out, snd*amp);
		Out.ar(b, snd * echoAmp * amp);
}).add;
)

b = Bus.audio(s, 1);
c = Buffer.alloc(s,44100*10,1);
(
SynthDef(\tikikikik, { |in, amp = 1|
		var snd, buf, trig;
		snd = In.ar(b);
		snd = snd + BufCombC.ar(c, snd, 1/4, 10);
		snd = RHPF.ar(snd, SinOsc.kr(0.05).range(5000, 10000), 0.1);
		snd = GVerb.ar(snd, 40, 2).sum;
		snd = PanAz.ar(24, snd, LFNoise2.ar(0.1), 1, 4);
		Out.ar(0, snd * amp * SinOsc.kr(0.1).range(0.1,1).poll);
}).add;
)

Synth(\tikikikik, [\in, b]);

(
 Pbind(
	\instrument, \ticketick,
	\amp, Pseq([0, 0.7, 0, 0], inf),
	\dur, 1
	).play(quant:4);
)

