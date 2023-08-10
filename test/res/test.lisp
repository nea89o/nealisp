(debuglog "Hello, World, here is an atom:" :iamanatom)
(defun myfun (var) (debuglog var)) 
(myfun :myfunworks)
((lambda (a) (debuglog a)) :atom)
(defun testlog (a ...) (seq
    (debuglog "a" a)
    (debuglog "..." ...)))
(testlog :test :work :whatever)
(def helloworld (pure "hello world"))
(debuglog helloworld (helloworld))
(debuglog "+" (+ 1.2 15))
(debuglog "-" (- 1 3))
(debuglog "*" (* 10 10))
(debuglog "/" (/ 1 3 2))
(debuglog "============")
(defun testsomething (c) (debuglog (if! c (seq (debuglog "left evaluated") (return "truthy value")) "falsey value")))
(testsomething true)
(testsomething false)
(noop)
(debuglog "============")
(debuglog "This should fail" sc)
(import :secondary)
(debuglog "This should work" sc)

(debuglog "============")
(debuglog "Running tests")
(import :test)
(test.test "Funny test" (seq
    (debuglog "Funny test running")
    ((test.assert false "False failed"))
    ((test.fail "Test failed"))))


