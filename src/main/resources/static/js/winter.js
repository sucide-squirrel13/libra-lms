var Winter = (() => {
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __commonJS = (cb, mod) => function __require() {
    try {
      return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
    } catch (e) {
      throw mod = 0, e;
    }
  };

  // node_modules/q/q.js
  var require_q = __commonJS({
    "node_modules/q/q.js"(exports, module) {
      (function(definition) {
        if (typeof bootstrap === "function") {
          bootstrap("promise", definition);
        } else if (typeof exports === "object") {
          module.exports = definition();
        } else if (typeof define === "function" && define.amd) {
          define(definition);
        } else if (typeof ses !== "undefined") {
          if (!ses.ok()) {
            return;
          } else {
            ses.makeQ = definition;
          }
        } else {
          Q = definition();
        }
      })(function() {
        "use strict";
        var hasStacks = false;
        try {
          throw new Error();
        } catch (e) {
          hasStacks = !!e.stack;
        }
        var qStartingLine = captureLine();
        var qFileName;
        var noop = function() {
        };
        var nextTick = (function() {
          var head = { task: void 0, next: null };
          var tail = head;
          var flushing = false;
          var requestTick = void 0;
          var isNodeJS = false;
          function flush() {
            while (head.next) {
              head = head.next;
              var task = head.task;
              head.task = void 0;
              var domain = head.domain;
              if (domain) {
                head.domain = void 0;
                domain.enter();
              }
              try {
                task();
              } catch (e) {
                if (isNodeJS) {
                  if (domain) {
                    domain.exit();
                  }
                  setTimeout(flush, 0);
                  if (domain) {
                    domain.enter();
                  }
                  throw e;
                } else {
                  setTimeout(function() {
                    throw e;
                  }, 0);
                }
              }
              if (domain) {
                domain.exit();
              }
            }
            flushing = false;
          }
          nextTick = function(task) {
            tail = tail.next = {
              task,
              domain: isNodeJS && process.domain,
              next: null
            };
            if (!flushing) {
              flushing = true;
              requestTick();
            }
          };
          if (typeof process !== "undefined" && process.nextTick) {
            isNodeJS = true;
            requestTick = function() {
              process.nextTick(flush);
            };
          } else if (typeof setImmediate === "function") {
            if (typeof window !== "undefined") {
              requestTick = setImmediate.bind(window, flush);
            } else {
              requestTick = function() {
                setImmediate(flush);
              };
            }
          } else if (typeof MessageChannel !== "undefined") {
            var channel = new MessageChannel();
            channel.port1.onmessage = function() {
              requestTick = requestPortTick;
              channel.port1.onmessage = flush;
              flush();
            };
            var requestPortTick = function() {
              channel.port2.postMessage(0);
            };
            requestTick = function() {
              setTimeout(flush, 0);
              requestPortTick();
            };
          } else {
            requestTick = function() {
              setTimeout(flush, 0);
            };
          }
          return nextTick;
        })();
        var call = Function.call;
        function uncurryThis(f) {
          return function() {
            return call.apply(f, arguments);
          };
        }
        var array_slice = uncurryThis(Array.prototype.slice);
        var array_reduce = uncurryThis(
          Array.prototype.reduce || function(callback, basis) {
            var index = 0, length = this.length;
            if (arguments.length === 1) {
              do {
                if (index in this) {
                  basis = this[index++];
                  break;
                }
                if (++index >= length) {
                  throw new TypeError();
                }
              } while (1);
            }
            for (; index < length; index++) {
              if (index in this) {
                basis = callback(basis, this[index], index);
              }
            }
            return basis;
          }
        );
        var array_indexOf = uncurryThis(
          Array.prototype.indexOf || function(value) {
            for (var i = 0; i < this.length; i++) {
              if (this[i] === value) {
                return i;
              }
            }
            return -1;
          }
        );
        var array_map = uncurryThis(
          Array.prototype.map || function(callback, thisp) {
            var self = this;
            var collect = [];
            array_reduce(self, function(undefined2, value, index) {
              collect.push(callback.call(thisp, value, index, self));
            }, void 0);
            return collect;
          }
        );
        var object_create = Object.create || function(prototype) {
          function Type() {
          }
          Type.prototype = prototype;
          return new Type();
        };
        var object_hasOwnProperty = uncurryThis(Object.prototype.hasOwnProperty);
        var object_keys = Object.keys || function(object) {
          var keys = [];
          for (var key in object) {
            if (object_hasOwnProperty(object, key)) {
              keys.push(key);
            }
          }
          return keys;
        };
        var object_toString = uncurryThis(Object.prototype.toString);
        function isObject(value) {
          return value === Object(value);
        }
        function isStopIteration(exception) {
          return object_toString(exception) === "[object StopIteration]" || exception instanceof QReturnValue;
        }
        var QReturnValue;
        if (typeof ReturnValue !== "undefined") {
          QReturnValue = ReturnValue;
        } else {
          QReturnValue = function(value) {
            this.value = value;
          };
        }
        var STACK_JUMP_SEPARATOR = "From previous event:";
        function makeStackTraceLong(error, promise2) {
          if (hasStacks && promise2.stack && typeof error === "object" && error !== null && error.stack && error.stack.indexOf(STACK_JUMP_SEPARATOR) === -1) {
            var stacks = [];
            for (var p = promise2; !!p; p = p.source) {
              if (p.stack) {
                stacks.unshift(p.stack);
              }
            }
            stacks.unshift(error.stack);
            var concatedStacks = stacks.join("\n" + STACK_JUMP_SEPARATOR + "\n");
            error.stack = filterStackString(concatedStacks);
          }
        }
        function filterStackString(stackString) {
          var lines = stackString.split("\n");
          var desiredLines = [];
          for (var i = 0; i < lines.length; ++i) {
            var line = lines[i];
            if (!isInternalFrame(line) && !isNodeFrame(line) && line) {
              desiredLines.push(line);
            }
          }
          return desiredLines.join("\n");
        }
        function isNodeFrame(stackLine) {
          return stackLine.indexOf("(module.js:") !== -1 || stackLine.indexOf("(node.js:") !== -1;
        }
        function getFileNameAndLineNumber(stackLine) {
          var attempt1 = /at .+ \((.+):(\d+):(?:\d+)\)$/.exec(stackLine);
          if (attempt1) {
            return [attempt1[1], Number(attempt1[2])];
          }
          var attempt2 = /at ([^ ]+):(\d+):(?:\d+)$/.exec(stackLine);
          if (attempt2) {
            return [attempt2[1], Number(attempt2[2])];
          }
          var attempt3 = /.*@(.+):(\d+)$/.exec(stackLine);
          if (attempt3) {
            return [attempt3[1], Number(attempt3[2])];
          }
        }
        function isInternalFrame(stackLine) {
          var fileNameAndLineNumber = getFileNameAndLineNumber(stackLine);
          if (!fileNameAndLineNumber) {
            return false;
          }
          var fileName = fileNameAndLineNumber[0];
          var lineNumber = fileNameAndLineNumber[1];
          return fileName === qFileName && lineNumber >= qStartingLine && lineNumber <= qEndingLine;
        }
        function captureLine() {
          if (!hasStacks) {
            return;
          }
          try {
            throw new Error();
          } catch (e) {
            var lines = e.stack.split("\n");
            var firstLine = lines[0].indexOf("@") > 0 ? lines[1] : lines[2];
            var fileNameAndLineNumber = getFileNameAndLineNumber(firstLine);
            if (!fileNameAndLineNumber) {
              return;
            }
            qFileName = fileNameAndLineNumber[0];
            return fileNameAndLineNumber[1];
          }
        }
        function deprecate(callback, name, alternative) {
          return function() {
            if (typeof console !== "undefined" && typeof console.warn === "function") {
              console.warn(name + " is deprecated, use " + alternative + " instead.", new Error("").stack);
            }
            return callback.apply(callback, arguments);
          };
        }
        function Q2(value) {
          if (isPromise(value)) {
            return value;
          }
          if (isPromiseAlike(value)) {
            return coerce(value);
          } else {
            return fulfill(value);
          }
        }
        Q2.resolve = Q2;
        Q2.nextTick = nextTick;
        Q2.longStackSupport = false;
        Q2.defer = defer;
        function defer() {
          var messages = [], progressListeners = [], resolvedPromise;
          var deferred = object_create(defer.prototype);
          var promise2 = object_create(Promise2.prototype);
          promise2.promiseDispatch = function(resolve, op, operands) {
            var args = array_slice(arguments);
            if (messages) {
              messages.push(args);
              if (op === "when" && operands[1]) {
                progressListeners.push(operands[1]);
              }
            } else {
              nextTick(function() {
                resolvedPromise.promiseDispatch.apply(resolvedPromise, args);
              });
            }
          };
          promise2.valueOf = function() {
            if (messages) {
              return promise2;
            }
            var nearerValue = nearer(resolvedPromise);
            if (isPromise(nearerValue)) {
              resolvedPromise = nearerValue;
            }
            return nearerValue;
          };
          promise2.inspect = function() {
            if (!resolvedPromise) {
              return { state: "pending" };
            }
            return resolvedPromise.inspect();
          };
          if (Q2.longStackSupport && hasStacks) {
            try {
              throw new Error();
            } catch (e) {
              promise2.stack = e.stack.substring(e.stack.indexOf("\n") + 1);
            }
          }
          function become(newPromise) {
            resolvedPromise = newPromise;
            promise2.source = newPromise;
            array_reduce(messages, function(undefined2, message) {
              nextTick(function() {
                newPromise.promiseDispatch.apply(newPromise, message);
              });
            }, void 0);
            messages = void 0;
            progressListeners = void 0;
          }
          deferred.promise = promise2;
          deferred.resolve = function(value) {
            if (resolvedPromise) {
              return;
            }
            become(Q2(value));
          };
          deferred.fulfill = function(value) {
            if (resolvedPromise) {
              return;
            }
            become(fulfill(value));
          };
          deferred.reject = function(reason) {
            if (resolvedPromise) {
              return;
            }
            become(reject(reason));
          };
          deferred.notify = function(progress2) {
            if (resolvedPromise) {
              return;
            }
            array_reduce(progressListeners, function(undefined2, progressListener) {
              nextTick(function() {
                progressListener(progress2);
              });
            }, void 0);
          };
          return deferred;
        }
        defer.prototype.makeNodeResolver = function() {
          var self = this;
          return function(error, value) {
            if (error) {
              self.reject(error);
            } else if (arguments.length > 2) {
              self.resolve(array_slice(arguments, 1));
            } else {
              self.resolve(value);
            }
          };
        };
        Q2.Promise = promise;
        Q2.promise = promise;
        function promise(resolver) {
          if (typeof resolver !== "function") {
            throw new TypeError("resolver must be a function.");
          }
          var deferred = defer();
          try {
            resolver(deferred.resolve, deferred.reject, deferred.notify);
          } catch (reason) {
            deferred.reject(reason);
          }
          return deferred.promise;
        }
        promise.race = race;
        promise.all = all;
        promise.reject = reject;
        promise.resolve = Q2;
        Q2.passByCopy = function(object) {
          return object;
        };
        Promise2.prototype.passByCopy = function() {
          return this;
        };
        Q2.join = function(x, y) {
          return Q2(x).join(y);
        };
        Promise2.prototype.join = function(that) {
          return Q2([this, that]).spread(function(x, y) {
            if (x === y) {
              return x;
            } else {
              throw new Error("Can't join: not the same: " + x + " " + y);
            }
          });
        };
        Q2.race = race;
        function race(answerPs) {
          return promise(function(resolve, reject2) {
            for (var i = 0, len = answerPs.length; i < len; i++) {
              Q2(answerPs[i]).then(resolve, reject2);
            }
          });
        }
        Promise2.prototype.race = function() {
          return this.then(Q2.race);
        };
        Q2.makePromise = Promise2;
        function Promise2(descriptor, fallback, inspect) {
          if (fallback === void 0) {
            fallback = function(op) {
              return reject(new Error(
                "Promise does not support operation: " + op
              ));
            };
          }
          if (inspect === void 0) {
            inspect = function() {
              return { state: "unknown" };
            };
          }
          var promise2 = object_create(Promise2.prototype);
          promise2.promiseDispatch = function(resolve, op, args) {
            var result;
            try {
              if (descriptor[op]) {
                result = descriptor[op].apply(promise2, args);
              } else {
                result = fallback.call(promise2, op, args);
              }
            } catch (exception) {
              result = reject(exception);
            }
            if (resolve) {
              resolve(result);
            }
          };
          promise2.inspect = inspect;
          if (inspect) {
            var inspected = inspect();
            if (inspected.state === "rejected") {
              promise2.exception = inspected.reason;
            }
            promise2.valueOf = function() {
              var inspected2 = inspect();
              if (inspected2.state === "pending" || inspected2.state === "rejected") {
                return promise2;
              }
              return inspected2.value;
            };
          }
          return promise2;
        }
        Promise2.prototype.toString = function() {
          return "[object Promise]";
        };
        Promise2.prototype.then = function(fulfilled, rejected, progressed) {
          var self = this;
          var deferred = defer();
          var done = false;
          function _fulfilled(value) {
            try {
              return typeof fulfilled === "function" ? fulfilled(value) : value;
            } catch (exception) {
              return reject(exception);
            }
          }
          function _rejected(exception) {
            if (typeof rejected === "function") {
              makeStackTraceLong(exception, self);
              try {
                return rejected(exception);
              } catch (newException) {
                return reject(newException);
              }
            }
            return reject(exception);
          }
          function _progressed(value) {
            return typeof progressed === "function" ? progressed(value) : value;
          }
          nextTick(function() {
            self.promiseDispatch(function(value) {
              if (done) {
                return;
              }
              done = true;
              deferred.resolve(_fulfilled(value));
            }, "when", [function(exception) {
              if (done) {
                return;
              }
              done = true;
              deferred.resolve(_rejected(exception));
            }]);
          });
          self.promiseDispatch(void 0, "when", [void 0, function(value) {
            var newValue;
            var threw = false;
            try {
              newValue = _progressed(value);
            } catch (e) {
              threw = true;
              if (Q2.onerror) {
                Q2.onerror(e);
              } else {
                throw e;
              }
            }
            if (!threw) {
              deferred.notify(newValue);
            }
          }]);
          return deferred.promise;
        };
        Q2.when = when;
        function when(value, fulfilled, rejected, progressed) {
          return Q2(value).then(fulfilled, rejected, progressed);
        }
        Promise2.prototype.thenResolve = function(value) {
          return this.then(function() {
            return value;
          });
        };
        Q2.thenResolve = function(promise2, value) {
          return Q2(promise2).thenResolve(value);
        };
        Promise2.prototype.thenReject = function(reason) {
          return this.then(function() {
            throw reason;
          });
        };
        Q2.thenReject = function(promise2, reason) {
          return Q2(promise2).thenReject(reason);
        };
        Q2.nearer = nearer;
        function nearer(value) {
          if (isPromise(value)) {
            var inspected = value.inspect();
            if (inspected.state === "fulfilled") {
              return inspected.value;
            }
          }
          return value;
        }
        Q2.isPromise = isPromise;
        function isPromise(object) {
          return isObject(object) && typeof object.promiseDispatch === "function" && typeof object.inspect === "function";
        }
        Q2.isPromiseAlike = isPromiseAlike;
        function isPromiseAlike(object) {
          return isObject(object) && typeof object.then === "function";
        }
        Q2.isPending = isPending;
        function isPending(object) {
          return isPromise(object) && object.inspect().state === "pending";
        }
        Promise2.prototype.isPending = function() {
          return this.inspect().state === "pending";
        };
        Q2.isFulfilled = isFulfilled;
        function isFulfilled(object) {
          return !isPromise(object) || object.inspect().state === "fulfilled";
        }
        Promise2.prototype.isFulfilled = function() {
          return this.inspect().state === "fulfilled";
        };
        Q2.isRejected = isRejected;
        function isRejected(object) {
          return isPromise(object) && object.inspect().state === "rejected";
        }
        Promise2.prototype.isRejected = function() {
          return this.inspect().state === "rejected";
        };
        var unhandledReasons = [];
        var unhandledRejections = [];
        var trackUnhandledRejections = true;
        function resetUnhandledRejections() {
          unhandledReasons.length = 0;
          unhandledRejections.length = 0;
          if (!trackUnhandledRejections) {
            trackUnhandledRejections = true;
          }
        }
        function trackRejection(promise2, reason) {
          if (!trackUnhandledRejections) {
            return;
          }
          unhandledRejections.push(promise2);
          if (reason && typeof reason.stack !== "undefined") {
            unhandledReasons.push(reason.stack);
          } else {
            unhandledReasons.push("(no stack) " + reason);
          }
        }
        function untrackRejection(promise2) {
          if (!trackUnhandledRejections) {
            return;
          }
          var at = array_indexOf(unhandledRejections, promise2);
          if (at !== -1) {
            unhandledRejections.splice(at, 1);
            unhandledReasons.splice(at, 1);
          }
        }
        Q2.resetUnhandledRejections = resetUnhandledRejections;
        Q2.getUnhandledReasons = function() {
          return unhandledReasons.slice();
        };
        Q2.stopUnhandledRejectionTracking = function() {
          resetUnhandledRejections();
          trackUnhandledRejections = false;
        };
        resetUnhandledRejections();
        Q2.reject = reject;
        function reject(reason) {
          var rejection = Promise2({
            "when": function(rejected) {
              if (rejected) {
                untrackRejection(this);
              }
              return rejected ? rejected(reason) : this;
            }
          }, function fallback() {
            return this;
          }, function inspect() {
            return { state: "rejected", reason };
          });
          trackRejection(rejection, reason);
          return rejection;
        }
        Q2.fulfill = fulfill;
        function fulfill(value) {
          return Promise2({
            "when": function() {
              return value;
            },
            "get": function(name) {
              return value[name];
            },
            "set": function(name, rhs) {
              value[name] = rhs;
            },
            "delete": function(name) {
              delete value[name];
            },
            "post": function(name, args) {
              if (name === null || name === void 0) {
                return value.apply(void 0, args);
              } else {
                return value[name].apply(value, args);
              }
            },
            "apply": function(thisp, args) {
              return value.apply(thisp, args);
            },
            "keys": function() {
              return object_keys(value);
            }
          }, void 0, function inspect() {
            return { state: "fulfilled", value };
          });
        }
        function coerce(promise2) {
          var deferred = defer();
          nextTick(function() {
            try {
              promise2.then(deferred.resolve, deferred.reject, deferred.notify);
            } catch (exception) {
              deferred.reject(exception);
            }
          });
          return deferred.promise;
        }
        Q2.master = master;
        function master(object) {
          return Promise2({
            "isDef": function() {
            }
          }, function fallback(op, args) {
            return dispatch(object, op, args);
          }, function() {
            return Q2(object).inspect();
          });
        }
        Q2.spread = spread;
        function spread(value, fulfilled, rejected) {
          return Q2(value).spread(fulfilled, rejected);
        }
        Promise2.prototype.spread = function(fulfilled, rejected) {
          return this.all().then(function(array) {
            return fulfilled.apply(void 0, array);
          }, rejected);
        };
        Q2.async = async;
        function async(makeGenerator) {
          return function() {
            function continuer(verb, arg) {
              var result;
              if (typeof StopIteration === "undefined") {
                try {
                  result = generator[verb](arg);
                } catch (exception) {
                  return reject(exception);
                }
                if (result.done) {
                  return result.value;
                } else {
                  return when(result.value, callback, errback);
                }
              } else {
                try {
                  result = generator[verb](arg);
                } catch (exception) {
                  if (isStopIteration(exception)) {
                    return exception.value;
                  } else {
                    return reject(exception);
                  }
                }
                return when(result, callback, errback);
              }
            }
            var generator = makeGenerator.apply(this, arguments);
            var callback = continuer.bind(continuer, "next");
            var errback = continuer.bind(continuer, "throw");
            return callback();
          };
        }
        Q2.spawn = spawn;
        function spawn(makeGenerator) {
          Q2.done(Q2.async(makeGenerator)());
        }
        Q2["return"] = _return;
        function _return(value) {
          throw new QReturnValue(value);
        }
        Q2.promised = promised;
        function promised(callback) {
          return function() {
            return spread([this, all(arguments)], function(self, args) {
              return callback.apply(self, args);
            });
          };
        }
        Q2.dispatch = dispatch;
        function dispatch(object, op, args) {
          return Q2(object).dispatch(op, args);
        }
        Promise2.prototype.dispatch = function(op, args) {
          var self = this;
          var deferred = defer();
          nextTick(function() {
            self.promiseDispatch(deferred.resolve, op, args);
          });
          return deferred.promise;
        };
        Q2.get = function(object, key) {
          return Q2(object).dispatch("get", [key]);
        };
        Promise2.prototype.get = function(key) {
          return this.dispatch("get", [key]);
        };
        Q2.set = function(object, key, value) {
          return Q2(object).dispatch("set", [key, value]);
        };
        Promise2.prototype.set = function(key, value) {
          return this.dispatch("set", [key, value]);
        };
        Q2.del = // XXX legacy
        Q2["delete"] = function(object, key) {
          return Q2(object).dispatch("delete", [key]);
        };
        Promise2.prototype.del = // XXX legacy
        Promise2.prototype["delete"] = function(key) {
          return this.dispatch("delete", [key]);
        };
        Q2.mapply = // XXX As proposed by "Redsandro"
        Q2.post = function(object, name, args) {
          return Q2(object).dispatch("post", [name, args]);
        };
        Promise2.prototype.mapply = // XXX As proposed by "Redsandro"
        Promise2.prototype.post = function(name, args) {
          return this.dispatch("post", [name, args]);
        };
        Q2.send = // XXX Mark Miller's proposed parlance
        Q2.mcall = // XXX As proposed by "Redsandro"
        Q2.invoke = function(object, name) {
          return Q2(object).dispatch("post", [name, array_slice(arguments, 2)]);
        };
        Promise2.prototype.send = // XXX Mark Miller's proposed parlance
        Promise2.prototype.mcall = // XXX As proposed by "Redsandro"
        Promise2.prototype.invoke = function(name) {
          return this.dispatch("post", [name, array_slice(arguments, 1)]);
        };
        Q2.fapply = function(object, args) {
          return Q2(object).dispatch("apply", [void 0, args]);
        };
        Promise2.prototype.fapply = function(args) {
          return this.dispatch("apply", [void 0, args]);
        };
        Q2["try"] = Q2.fcall = function(object) {
          return Q2(object).dispatch("apply", [void 0, array_slice(arguments, 1)]);
        };
        Promise2.prototype.fcall = function() {
          return this.dispatch("apply", [void 0, array_slice(arguments)]);
        };
        Q2.fbind = function(object) {
          var promise2 = Q2(object);
          var args = array_slice(arguments, 1);
          return function fbound() {
            return promise2.dispatch("apply", [
              this,
              args.concat(array_slice(arguments))
            ]);
          };
        };
        Promise2.prototype.fbind = function() {
          var promise2 = this;
          var args = array_slice(arguments);
          return function fbound() {
            return promise2.dispatch("apply", [
              this,
              args.concat(array_slice(arguments))
            ]);
          };
        };
        Q2.keys = function(object) {
          return Q2(object).dispatch("keys", []);
        };
        Promise2.prototype.keys = function() {
          return this.dispatch("keys", []);
        };
        Q2.all = all;
        function all(promises) {
          return when(promises, function(promises2) {
            var countDown = 0;
            var deferred = defer();
            array_reduce(promises2, function(undefined2, promise2, index) {
              var snapshot;
              if (isPromise(promise2) && (snapshot = promise2.inspect()).state === "fulfilled") {
                promises2[index] = snapshot.value;
              } else {
                ++countDown;
                when(
                  promise2,
                  function(value) {
                    promises2[index] = value;
                    if (--countDown === 0) {
                      deferred.resolve(promises2);
                    }
                  },
                  deferred.reject,
                  function(progress2) {
                    deferred.notify({ index, value: progress2 });
                  }
                );
              }
            }, void 0);
            if (countDown === 0) {
              deferred.resolve(promises2);
            }
            return deferred.promise;
          });
        }
        Promise2.prototype.all = function() {
          return all(this);
        };
        Q2.allResolved = deprecate(allResolved, "allResolved", "allSettled");
        function allResolved(promises) {
          return when(promises, function(promises2) {
            promises2 = array_map(promises2, Q2);
            return when(all(array_map(promises2, function(promise2) {
              return when(promise2, noop, noop);
            })), function() {
              return promises2;
            });
          });
        }
        Promise2.prototype.allResolved = function() {
          return allResolved(this);
        };
        Q2.allSettled = allSettled;
        function allSettled(promises) {
          return Q2(promises).allSettled();
        }
        Promise2.prototype.allSettled = function() {
          return this.then(function(promises) {
            return all(array_map(promises, function(promise2) {
              promise2 = Q2(promise2);
              function regardless() {
                return promise2.inspect();
              }
              return promise2.then(regardless, regardless);
            }));
          });
        };
        Q2.fail = // XXX legacy
        Q2["catch"] = function(object, rejected) {
          return Q2(object).then(void 0, rejected);
        };
        Promise2.prototype.fail = // XXX legacy
        Promise2.prototype["catch"] = function(rejected) {
          return this.then(void 0, rejected);
        };
        Q2.progress = progress;
        function progress(object, progressed) {
          return Q2(object).then(void 0, void 0, progressed);
        }
        Promise2.prototype.progress = function(progressed) {
          return this.then(void 0, void 0, progressed);
        };
        Q2.fin = // XXX legacy
        Q2["finally"] = function(object, callback) {
          return Q2(object)["finally"](callback);
        };
        Promise2.prototype.fin = // XXX legacy
        Promise2.prototype["finally"] = function(callback) {
          callback = Q2(callback);
          return this.then(function(value) {
            return callback.fcall().then(function() {
              return value;
            });
          }, function(reason) {
            return callback.fcall().then(function() {
              throw reason;
            });
          });
        };
        Q2.done = function(object, fulfilled, rejected, progress2) {
          return Q2(object).done(fulfilled, rejected, progress2);
        };
        Promise2.prototype.done = function(fulfilled, rejected, progress2) {
          var onUnhandledError = function(error) {
            nextTick(function() {
              makeStackTraceLong(error, promise2);
              if (Q2.onerror) {
                Q2.onerror(error);
              } else {
                throw error;
              }
            });
          };
          var promise2 = fulfilled || rejected || progress2 ? this.then(fulfilled, rejected, progress2) : this;
          if (typeof process === "object" && process && process.domain) {
            onUnhandledError = process.domain.bind(onUnhandledError);
          }
          promise2.then(void 0, onUnhandledError);
        };
        Q2.timeout = function(object, ms, message) {
          return Q2(object).timeout(ms, message);
        };
        Promise2.prototype.timeout = function(ms, message) {
          var deferred = defer();
          var timeoutId = setTimeout(function() {
            deferred.reject(new Error(message || "Timed out after " + ms + " ms"));
          }, ms);
          this.then(function(value) {
            clearTimeout(timeoutId);
            deferred.resolve(value);
          }, function(exception) {
            clearTimeout(timeoutId);
            deferred.reject(exception);
          }, deferred.notify);
          return deferred.promise;
        };
        Q2.delay = function(object, timeout) {
          if (timeout === void 0) {
            timeout = object;
            object = void 0;
          }
          return Q2(object).delay(timeout);
        };
        Promise2.prototype.delay = function(timeout) {
          return this.then(function(value) {
            var deferred = defer();
            setTimeout(function() {
              deferred.resolve(value);
            }, timeout);
            return deferred.promise;
          });
        };
        Q2.nfapply = function(callback, args) {
          return Q2(callback).nfapply(args);
        };
        Promise2.prototype.nfapply = function(args) {
          var deferred = defer();
          var nodeArgs = array_slice(args);
          nodeArgs.push(deferred.makeNodeResolver());
          this.fapply(nodeArgs).fail(deferred.reject);
          return deferred.promise;
        };
        Q2.nfcall = function(callback) {
          var args = array_slice(arguments, 1);
          return Q2(callback).nfapply(args);
        };
        Promise2.prototype.nfcall = function() {
          var nodeArgs = array_slice(arguments);
          var deferred = defer();
          nodeArgs.push(deferred.makeNodeResolver());
          this.fapply(nodeArgs).fail(deferred.reject);
          return deferred.promise;
        };
        Q2.nfbind = Q2.denodeify = function(callback) {
          var baseArgs = array_slice(arguments, 1);
          return function() {
            var nodeArgs = baseArgs.concat(array_slice(arguments));
            var deferred = defer();
            nodeArgs.push(deferred.makeNodeResolver());
            Q2(callback).fapply(nodeArgs).fail(deferred.reject);
            return deferred.promise;
          };
        };
        Promise2.prototype.nfbind = Promise2.prototype.denodeify = function() {
          var args = array_slice(arguments);
          args.unshift(this);
          return Q2.denodeify.apply(void 0, args);
        };
        Q2.nbind = function(callback, thisp) {
          var baseArgs = array_slice(arguments, 2);
          return function() {
            var nodeArgs = baseArgs.concat(array_slice(arguments));
            var deferred = defer();
            nodeArgs.push(deferred.makeNodeResolver());
            function bound() {
              return callback.apply(thisp, arguments);
            }
            Q2(bound).fapply(nodeArgs).fail(deferred.reject);
            return deferred.promise;
          };
        };
        Promise2.prototype.nbind = function() {
          var args = array_slice(arguments, 0);
          args.unshift(this);
          return Q2.nbind.apply(void 0, args);
        };
        Q2.nmapply = // XXX As proposed by "Redsandro"
        Q2.npost = function(object, name, args) {
          return Q2(object).npost(name, args);
        };
        Promise2.prototype.nmapply = // XXX As proposed by "Redsandro"
        Promise2.prototype.npost = function(name, args) {
          var nodeArgs = array_slice(args || []);
          var deferred = defer();
          nodeArgs.push(deferred.makeNodeResolver());
          this.dispatch("post", [name, nodeArgs]).fail(deferred.reject);
          return deferred.promise;
        };
        Q2.nsend = // XXX Based on Mark Miller's proposed "send"
        Q2.nmcall = // XXX Based on "Redsandro's" proposal
        Q2.ninvoke = function(object, name) {
          var nodeArgs = array_slice(arguments, 2);
          var deferred = defer();
          nodeArgs.push(deferred.makeNodeResolver());
          Q2(object).dispatch("post", [name, nodeArgs]).fail(deferred.reject);
          return deferred.promise;
        };
        Promise2.prototype.nsend = // XXX Based on Mark Miller's proposed "send"
        Promise2.prototype.nmcall = // XXX Based on "Redsandro's" proposal
        Promise2.prototype.ninvoke = function(name) {
          var nodeArgs = array_slice(arguments, 1);
          var deferred = defer();
          nodeArgs.push(deferred.makeNodeResolver());
          this.dispatch("post", [name, nodeArgs]).fail(deferred.reject);
          return deferred.promise;
        };
        Q2.nodeify = nodeify;
        function nodeify(object, nodeback) {
          return Q2(object).nodeify(nodeback);
        }
        Promise2.prototype.nodeify = function(nodeback) {
          if (nodeback) {
            this.then(function(value) {
              nextTick(function() {
                nodeback(null, value);
              });
            }, function(error) {
              nextTick(function() {
                nodeback(error);
              });
            });
          } else {
            return this;
          }
        };
        var qEndingLine = captureLine();
        return Q2;
      });
    }
  });

  // node_modules/winter/lib/providers/module-provider.js
  var require_module_provider = __commonJS({
    "node_modules/winter/lib/providers/module-provider.js"(exports, module) {
      var Q2 = require_q();
      var ModuleProvider = function(name, mod) {
        this._name = name;
        this._dependencies = this._parseDependencies(name, mod);
        this._module = mod;
        this._instance = null;
      };
      var FN_ARGS = /^function\s*[^\(]*\(\s*([^\)]*)\)/m;
      var FN_ARG_SPLIT = /,/;
      var FN_ARG = /^\s*(_?)(\S+?)\1\s*$/;
      var STRIP_COMMENTS = /((\/\/.*$)|(\/\*[\s\S]*?\*\/))/mg;
      ModuleProvider.prototype._parseDependencies = function(name, mod) {
        if (mod instanceof Function) {
          var args = [];
          var uncommented = mod.toString().replace(STRIP_COMMENTS, "");
          var declaration = uncommented.match(FN_ARGS);
          declaration[1].split(FN_ARG_SPLIT).forEach(function(arg) {
            arg.replace(FN_ARG, function(all, underscore, name2) {
              args.push(name2);
            });
          });
          return args;
        } else {
          throw new Error("[" + name + "] is not a function; could not parse dependencies.");
        }
      };
      ModuleProvider.prototype.dependencies = function() {
        return this._dependencies;
      };
      ModuleProvider.prototype.instantiate = function(providers) {
        var self = this;
        if (self._instance != null) {
          return self._instance;
        } else {
          var promises = [];
          self._dependencies.forEach(function(dependency) {
            promises.push(Q2(providers[dependency].instantiate(providers, self._name)));
          });
          return Q2.all(promises).then(function(parameters) {
            if (self._module instanceof Function) {
              var result = new (Function.prototype.bind.apply(self._module, [self._module].concat(parameters)))();
              self._instance = result;
              return result;
            } else {
              throw new Error("[" + self._name + "] is not a function; could not instantiate.");
            }
          });
        }
      };
      module.exports = ModuleProvider;
    }
  });

  // node_modules/winter/lib/providers/value-provider.js
  var require_value_provider = __commonJS({
    "node_modules/winter/lib/providers/value-provider.js"(exports, module) {
      var ValueProvider = function(value) {
        this.value = value;
      };
      ValueProvider.prototype.dependencies = function() {
        return [];
      };
      ValueProvider.prototype.instantiate = function() {
        return this.value;
      };
      module.exports = ValueProvider;
    }
  });

  // ../../../../tmp/log4js-browser-shim.js
  var require_log4js_browser_shim = __commonJS({
    "../../../../tmp/log4js-browser-shim.js"(exports, module) {
      module.exports = {
        getLogger: function(name) {
          return {
            info: function() {
              console.info.apply(console, ["[" + name + "]"].concat(Array.prototype.slice.call(arguments)));
            },
            warn: function() {
              console.warn.apply(console, ["[" + name + "]"].concat(Array.prototype.slice.call(arguments)));
            },
            error: function() {
              console.error.apply(console, ["[" + name + "]"].concat(Array.prototype.slice.call(arguments)));
            },
            debug: function() {
              console.debug.apply(console, ["[" + name + "]"].concat(Array.prototype.slice.call(arguments)));
            }
          };
        }
      };
    }
  });

  // node_modules/winter/lib/providers/logger-provider.js
  var require_logger_provider = __commonJS({
    "node_modules/winter/lib/providers/logger-provider.js"(exports, module) {
      var log4js = require_log4js_browser_shim();
      var LoggerProvider = function() {
      };
      LoggerProvider.prototype.dependencies = function() {
        return [];
      };
      LoggerProvider.prototype.instantiate = function(providers, name) {
        if (name == void 0) {
          return null;
        } else {
          return log4js.getLogger(name);
        }
      };
      module.exports = LoggerProvider;
    }
  });

  // node_modules/winter/lib/index.js
  var require_index = __commonJS({
    "node_modules/winter/lib/index.js"(exports, module) {
      var Q2 = require_q();
      var Winter = function() {
        this._providers = {};
        this._dependencies = {};
      };
      var ModuleProvider = require_module_provider();
      var ValueProvider = require_value_provider();
      var LoggerProvider = require_logger_provider();
      Winter.ModuleProvider = ModuleProvider;
      Winter.ValueProvider = ValueProvider;
      Winter.LoggerProvider = LoggerProvider;
      Winter.prototype.register = function(name, provider) {
        this._providers[name] = provider;
        this._dependencies[name] = provider.dependencies();
      };
      Winter.prototype.registerModules = function(loader, mapping) {
        var self = this;
        Object.keys(mapping).forEach(function(name) {
          var value = mapping[name];
          var mod = loader(value);
          var provider = new ModuleProvider(name, mod);
          self.register(name, provider);
        });
      };
      Winter.prototype._linkNext = function(names, dependants, spliceableDependencies) {
        var self = this;
        var next = [];
        var promises = [];
        names.forEach(function(name) {
          var promise = Q2(self._providers[name].instantiate(self._providers)).then(function() {
            var moduleDependants = dependants[name];
            if (moduleDependants != null) {
              delete moduleDependants[name];
              moduleDependants.forEach(function(dependant) {
                var moduleDependencies = spliceableDependencies[dependant];
                var index = moduleDependencies.indexOf(name);
                while (index !== -1) {
                  moduleDependencies.splice(index, 1);
                  index = moduleDependencies.indexOf(name);
                }
                if (moduleDependencies.length == 0) {
                  next.push(dependant);
                }
              });
            }
          });
          promises.push(promise);
        });
        return Q2.all(promises).then(function() {
          if (next.length > 0) {
            return self._linkNext(next, dependants, spliceableDependencies);
          }
        });
      };
      Winter.prototype.link = function() {
        var self = this;
        var instantiatable = [];
        Object.keys(self._dependencies).forEach(function(name) {
          if (self._dependencies[name].length == 0) {
            instantiatable.push(name);
          }
        });
        var dependants = self._depentants(self._dependencies);
        var spliceableDependencies = {};
        Object.keys(self._dependencies).forEach(function(key) {
          spliceableDependencies[key] = self._dependencies[key].slice();
        });
        return self._linkNext(instantiatable, dependants, spliceableDependencies).then(function() {
          var unresolved = {};
          Object.keys(spliceableDependencies).forEach(function(dependant) {
            var unresolvedDependencies = spliceableDependencies[dependant];
            if (unresolvedDependencies.length > 0) {
              unresolved[dependant] = unresolvedDependencies;
            }
          });
          if (Object.keys(unresolved).length > 0) {
            throw new Error("Unresolved dependencies:" + JSON.stringify(unresolved, null, 2));
          }
        });
      };
      Winter.prototype._depentants = function(dependenciesmap) {
        var dependants = {};
        Object.keys(dependenciesmap).forEach(function(name) {
          dependenciesmap[name].forEach(function(dependency) {
            dependants[dependency] = (dependants[dependency] || []).concat(name);
          });
        });
        return dependants;
      };
      module.exports = Winter;
    }
  });
  return require_index();
})();
/*! Bundled license information:

q/q.js:
  (*!
   *
   * Copyright 2009-2012 Kris Kowal under the terms of the MIT
   * license found at http://github.com/kriskowal/q/raw/master/LICENSE
   *
   * With parts by Tyler Close
   * Copyright 2007-2009 Tyler Close under the terms of the MIT X license found
   * at http://www.opensource.org/licenses/mit-license.html
   * Forked at ref_send.js version: 2009-05-11
   *
   * With parts by Mark Miller
   * Copyright (C) 2011 Google Inc.
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   *
   *)
*/
