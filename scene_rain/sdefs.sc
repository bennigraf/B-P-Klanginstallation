SynthDef(\rain, { |out = 0, amp = 1, freq = 1200, rq = 2, quantbus|
	var snd, trig;
	trig = Dust.ar(In.kr(quantbus));
	snd = BrownNoise.ar() * Decay.ar(trig, 0.4).lag(0.02);
	snd = BPF.ar(snd, LFNoise1.kr(1).range(800, 3300), rq);
//	snd = GVerb.ar(snd, 20, 0.3, 0.8);
	Out.ar(out, snd*amp);
}).add;
