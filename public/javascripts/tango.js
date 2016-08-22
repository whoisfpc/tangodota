var app = angular.module('tango', ['ui.router']);

app.filter('convertTime', function() {
    return function(input) {
        var mins = Math.floor(input / 60);
        mins = mins < 10 ? '0'+mins : mins;
        var secs = input % 60;
        secs = secs < 10 ? '0'+secs : secs;
        return mins + ':' + secs;
    }
});

app.directive("fileread", [function () {
    return {
        scope: {
            fileread: "="
        },
        link: function (scope, element, attributes) {
            element.bind("change", function (changeEvent) {
                scope.$apply(function () {
                    scope.fileread = changeEvent.target.files[0];
                    // or all selected files:
                    // scope.fileread = changeEvent.target.files;
                });
            });
        }
    }
}]);

app.controller('ReplayCtrl', ['$scope', '$filter', 'replayParser', function($scope, $filter, replayParser) {
    //TODO 如何初始化？
    $scope.replayInfo = replayParser.replayInfo;
    $scope.players = {
        team1: $scope.replayInfo.players.slice(0,5),
        team2: $scope.replayInfo.players.slice(5,10)
    };

    // generate heatmap
    var heatmap = h337.create({
        container: document.getElementById('heatmap-container')
        // radius: 30,
        // maxOpacity: .5,
        // minOpacity: 0,
        // blur: .75
    });

    // convert position coordinate to image coordinate
    var positions = replayParser.replayInfo.positions;
    var convertPos = [];
    var worldCoord, imgCoord;
    var w = $('#heatmap-container').width();
    var h = $('#heatmap-container').height();
    positions.forEach(function(element, i) {
        convertPos.push([]);
        positions[i].forEach(function(pos) {
            worldCoord = coordFromCell(pos.x, pos.y);
            imgCoord = imgCoordFromWorldNorm(worldCoord[0], worldCoord[1]);
            convertPos[i].push({x:imgCoord[0]*w, y:imgCoord[1]*h, value: 1});
        }, this);
    }, this);

    $scope.selectPos = convertPos[0];

    // generate slider
    var mySlider = $('#slider-container').slider({
        id: 'dt-slider',
        range: true,
        min: 0,
        max: $scope.selectPos.length,
        value: [ 0, $scope.selectPos.length ],
        tooltip: "always",
        tooltip_position:'bottom',
        formatter: function(value) {
            return $filter('convertTime')(value[0])+'/'+$filter('convertTime')(value[1]);
        }
    });

    mySlider.on('slide', function(e) {
	    showHeatmap($scope.selectPos.slice(e.value[0], e.value[1]));
    });

    $scope.switchPlayer = function(idx) {
        console.log('switch to '+ idx);
        $scope.selectPos = convertPos[idx];
        $('.hero-select').removeClass('hero-select');
        $('.avatar').eq(idx).addClass('hero-select');
        mySlider.slider('setAttribute', 'max', $scope.selectPos.length);
        var s = mySlider.slider('getValue')[0];
        var t = mySlider.slider('getValue')[1];
        showHeatmap($scope.selectPos.slice(s, t))
    }

    function showHeatmap(posPart) {
        heatmap.setData({
            max: 1, // it is okay because max will auto set when value larger than it
            min: 0,
           data: posPart
        });
    }

    function coordFromCell(x, y) {
        // cell has been shifted to (0,0)(left corner) in jave parser
        var offset = 128 * 64;
        // convert postion to world coord, (0,0) is at center of map
        return [(x*128) - offset, (y*128) - offset];
    }

    function imgCoordFromWorldNorm(x, y) {
        return [((8576.0 + x) * 0.0634 + -30.4377)/1024, ((8192.0 - y) * 0.0646 + -40.7889)/1024];
    }
}]);

app.controller('UploadCtrl', [
'$scope', '$http', '$state', 'replayParser',
function($scope, $http, $state, replayParser) {
    $scope.upload = function() {
        if ($scope.matchId) {

        } else {
            replayParser.getReplayInfoByFile($scope.replayFile).then(function(){
                $state.go('parser');
            });
        }
    }
}]);

app.controller('BlankCtrl', ['$scope', 'replayParser',
function($scope, replayParser) {
        $scope.upinfo = replayParser;
}]);

app.factory('replayParser', ['$http', function($http) {
    var o = {
        replayInfo: {},
        onUpload: false,
        uploadedPercent: 0,
    };
    // TODO
    o.getReplayInfoByFile = function(replayFile) {
        var formData = new FormData();
        formData.append('replay_blob', replayFile, replayFile.name);
        var postParams = {
            method: 'POST',
            url: '/upload_replay',
            headers: {
                //it must be undefined so boundary will be added fellow "multipart/form-data"
                'Content-Type': undefined
            },
            data: formData,
            uploadEventHandlers: {
                loadstart: function(e) {
                    o.onUpload = true;
                },
                progress: function(e) {
                    console.log('upload: '+ e.loaded);
                    o.uploadedPercent = e.loaded / e.total;
                },
                loadend: function(e) {
                    //o.onUpload = false;
                }
            }
        };
        return $http(postParams).then(function(data){
            o.replayInfo = data.data;
            o.onUpload = false;
        });
    };
    return o;
}]);

app.config([
'$stateProvider',
'$urlRouterProvider',
function($stateProvider, $urlRouterProvider) {

  $stateProvider
    .state('home', {
      url: '/home',
      templateUrl: '/template/blank.html',
      controller: 'BlankCtrl'
    })
    .state('parser', {
      url: '/parser',
      templateUrl: '/template/parser.html',
      controller: 'ReplayCtrl',
      resolve: {
        
      }
    })
  $urlRouterProvider.otherwise('home');
}]);