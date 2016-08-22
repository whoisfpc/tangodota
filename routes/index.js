var express = require('express');
var router = express.Router();
var fs = require('fs');
var multer = require('multer');
var spawn = require('child_process').spawn;
var upload = multer({ dest: __dirname+'/../tmp/' }); // for parsing multipart/form-data

/* GET home page. */
router.get('/', function(req, res) {
    res.render('index');
});

router.post('/upload_replay', upload.single('replay_blob'), function (req, res) {

    // var fake_data = __dirname + '/../fake_data/info.json'; 
    // fs.readFile(fake_data, 'utf8',function(err, data) {
    //     if (err) {
    //         console.log(err);
    //     } else {
    //         res.json(JSON.parse(data));
    //     }
    // });
    console.log(req.file);
    var parser = spawn('java', ['-jar', './TangoParser/target/TangoParser.jar'], 
        {
            stdio: ['pipe', 'pipe', 'pipe'],
            encoding: 'utf8'
        });
    parser.stdout.on('data', function(data) {
        res.json(JSON.parse(data));
    });
    parser.stderr.on('data', function(data) {
        console.log(data.toString());
    });
    parser.on('close', function(code) {
        console.log('Child process exited with exit code ' + code);
    });
    fs.readFile(req.file.path, function(err, data) {
        if (err) {
            console.log(err);
        } else {
            parser.stdin.end(data);
        }
    });
    process.on('exit', function(){
        parser.kill(); 
    });
});

module.exports = router;
