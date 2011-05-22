
(
~states = Pfsm([
	#[0],		// initial state
// State 0
	\833, #[0]
]).asStream;

)
(
~states = Pfsm([
	#[0],		// initial state
// State 0
	\tagammeer, #[0]
]).asStream;

)

~states.next




(
var basepath = "/Users/bgraf/Desktop/B-P-Klanginstallation/";
Routine({
	var runtimemod = 0.3;
	var channels = 10;
	var currentScene, ttl;
	
	var scene = "tagammeer";
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



s.options.numControlBusChannels
s.options.numAudioBusChannels
ServerOptions.inDevices
s.options.device


Server.default = Server.local
s.options.outDevice = "Digidesign HW ( 003 )";
s.options.inDevice = "Built-in Microphone";
s.options.numOutputBusChannels = 10
s.options.memSize = 2 ** 17
s.quit
s.boot
s.volume.volume = -17
s.volume.gui


~basepath = "/Users/bgraf/Desktop/B-P-Klanginstallation/";


(
~states = Pfsm([
	#[0],		// initial state
// State 0: Rain
	\rain, #[1, 2, 3, 4, 5, 6],
// State 1: Drowny rain
	\drowny, #[2, 3, 4, 5, 6],
// State 2: Storm
	\storm, #[0, 3, 4, 5, 6],
// State 3: Thewoods
	\thewoods, #[0, 1, 2, 4, 5, 7],
// State 4: Aehm...
	\erm, #[0, 1, 2, 3, 5, 7],
// State 5: The Cave
	\thecave, #[0, 1, 3, 4],
// State 6: 8_33
	\833, #[0, 1, 2, 3, 4, 5, 7],
// State 7: Tag am Meer
	\tagammeer, #[0, 1, 2, 5, 1, 0]
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
~scenes.put(\tagammeer, "tagammeer");
) 
(
~runner = Task({
	var runtimemod = 0.7;
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



Task({
	var vol = -17;
	1000.do{ |n|
		s.volume.volume = vol;
		vol = vol - 0.2;
		0.3.wait;
	}
}).play