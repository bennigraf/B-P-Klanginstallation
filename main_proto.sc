
/*
// testing
(
~proto = (~basepath++"./scenes/proto.sc").load;
~storm = nil;
Routine({
	var waittime;
	~storm = (~basepath++"scenes/storm.sc").load;
	~storm.init(2);
	~storm.bootedUp.wait;
	waittime = ~storm.run(nil, 0.05).postln;
	waittime.wait;
	~storm.end(nil, 0.05);
}).play;
);
~storm.state
~storm.busses.flash.amp.get()
~storm.busses.brrr.amp.get()
// /testing
*/


~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";

(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0, 1],
// State 1:
	\storm, #[0],
]).asStream;

)

(
~scenes = Dictionary();
~scenes.put(\rain, "rain");
~scenes.put(\storm, "storm");
) 
(
~runner = Task({
	var runtimemod = 0.05;
	var channels = 2;
	var currentScene = nil, lastScene = nil;
	inf.do{
		var state = ~states.next;
		var ttl, lastSceneEnding = 0;
		
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
