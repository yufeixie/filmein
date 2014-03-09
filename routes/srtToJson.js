var mongo_lib = require('./mongo_db_lib');




var makeFilmObject = function(filmdata) {
    console.log(JSON.stringify(filmdata));
    var srtContents = "1\r\n00:01:47,250 --> 00:01:50,500\r\nThis blade has a dark past.\n\r2\r\n00:01:51,800 --> 00:01:55,800\r\nIt has shed much innocent blood.\n\r3\r\n00:01:58,000 --> 00:02:01,450\r\nYou\'re a fool for traveling alone,\r\nso completely unprepared.\n\r4\r\n00:02:01,750 --> 00:02:04,800\r\nYou\'re lucky your blood\'s still flowing.\n\r5\r\n00:02:05,250 --> 00:02:06,300\r\nThank you.\n\r6\r\n00:02:07,500 --> 00:02:09,000\r\nSo...\n\r7\r\n00:02:09,400 --> 00:02:13,800\r\nWhat brings you to\r\nthe land of the gatekeepers?\n\r8\r\n00:02:15,000 --> 00:02:17,500\r\nI\'m searching for someone.\n\r9\r\n00:02:18,000 --> 00:02:22,200\r\nSomeone very dear?\r\nA kindred spirit?\n\r10\r\n00:02:23,400 --> 00:02:25,000\r\nA dragon.\n\r11\r\n00:02:28,850 --> 00:02:31,750\r\nA dangerous quest for a lone hunter.\n\r12\r\n00:02:32,950 --> 00:02:35,870\r\nI\'ve been alone for\r\nas long as I can remember.\n\r13\r\n00:03:27,250 --> 00:03:30,500\r\nWe\'re almost done. Shhh...\n\r14\r\n00:03:30,750 --> 00:03:33,500\r\nHey, sit still.\n\r15\r\n00:03:48,250 --> 00:03:52,250\r\nGood night, Scales.\n\r16\r\n00:04:10,350 --> 00:04:13,850\r\nGet him, Scales! Come on!\n\r17\r\n00:04:25,250 --> 00:04:28,250\r\nScales?\n\r18\r\n00:05:04,000 --> 00:05:07,500\r\nYeah! Come on!\n\r19\r\n00:05:38,750 --> 00:05:42,000\r\nScales!\n\r20\r\n00:07:25,850 --> 00:07:27,500\r\nI have failed.\n\r21\r\n00:07:32,800 --> 00:07:36,500\r\nYou\'ve only failed to see...\n\r22\r\n00:07:37,800 --> 00:07:40,500\r\nThese are dragon lands, Sintel.\n\r23\r\n00:07:40,850 --> 00:07:44,000\r\nYou are closer than you know.\n\r24\r\n00:09:17,600 --> 00:09:19,500\r\nScales!\n\r25\r\n00:10:21,600 --> 00:10:24,000\r\nScales?\n\r26\r\n00:10:26,200 --> 00:10:29,800\r\nScales...\r\n";
    var filmName = filmdata.filmName;
    var imdb_id = filmdata.imdb_id;
    var rating = filmdata.rating;
    var year = filmdata.year;
    var cast1 = filmdata.cast1;
    var cast2 = filmdata.cast2;
    var cast3 = filmdata.cast3;
    var pic_url = filmdata.pic_url;
    var genre = filmdata.genre;
    var time = filmdata.time;
    var desc = filmdata.desc;
    var subtitles = filmdata.subtitles;

    subtitleData = srtToJson(subtitles);
    var film = {
        "name": filmName.toLowerCase(),
        "imdb_id":imdb_id,
        "info": {
            "rating":rating,
            "year":year,
            "cast": [cast1, cast2, cast3],
            "pic_url":pic_url
        },
        "trivia":{
            genre:genre,
            time: time,
            text: desc
        },
        "subtitles":subtitleData
        };

        return film;

};

var srtToJson = function (srt) {
    var subtitleData = [];

    subGroups = srt.split("\n\r");
    for(var i = 0; i < subGroups.length; i++) {
        parts = subGroups[i].split("\r\n");
        
        count = parts[0];
        
        timings = parts[1].split(" ");
        startTime = srtTimeToMilli(timings[0]);
        endTime = srtTimeToMilli(timings[2]);
        
        var text = "";
        for(var j = 2; j < parts.length; j++) {
            text += parts[j];
            if(j != parts.length - 1) {
                text += "\r\n";
            }
        }
        
        var subtitleGroup = {
            "count":count,
            "start":startTime,
            "endTime":endTime,
            "text":text
        };
        
        subtitleData.push(subtitleGroup);
    }

    return subtitleData;
};

//00:01:02,125
//hr:min:sec,ms
var srtTimeToMilli = function (time) {
    var parts = time.split(":");
    var subParts = parts[2].split(",");
    var hours = parts[0];
    var mins = parts[1];
    var secs = subParts[0];
    var ms = subParts[1];

    var totalTime = 0;
    totalTime += hours * 3600000;
    totalTime += mins * 60000;
    totalTime += secs * 1000;
    totalTime += ms * 1;

    return totalTime;
};


exports.makeFilmObject = makeFilmObject;