Server.default = Server.local
s.options.numOutputBusChannels = 10
s.options.memSize = 2 ** 17
s.boot


(
var basepath = "/Users/student/Desktop/Klanginstallation graf-kleinknecht/B-P-Klanginstallation/";

Routine({
	var runtimemod = 1;
	var channels = 8;
	var currentScene, lastScene, ttl;
	var scene;
	
	// drowny!
	scene = "drowny";
	("load"+scene).postln;
	currentScene = (basepath++"scenes/"++scene++".sc").load;
	currentScene.init(channels);
	currentScene.bootedUp.wait;
	currentScene.runtime = 45;
	currentScene.starttime = 2/8;  // run for 39,3 seconds + xx sec fade out
	currentScene.sustime = 5/8;
	currentScene.endtime = 1/8;
	
	ttl = currentScene.run(nil, runtimemod);
	("run for"+ttl).postln;
	lastScene = currentScene;
	ttl.wait;
	
	currentScene.end(nil, runtimemod).wait; // fade out drowny
	
	
	// thewoods
	scene = "thewoods";
	("load"+scene).postln;
	currentScene = (basepath++"scenes/"++scene++".sc").load;
	currentScene.init(channels);
	currentScene.bootedUp.wait;
	currentScene.runtime = 30;
	currentScene.starttime = 0.5/4;
	currentScene.sustime = 2.5/4;
	currentScene.endtime = 1.5/4;
	
	ttl = currentScene.run(nil, runtimemod);
	("run for"+ttl).postln;
	ttl.wait;
	
	currentScene.end(nil, runtimemod).wait; // fade out thewoods

	
	scene = "tagammeer";
	("load"+scene).postln;
	currentScene = (basepath++"scenes/"++scene++".sc").load;
	currentScene.init(channels);
	currentScene.bootedUp.wait;
	currentScene.runtime = 50;
	currentScene.starttime = 1.5/5;
	currentScene.sustime = 2.5/5;
	currentScene.endtime = 1/5;
	
	ttl = currentScene.run(nil, runtimemod);
	("run for"+ttl).postln;
	ttl.wait;
	
	currentScene.end(nil, runtimemod).wait; // fade out tagammeer
	
	
	scene = "thecave";
	("load"+scene).postln;
	currentScene = (basepath++"scenes/"++scene++".sc").load;
	currentScene.init(channels);
	currentScene.bootedUp.wait;
	currentScene.runtime = 40;
	currentScene.starttime = 1/4;
	currentScene.sustime = 2.5/4;
	currentScene.endtime = 1/4;
	
	ttl = currentScene.run(nil, runtimemod);
	("run for"+ttl).postln;
	ttl.wait;
	
	currentScene.end(nil, runtimemod).wait; // fade out thecave

	
	
	
}).play;
)
