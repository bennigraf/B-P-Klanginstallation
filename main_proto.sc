
/*
// testing
(
~proto = (~basepath++"./scenes/proto.sc").load;
~erm = nil;
Routine({
	var waittime;
	~erm.haltSelf;
	~erm = (~basepath++"scenes/erm.sc").load;
	~erm.init(2);
	~erm.bootedUp.wait;
	~erm.run(nil, 0.5);
	waittime = ~erm.run(nil, 0.05).postln;
	waittime.wait;
	~erm.end(nil, 0.5);
}).play;
);
~erm.buffers
~storm.busses.flash.amp.get()
~storm.busses.brrr.amp.get()
// /testing
*/


~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";

(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[0, 1, 2],
// State 1:
	\storm, #[0, 2],
// State 2:
	\erm, #[0, 1]
]).asStream;

)

(
~scenes = Dictionary();
~scenes.put(\rain, "rain");
~scenes.put(\storm, "storm");
~scenes.put(\erm, "erm");
) 
(
~runner = Task({
	var runtimemod = 0.05;
	var channels = 10;
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
~runner.stop
~runner.play
)
