/**
 * User:    Josh Adam <josh.adam@phac-aspc.gc.ca>
 * Date:    2013-06-04
 * Time:    1:05 PM
 * License: MIT
 */ ( function ( ng, app ) {
  'use strict';
  app.controller( 'ProjectCtrl', [ '$scope', '$rootScope', 'ajaxService', '$location',
    function ( $scope, $rootScope, ajaxService, $location ) {
      $scope.sample = {};
      $scope.samples = {};
      $scope.list2 = [ ];

      $scope.addFileToSample = function ( evt, ui, url ) {
        $scope.dragOverOut(evt);
        ajaxService.create( url, {
          'sequenceFileId': $scope.list2[ 0 ].identifier
        } ).then( function ( data ) {
          console.log( data );
          $scope.list2 = [ ];
        } );
      };

      $scope.addFileToDetailSample = function () {
        ajaxService.create( $scope.sample.addUrl, {
          'sequenceFileId': $scope.list2[ 0 ].identifier
        } ).then( function ( data ) {
            console.log( data );
            $scope.list2 = [ ];
          } );
      };

      $scope.getSequenceFiles = function (sample) {
        console.log('getting sequence files');
        // TODO: (Josh: 2013-06-18) Add loading spinner!
        $scope.sample.addUrl = sample.links['sample/sequenceFiles'];
        $scope.sample.sequenceFiles = [];
        $scope.sample.details = true;
        
        ajaxService.get(sample.links['sample/sequenceFiles']).then(function (data) {
          console.log(data);
          $scope.sample.sequenceFiles = data.resource.resources;
          console.log($scope.sample.sequenceFiles);
        });
      };

      $scope.fileDrag = function ( evt ) {
        $( evt.target ).toggleClass( 'project__file--drag' );
      };

      $scope.dragOverOut = function ( evt ) {
        console.log(evt);
        $( evt.target).find('.folder').toggleClass( 'folder--draghover' );
      };

      $scope.deleteProject = function ( ) {
        ajaxService.deleteItem( $scope.project.links.self ).then( function ( ) {
          $rootScope.$broadcast( 'PROJECT_DELETED', {
            'name': $scope.project.name
          } );
          $location.path( '/' );
        } );
      };
    }
  ] );
} )( angular, NGS );