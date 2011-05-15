


/////////// TESTING STUFF HERE!!! /////////////


ServerOptions.devices;
s.options.device = "8chan-Setup";
s.options.numOutputBusChannels;
s.quit;
s.boot;


(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0],
// State 1:
//	\rain, #[0,3],
]).asStream;

)
~states.next

(
~scenes = Dictionary();
~scenes.put(\rain, Rain());
)
(
~runner = Task({
	inf.do{
		var state = ~states.next;
		state.postln;
		~scenes[state.asSymbol].run(runtime: 1);
		("Waiting "++~scenes[state.asSymbol].getRuntime).postln;
		~scenes[state.asSymbol].getRuntime.wait;
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



~storm = Storm(2);

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
SynthDef(\brrr, { |out=0, upperlimit = 400, amp = 0.5|
	var snd = BrownNoise.ar();
	snd = snd * Decay2.ar({Dust.ar(LFNoise0.kr(13).range(1, 100))}!2, 0.05, 0.5);
	snd = RLPF.ar(snd, LFNoise0.kr(13).range(100, upperlimit), 0.8);		// note: RLPF takes q as 3rd arg
	snd = Compander.ar(snd, snd, 0.7, 1, 1/3, 10, 10);		// In, Ctrl, Thresh, Below, Above, Attack, Release
	snd = snd.softclip * 0.4;
	FreeVerb.ar(snd, 0.45, 8, 0.4);		// Mix, Room, Damp
	Out.ar(0, snd * amp);
//	Out.ar(~revbus, snd * 0.5 * ~bus[30].kr);
}).add;
)
~brr = Synth(\brrr);


~a = ~b = List()
~a.size
~b
~a.add(~a.add(3))

~a.do{ |n|
	n.postln;
}