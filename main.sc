


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
~rain.getBusvalue.postln

~rain.debug("on")
~rain.debug("off")
~rain.run(3, 5, 13);
~rain.start(10)
~rain.end(3);


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