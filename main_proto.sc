
/*
// testing
(
~w = (~basepath++"./scenes/thewoods.sc").load;
~erm = nil;
Routine({
	var waittime;
	~w.haltSelf;
	~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/"; 
	~w = (~basepath++"scenes/drowny.sc").load;
	~w.init(2);
	~w.bootedUp.wait;
	~w.run(nil, 0.051);
	waittime = ~erm.run(nil, 0.05).postln;
	waittime.wait;
	~w.end(nil, 0.01);
}).play;
);
~erm.buffers
~storm.busses.flash.amp.get()
~storm.busses.brrr.amp.get()
~w.busses.birds.amp.set(0)
~a = Synth(\ding);
// /testing
*/


Server.default = Server.local
s.options.device = "Digidesign HW ( 003 )";
s.boot

~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";

(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0, 1, 2, 3],
// State 1: Drowny rain
	\drowny, 
// State 1:
	\storm, #[0, 2, 3],
// State 2:
	\thewoods, #[0, 1, 3],
// State 3:
	\erm, #[0, 1, 2]
]).asStream;

)

(
~scenes = Dictionary();
~scenes.put(\rain, "rain");
~scenes.put(\storm, "storm");
~scenes.put(\erm, "erm");
~scenes.put(\thewoods, "thewoods");
) 
(
~runner = Task({
	var runtimemod = 0.25;
	var channels = 10;
	var currentScene = nil, lastScene = nil;
	inf.do{
		var state = ~states.next;
		var ttl, lastSceneEnding = 0;
		("Naechste Szene: "++state).postln;
		currentScene = (~basepath++"scenes/"++~scenes[state.asSymbol]++".sc").load;
		currentScene.init(channels);
		currentScene.bootedUp.wait;
		
		if(lastScene.isNil.not) {
			if(currentScene.getStartingTime(runtimemod) < lastScene.getEndingTime(runtimemod)) {
				(lastScene.getEndingTime(runtimemod) - currentScene.getStartingTime(runtimemod)).wait;
			};
		};
		
		ttl = currentScene.run(nil, runtimemod);
		ttl.wait;
		
		lastScene = nil;
		lastScene = currentScene;
		currentScene = nil;
		lastScene.end(nil, runtimemod);
	}
});
~runner.play
)

~runner.stop