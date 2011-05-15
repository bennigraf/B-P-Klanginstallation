


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
