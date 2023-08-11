
(import :ntest)


(comment "Re-export ntest.test as test.test")
(def test.test ntest.test)
(export test.test)

(comment "Fail a test with a certain message. Returns a closure; needs to be invoked as ((test.fail \"Fail Message\"))")
(defun test.fail (message) (ntest.fail message))
(export test.fail)

(comment "Assert true or fail with message. Returns a closure")
(defun test.assert (cond message) (if cond noop (ntest.fail message)))
(export test.assert)

(comment "Assert that two arguments are equal. Returns a closure")
(defun test.assert-eq (actual expected)
  (test.assert
      (= actual expected)
      (stringify "Expected" expected "got" actual)))
(export test.assert-eq)
