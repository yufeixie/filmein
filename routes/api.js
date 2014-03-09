

var mongo_lib = require('./mongo_db_lib');
var srtToJson = require('./srtToJson');

exports.getFilm = function (req, res) {
	var name = req.params.name;
	console.log(req.params);
	mongo_lib.getFilm(name, function (err, result) {
		if (err) {
			console.log(JSON.stringify(err, null, 4));
			res.send(401);
			return;
		} else {
			res.json(result);
		}
	});
};

exports.putFilm = function (req, res) {
	//var film = req.body.film;
	var film = srtToJson.makeFilmObject(req.body);
	console.log("film created: " + JSON.stringify(film));
	mongo_lib.putFilm(film, function (err, result) {
		if (err) {
			console.log(JSON.stringify(err, null, 4));
			res.send(401);
			return;
		} else {
			res.json(result);
		}
	});
};

exports.getSubtitles = function (req, res) {
	var name = req.body.name;
	mongo_lib.getFilm(name, function (err, result) {
		res.json({subtitles: result.subtitles});
	});
};

