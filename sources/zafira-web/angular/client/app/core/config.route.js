(function () {
    'use strict';

    angular.module('app')
        .config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$ocLazyLoadProvider',
                function($stateProvider, $urlRouterProvider, $httpProvider, $ocLazyLoadProvider) {

                $stateProvider
	                .state('dashboard', {
	                    url: '/dashboards/:id',
	                    templateUrl: 'app/_dashboards/list.html',
                        data: {
                            requireLogin: true
                        },
                        params: {
                            currentUser: null
                        },
                        resolve: {
                            currentUser: ['$stateParams', '$q', 'UserService', '$state', ($stateParams, $q, UserService, $state) => {//TODO: use usual function instaed of arrow
                                var currentUser = UserService.getCurrentUser();

                                if (!currentUser) {
                                    return UserService.initCurrentUser()
                                        .then(function(user) {
                                            if (!$stateParams.id) {
                                                $state.go('dashboard', {id: user.defaultDashboardId}, {notify: false});
                                            }

                                            return $q.resolve(user);
                                        });
                                } else {
                                    if (!$stateParams.id) {
                                        $state.go('dashboard', {id: currentUser.defaultDashboardId}, {notify: false});
                                    }

                                    return $q.resolve(currentUser);
                                }
                            },]
                        }
	                })
	                .state('dashboards', {
	                    url: '/dashboards',
	                    templateUrl: 'app/_dashboards/list.html',
                        data: {
                            requireLogin: true
                        },
                        params: {
                            currentUser: null
                        },
                        resolve: {
                            currentUser: ['$stateParams', '$q', 'UserService', '$state', ($stateParams, $q, UserService, $state) => {//TODO: remove unused $stateParams and use usual function instaed of arrow
                                var currentUser = UserService.getCurrentUser();

                                if (!currentUser) {
                                    return UserService.initCurrentUser()
                                        .then(function(user) {
                                            $state.go('dashboard', {id: user.defaultDashboardId});
                                        });
                                } else {
                                    $state.go('dashboard', {id: currentUser.defaultDashboardId});
                                }
                            },]
                        }
	                })
                    .state('views', {
                        url: '/views/:id',
                        templateUrl: 'app/_views/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('signin', {
                        url: '/signin',
                        templateUrl: 'app/_auth/signin.html',
                        params: {
                            referrer: null,
                            referrerParams: null
                        },
                        data: {
                            onlyGuests: true,
                            classes: 'body-wide body-auth'
                        }
                    })
                    .state('signup', {
                        url: '/signup',
                        templateUrl: 'app/_auth/signup.html',
                        data: {
                            onlyGuests: true,
                            classes: 'body-wide body-auth'
                        }
                    })
                    .state('logout', {
                        url: '/logout',
                        controller: function($state, AuthService) {
                            AuthService.ClearCredentials();
                            $state.go('signin');
                        },
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('forgotPassword', {
                        url: '/password/forgot',
                        templateUrl: 'app/_auth/forgot-password.html',
                        data: {
                            onlyGuests: true,
                            classes: 'body-wide body-auth'
                        }
                    })
                    .state('resetPassword', {
                        url: '/password/reset',
                        templateUrl: 'app/_auth/reset-password.html',
                        data: {
                            onlyGuests: true,
                            classes: 'body-wide body-auth'
                        }
                    })
                    .state('users/profile', {
                        url: '/users/profile',
                        templateUrl: 'app/_users/profile.html',
                        data: {
                            requireLogin: true,
                            classes: 'p-user-profile'
                        }
                    })
                    .state('users', {
                        url: '/users',
                        templateUrl: 'app/_users/list.html',
                        data: {
                            requireLogin: true,
                            classes: 'p-users'
                        }
                    })
                    .state('scm/callback', {
                        url: '/scm/callback',
                        templateUrl: 'app/_scm/list.html'
                    })
                    .state('tests/cases', {
                        url: '/tests/cases',
                        templateUrl: 'app/_testcases/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('tests/cases/metrics', {
                        url: '/tests/cases/:id/metrics',
                        templateUrl: 'app/_testcases/metrics/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('tests/run', {
                        controller: 'TestDetailsController',
                        controllerAs: '$ctrl',
                        url: '/tests/runs/:testRunId',
                        templateUrl: 'app/containers/test-details/test-details.html',
                        store: true,
                        params: {
                            testRun: null
                        },
                        data: {
                            requireLogin: true,
                            classes: 'p-tests-run-details'
                        },
                        resolve: {
                            testRun: ['$stateParams', '$q', '$state', 'TestRunService', function($stateParams, $q, $state, TestRunService) {
                                if ($stateParams.testRun) {
                                    return $q.resolve($stateParams.testRun);
                                } else if ($stateParams.testRunId) {
                                    const params = {
                                        id: $stateParams.testRunId
                                    };

                                    return TestRunService.searchTestRuns(params)
                                    .then(function(response) {
                                        if (response.success && response.data.results && response.data.results[0]) {
                                            return response.data.results[0];
                                        } else { //TODO: show error message & redirect to testruns
                                            return $q.reject({message: 'Can\'t get test run with ID=' + $stateParams.testRunId});
                                        }
                                    })
                                    .catch(function(error) {
                                        console.log(error); //TODO: show toaster notification
                                        $state.go('tests/runs');
                                    });
                                } else {
                                    $state.go('tests/runs');
                                }
                            }]
                        }
                    })
                    .state('tests/runs', {
                        url: '/tests/runs',
                        templateUrl: 'app/containers/tests-runs/tests-runs.html',
                        controller: 'TestsRunsController',
                        controllerAs: '$ctrl',
                        bindToController: true,
                        params: {
                            activeTestRunId: null
                        },
                        data: {
                            requireLogin: true,
                            classes: 'p-tests-runs'
                        },
                        resolve: {
                            resolvedTestRuns: ['$state', 'testsRunsService', '$q', function($state, testsRunsService, $q) {
                                const prevState = $state.current.name;

                                testsRunsService.resetFilteringState();
                                // read saved search/filtering data only if we reload current page or returning from internal page
                                if (!prevState || prevState === 'tests/run' || prevState === 'tests/runs') {
                                    testsRunsService.readStoredParams();
                                } else {
                                    testsRunsService.deleteStoredParams();
                                }

                                return testsRunsService.fetchTestRuns().catch(function(err) {
                                    err && err.message && alertify.error(err.message);
                                    //1st approach: if can't load with user/cached searchParams reset them and reload page
                                    // if (!force) {
                                    //     testsRunsService.deleteStoredParams();
                                    //     $state.go('tests/runs', null, { reload: true });
                                    // }
                                    //2nd approach: if can't load with user/cached searchParams return empty data
                                    return $q.resolve([]);
                                });
                            }],
                            activeTestRunId: ['$stateParams', '$q', function($stateParams, $q) { //TODO: use to implement highlighting opened tesRun
                                const id = $stateParams.activeTestRunId ? $stateParams.activeTestRunId : undefined;

                                return $q.resolve(id);
                            }]
                        }
                    })
                    .state('tests/runs/info', {
                        url: '/tests/runs/:testRunId/info/:testId',
                        templateUrl: 'app/containers/test-run-info/test-run-info.html',
                        controller: 'TestRunInfoController',
                        controllerAs: '$ctrl',
                        bindToController: true,
                        data: {
                            requireLogin: true
                        },
                        params: {
                            testRun: null
                        },
                        resolve: {
                            testRun: ['$stateParams', '$q', '$state', 'TestRunService', function($stateParams, $q, $state, TestRunService) {
                                if ($stateParams.testRun) {
                                    return $q.resolve($stateParams.testRun);
                                } else if ($stateParams.testRunId) {
                                    const params = {
                                        id: $stateParams.testRunId
                                    };

                                    return TestRunService.searchTestRuns(params)
                                        .then(function(response) {
                                            if (response.success && response.data.results && response.data.results[0]) {
                                                return response.data.results[0];
                                            } else {
                                                return $q.reject({message: 'Can\'t get test run with ID=' + $stateParams.testRunId});
                                            }
                                        })
                                        .catch(function(error) {
                                            console.log(error); //TODO: show toaster notification
                                            $state.go('tests/runs');
                                        });
                                } else {
                                    $state.go('tests/runs');
                                }
                            }]
                        }
                    })
                    .state('settings', {
                        url: '/settings',
                        templateUrl: 'app/_settings/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('monitors', {
                        url: '/monitors',
                        templateUrl: 'app/_monitors/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('integrations', {
                        url: '/integrations',
                        templateUrl: 'app/_integrations/list.html',
                        data: {
                            requireLogin: true,
                            classes: 'p-integrations'
                        }
                    })
                    .state('certifications', {
                        url: '/certification',
                        templateUrl: 'app/_certifications/list.html',
                        data: {
                            requireLogin: true
                        }
                    })
                    .state('404', {
                        url: '/404',
                        templateUrl: 'app/page/404.html',
                        data: {
                            classes: 'body-wide body-err'
                        }
                    })
                    .state('500', {
                        url: '/500',
                        templateUrl: 'app/page/500.html',
                        data: {
                            classes: 'body-wide body-err'
                        }
                    });

                $urlRouterProvider
                    .when('/', '/dashboards')
                    .when('', '/dashboards')
                    .otherwise('/404');

            }
        ]);
})();
