

(
SynthDef(\wood2, { |out, amp, freq|
	var snd, env, synth;
	synth = SinOsc.ar([freq, freq*1.6]).sum;
	env = EnvGen.ar(Env.perc(0.0001, 0.3));
	snd = BPF.ar(synth*env, 1200, 10.reciprocal);
	snd = GVerb.ar(snd, 30, 5, 0.7);
	DetectSilence.ar(snd, doneAction:2);
	Out.ar(Rand(0,23), snd*amp);
}).add;

)

s.scope
Synth(\wood2, [\amp, 1, \freq,950])   
Synth(\wood2, [\amp, 1, \freq,450])   
Synth(\wood2, [\amp, 1, \freq,250])   




(
SynthDef(\leaves, { |out, amp, freq, rq|
	var snd, env, trig;
	trig = Dust.ar(2);
	snd = {BPF.ar(BrownNoise.ar(0.4) * Decay.ar(trig, 3, mul:0.1).lag(1.5), freq, rq)};
	snd = PanAz.ar(24, snd, LFNoise2.ar(0.05), 1, 8);	
Out.ar(out, snd*amp);
}).add;
)

(
80.do{
Synth(\leaves, [\amp, 0.4, \freq, 3200, \rq, 0.8.reciprocal]);
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
