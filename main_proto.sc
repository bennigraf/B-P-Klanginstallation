

(
var basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";
Routine({
	var runtimemod = 0.05;
	var channels = 2;
	var currentScene, ttl;
	
	var scene = "thewoods";
	("load"+scene).postln;
	currentScene = (basepath++"scenes/"++scene++".sc").load;
	currentScene.init(channels);
	currentScene.bootedUp.wait;
	
	ttl = currentScene.run(nil, runtimemod);
	("run for"+ttl).postln;
	ttl.wait;
	
	"end".postln;
	currentScene.end(nil, runtimemod).wait;
	currentScene = nil;
}).play;
)



Server.default = Server.local
s.options.device = "Digidesign HW ( 003 )";
s.boot

~basepath = "/Users/bennigraf/Documents/Musik/Supercollider/memyselfandi/bp/Brodukt/";


(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[1, 2, 3, 4, 5, 6],
// State 1: Drowny rain
	\drowny, #[2, 3, 4, 5, 6],
// State 2:
	\storm, #[0, 3, 4, 5, 6],
// State 3:
	\thewoods, #[0, 1, 2, 4, 5, 6],
// State 4:
	\erm, #[0, 1, 2, 3, 5, 6],
// State 5:
	\thecave, #[0, 1, 3, 4, 6],
// State 6:
	\833, #[0, 1, 2, 3, 4, 5]
]).asStream;

)

(
~scenes = Dictionary();
~scenes.put(\rain, "rain");
~scenes.put(\storm, "storm");
~scenes.put(\erm, "erm");
~scenes.put(\thewoods, "thewoods");
~scenes.put(\thecave, "thecave");
~scenes.put(\drowny, "drowny");
~scenes.put(\833, "8_33");
) 
(
~runner = Task({
	var runtimemod = 0.05;
	var channels = 2;
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