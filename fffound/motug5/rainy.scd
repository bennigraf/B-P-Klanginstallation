Server.internal.options.numOutputBusChannels_(24)



(
SynthDef(\lightning, { |out, amp, trig, cutoff|
	var snd, env, filter;
	snd = (LFNoise0.ar(Decay.kr(trig, 0.4, mul: 8000, add: 4200))!4).sum;
	filter = LPF.ar(snd, cutoff);
	env = EnvGen.ar(Env.perc(0.01,0.8,1,-2), trig);
	snd = GVerb.ar(filter * env, 60, 2, 0.8);
	Out.ar(0, snd*amp);	
}).add;
)

b = Synth(\lightning, [\amp, 0.2, \trig, 1, \cutoff, 4000]);
b.set(
b.free



(
SynthDef(\storm, { |out, amp|
	var snd, sig, arr, rate, filter;
	rate = 0.01;
	sig = Signal.newClear(1378);
	sig.waveFill({arg x, i; bilinrand(x) },1, 3).asArray;	arr = Buffer.loadCollection(s,sig); 
	snd = LFNoise1.ar(3000 + (PlayBuf.kr(1, arr, (rate), loop:0) * 200));
	snd = LPF.ar(snd, 6000);
	Out.ar(out, snd*amp!24);
}).add;
)

d = Synth(\storm, [\amp, 0.01])
d.set(\amp, 0.005)
d.free


	
	
(
SynthDef(\drone, { |out, amp| 
	var snd, env;
	snd = Klang.ar(`[ {exprand(80, 500)}!12, nil, nil],1,0);
	snd = RLPF.ar(snd, SinOsc.kr(0.02, add: 400, mul:250), 4.reciprocal);
	snd = snd * (1 - SinOsc.ar(LFNoise1.ar(0.02).range(2, 8), mul: LFNoise1.ar(0.05).range(0.01, 0.3), add: 0.5));
	snd = PanAz.ar(24, snd, LFNoise2.ar(0.05), 1, 3);
	Out.ar(out, snd*amp);
	}).add;
	)
	
	
c = Synth(\drone, [\amp, 0.01])	
c.set(\amp, 0.055)
c.free



(
SynthDef(\rain, { |out, amp, freq, rq|
	var snd, env, trig;
	trig = Dust.ar(0.5);
	snd = {BPF.ar(BrownNoise.ar(0.4) * Decay.ar(trig, 0.5).lag(0.005), freq, rq)};
//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
	Out.ar((Latch.ar(WhiteNoise.ar, trig)*24).round, snd*amp);
}).add;
)

100.do{
Synth(\rain, [\amp, 1, \freq, 1200, \rq, 0.5.reciprocal]);
}

a.set(\amp, 0.2)
a.set(\rq, 0.5.reciprocal)
a.free
s.scope

(
SynthDef(\ding, { |out, amp, freq, freq2|
	var snd,snd2, env, synth, synth2;
	synth = VarSaw.ar([freq, freq*3.6]).sum;
	synth2 = VarSaw.ar([freq2, freq2*1.6]).sum;
	env = EnvGen.ar(Env.perc(0.01, 8), doneAction:2);
	snd = BPF.ar(synth*env, 1200, 10.reciprocal);
	snd2 = BPF.ar(synth2*env, 9000, 10.reciprocal);
	snd = snd + snd2;
	snd = GVerb.ar(snd+snd2, 160, 50, 0.8).sum;
	snd = Compander.ar(snd, snd, 0.5, 1, 1/6, 0.041, 20) * 2;
	DetectSilence.ar(snd, doneAction:2);
	Out.ar(Rand(0,23), snd*amp);
}).add;



)

~revbus.index
~revbus = Bus.audio(s, 1)

(
SynthDef(\reverb, { |out = ([0,1])|
	var rev;
	rev = GVerb.ar(~revbus.ar, 60, 15, 0.8);
	Out.ar(out, rev);
}).add;
)


Synth(\reverb)
//a = Synth(\ding, [\amp, 1, \freq,400, \freq2,500])  
 

s.scope
(
p = Pbind(
	\instrument, \ding,
	\freq, Pxrand([200,400,600,800,1000,1600,2000,],inf),
	\freq2, Pxrand([300,500,700,900,1100,1300,1900,],inf),
	\dur, Pwhite(8, 32),
	\amp, Prand([0.2,0.4,0.6,0.9], inf),
	\octave, 4
	).play
	
)
p.free
s.scope
	
a.free















