

(
SynthDef(\wood2, { |out, amp, freq|
	var snd, env, synth, trig;
	trig = Impulse.kr(1/8) + Impulse.kr(1/8, 7/8);
	synth = SinOsc.ar(TRand.kr(250, 950, trig) * [1, 1.6]).sum/2;
	env = EnvGen.ar(Env.perc(0.0001, 0.3), trig);
	snd = BPF.ar(synth*env, 1200, 10.reciprocal);
	snd = GVerb.ar(snd, 30, 5, 0.7).sum;
//	DetectSilence.ar(snd, doneAction:2);
	Out.ar(2.rand, snd*amp);
}).add;

)
x.free
x = Synth(\wood2, [\amp, 1]);
s.scope
Synth(\wood2, [\amp, 1, \freq,950])   
Synth(\wood2, [\amp, 1, \freq,450])   
Synth(\wood2, [\amp, 1, \freq,250])   




(
SynthDef(\leaves, { |out, amp|
	var snd, env, trig;
	trig = Dust.ar(1/5);
	snd = BPF.ar(BrownNoise.ar(1) * Decay.ar(trig, 18, mul:0.051).lag(2.5), 3200 * LFNoise1.kr(1/5).range(0.7, 1.1), 0.8.reciprocal);
	snd = PanAz.ar(24, snd, LFNoise2.ar(0.05), 1, 8);	
Out.ar(out, snd*amp);
}).add;
)

(
20.do{
Synth(\leaves, [\amp, 0.8, \freq, 3200, \rq, 0.8.reciprocal]);
}
)

/*
(
SynthDef(\wood, { |out, amp, modfreq, modindex, freq|
	var snd, env, synth;
	synth = Klang.ar(`[{exprand(300,500)}!65, nil, nil],1,0) * 0.1;
	env = EnvGen.ar(Env.perc(0.001, 0.3));
	snd = GVerb.ar(synth*env, 90, 7, 0.5);
	Out.ar(Rand(0,23), snd*amp);
}).add;

)
Synth(\wood, [\amp, 0.3])

*/
