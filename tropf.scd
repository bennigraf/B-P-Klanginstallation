(
SynthDef(\tock, { |out, amp=1, freq=140, mfreq=170|
	var snd, trig, env, etrig;
	etrig = Impulse.kr(0.01);
	trig = Impulse.kr(0.08);
	snd = SinOsc.ar([freq, freq*1.6]).sum * EnvGen.ar(Env.perc(0.001, 0.5), trig);
	snd = BPF.ar(snd, TRand.kr(160, 250, etrig), 8.reciprocal);
	snd = GVerb.ar(snd, 40, 5, 0.8);
	env = EnvGen.ar(Env.new([0.001,1,1,0.001], [60,20,75], 'exponential'), etrig);
	Out.ar((Latch.ar(WhiteNoise.ar, etrig)*24), snd*amp*env);
}).add;
)

Synth(\tock)





(

SynthDef(\drops, { |out, amp, freq, rate|
	var trig = Dust.ar(0.3, mul:0.7);
	var snd = SinOsc.ar(freq * Decay.ar(trig, 1/8).linlin(0, 1, 5, 0)) ;
	snd = snd * Decay.ar(trig, 0.1, 0.1).lag(0.05);
	snd = GVerb.ar(snd, 60, 4, 0.8);
	Out.ar(out, snd);
}).add;
)

Synth(\drops, [\out, 15, \amp,0.6, \freq, 400, \rate, 0.3] )
Synth(\drops, [\out, 19,\amp,0.6, \freq, 100, \rate, 0.2] )
Synth(\drops, [\out, 9, \amp, 0.4, \freq, 200, \rate, 0.2] )
Synth(\drops, [\out, 12, \amp, 0.4, \freq, 230, \rate, 0.2] )



(
SynthDef(\stream, { |out, amp=0.0002|
	var snd, freq, trig;
	trig = Dust.kr(150);
	freq = TExpRand.kr(400, 1500, trig) + LFNoise2.kr(20, mul: 100);
	snd = (SinOsc.ar(freq));
	snd = GVerb.ar(snd, 40, 0.5, 0.8);
	Out.ar(out, snd*amp!24)
}).add;
)

Synth(\stream);
	

(
SynthDef(\insect, { |out,amp=0.01,rel| 
	var env,noise1,noise2,snd,trig;
	trig = Dust.kr(0.04);
	noise1 = BPF.ar(WhiteNoise.ar(), LFNoise2.kr(2, 800, TRand.kr(2000, 5000, trig)),0.2);
	noise2 = BPF.ar(WhiteNoise.ar(), LFNoise2.kr(2, 800, TRand.kr(2000, 5000, trig)),0.15);
	snd = (noise1 + noise2) * SinOsc.kr(TRand.kr(4, 15, trig));
	env = EnvGen.ar(Env.linen(TRand.kr(0.1, 0.4, trig),TRand.kr(0.3, 1.5, trig),0.1,0.6), trig) * TRand.kr(0.05, 0.3, trig);
	snd = GVerb.ar(snd*env, 40, 8, 0.5);
	Out.ar(TRand.kr(0, 23, trig), snd)
}).add;
)



Synth(\insect)


s.scope

