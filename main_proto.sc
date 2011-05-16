
/*
// testing
~proto = (~basepath++"./scenes/proto.sc").load;
(
~proto = (~basepath++"./scenes/proto.sc").load;
~rain = nil;
Routine({
	var waittime;
	~rain = (~basepath++"scenes/rain.sc").load;
	~rain.init();
	~rain.bootedUp.wait;
	waittime = ~rain.run(nil, 0.05).postln;
	waittime.wait;
	~rain.end(nil, 0.05);
}).play;
);
~rain.state
~rain.busses.intensity.get()
~rain.busses.amp.get()
// /testing
*/


~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";

(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0],
// State 1:
/*	\storm, #[0],*/
]).asStream;

)

(
~scenes = Dictionary();
~scenes.put(\rain, "rain");
/*~scenes.put(\storm, "storm");*/
) 
(
~runner = Task({
	var runtimemod = 0.1;
	var currentScene = nil, lastScene = nil;
	inf.do{
		var state = ~states.next;
		var ttl, lastSceneEnding = 0;
		
		currentScene = (~basepath++"scenes/"++~scenes[state.asSymbol]++".sc").load;
		currentScene.init(8);
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
